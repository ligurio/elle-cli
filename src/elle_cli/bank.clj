(ns elle-cli.bank
  "Helper functions for doing bank tests, where you simulate
  transfers between accounts, and verify that reads always show
  the same balance. The test map should have these additional
  options:

  :accounts     A collection of account identifiers.
  :total-amount Total amount to allocate.
  :max-transfer The largest transfer we'll try to execute."
  ; (:refer-clojure :exclude [read test])
  (:require [clojure.core.reducers :as r]
            [jepsen [checker :as checker]
             [history :as h]
                    [store :as store]
                    [util :as util]]))

            ; [jepsen.tests.bank :as jepsen-bank]

; Source: https://github.com/jepsen-io/jepsen/blob/main/jepsen/src/jepsen/tests/bank.clj

(defn err-badness
  "Takes a bank error and returns a number, depending on its type. Bigger
  numbers mean more egregious errors."
  [test err]
  (case (:type err)
    :unexpected-key (count (:unexpected err))
    :nil-balance    (count (:nils err))
    :wrong-total    (Math/abs (float (/ (- (:total err) (:total-amount test))
                                        (:total-amount test))))
    :negative-value (- (reduce + (:negative err)))))

(defn check-op
  "Takes a single op and returns errors in its balance"
  [accts total negative-balances? op]
  (let [ks       (keys (:value op))
        balances (vals (:value op))]
    (cond (not-every? accts ks)
          {:type        :unexpected-key
           :unexpected  (remove accts ks)
           :op          op}

          (some nil? balances)
          {:type    :nil-balance
           :nils    (->> (:value op)
                         (remove val)
                         (into {}))
           :op      op}

          (not= total (reduce + balances))
          {:type     :wrong-total
           :total    (reduce + balances)
           :op       op}

          (and (not negative-balances?) (some neg? balances))
          {:type     :negative-value
           :negative (filter neg? balances)
           :op       op})))

(defn checker
  "Verifies that all reads must sum to (:total test), and, unless
  :negative-balances? is true, checks that all balances are
  non-negative."
  [checker-opts]
  (reify checker/Checker
    (check [this test history opts]
      (let [accts (set (:accounts test))
            total (:total-amount test)
            reads (->> history
                       (h/filter (h/has-f? :read))
                       h/oks)
            errors (->> reads
                        (r/map (partial check-op
                                        accts
                                        total
                                        (:negative-balances? checker-opts)))
                        (r/filter identity)
                        (group-by :type))]
        {:valid?      (every? empty? (vals errors))
         :read-count  (count reads)
         :error-count (reduce + (map count (vals errors)))
         :first-error (util/min-by (comp :index :op) (map first (vals errors)))
         :errors      (->> errors
                           (map
                             (fn [[type errs]]
                               [type
                                (merge {:count (count errs)
                                        :first (first errs)
                                        :worst (util/max-by
                                                 (partial err-badness test)
                                                 errs)
                                        :last  (peek errs)}
                                       (if (= type :wrong-total)
                                         {:lowest  (util/min-by :total errs)
                                          :highest (util/max-by :total errs)}
                                         {}))]))
                           (into {}))}))))
