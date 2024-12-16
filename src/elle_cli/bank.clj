(ns elle-cli.bank
  "Helper functions for doing bank tests, where you simulate
  transfers between accounts, and verify that reads always show
  the same balance. The checker options may include:

  :accounts           A collection of account identifiers.
  :total-amount       Total amount to allocate.
  :max-transfer       The largest transfer we'll try to execute.
  :negative-balances? Whether negative balances are allowed."
  (:require [clojure.core.reducers :as r]
            [jepsen [checker :as checker]
             [history :as h]
                    [util :as util]]))

(defn err-badness
  "Takes a bank error and returns a number, depending on its type. Bigger
  numbers mean more egregious errors."
  [total err]
  (case (:type err)
    :unexpected-key (count (:unexpected err))
    :nil-balance    (count (:nils err))
    :wrong-total    (if (pos? total)
                      (Math/abs (float (/ (- (:total err) total) total)))
                      0)
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
  "Verifies that all reads must sum to total-amount, and, unless
  :negative-balances? is true, checks that all balances are
  non-negative. If :accounts is not provided, infers account
  identifiers from the history. If :total-amount is not provided,
  infers it from the first read's sum."
  [checker-opts]
  (reify checker/Checker
    (check [this test history opts]
      (let [reads (->> history
                       (h/filter (h/has-f? :read))
                       h/oks)
            first-read-value (:value (first reads))
            accts (or (:accounts checker-opts)
                      (set (keys first-read-value)))
            total (or (:total-amount checker-opts)
                      (reduce + (vals first-read-value)))
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
                                                 (partial err-badness total)
                                                 errs)
                                        :last  (peek errs)}
                                       (if (= type :wrong-total)
                                         {:lowest  (util/min-by :total errs)
                                          :highest (util/max-by :total errs)}
                                         {}))]))
                           (into {}))}))))
