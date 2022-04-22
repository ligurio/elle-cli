(ns elle-cli.sequential
  "A sequential consistency test.
  Verify that client order is consistent with DB order by performing queries
  (in four distinct transactions) like:
  A: insert x
  A: insert y
  B: read y
  B: read x
  A's process order enforces that x must be visible before y, so we should
  always read both or neither.
  Splits keys up onto different tables to make sure they fall in different
  shard ranges."
  (:require [jepsen [checker :as checker]
                    [independent :as independent]]
            [knossos.model :as model]
            [knossos.op :as op]
            [clojure.core.reducers :as r]))

(defn trailing-nil?
  "Does the given sequence contain a nil anywhere after a non-nil element?"
  [coll]
  (some nil? (drop-while nil? coll)))

(defn subkeys
  "The subkeys used for a given key, in order."
  [key-count k]
  (mapv (partial str k "_") (range key-count)))

; https://jepsen.io/consistency/models/sequential
; https://github.com/jepsen-io/jepsen/blob/main/cockroachdb/src/jepsen/cockroach/sequential.clj
; https://github.com/jepsen-io/jepsen/blob/main/tidb/src/tidb/sequential.clj
; https://github.com/jepsen-io/jepsen/blob/main/dgraph/src/jepsen/dgraph/sequential.clj
(defn checker
  "A sequential consistency checker."
  []
  (reify checker/Checker
    (check [this test history opts]
      (assert (integer? (:key-count test)))
      (let [reads (->> history
                       (r/filter op/ok?)
                       (r/filter #(= :read (:f %)))
                       (r/map :value)
                       (into []))
            none (filter (comp (partial every? nil?) second) reads)
            some (filter (comp (partial some nil?) second) reads)
            bad  (filter (comp trailing-nil? second) reads)
            all  (filter (fn [[k ks]]
                           (= (subkeys (:key-count test) k)
                             (reverse ks)))
                         reads)]
        {:valid?      (not (seq bad))
         :all-count   (count all)
         :some-count  (count some)
         :none-count  (count none)
         :bad-count   (count bad)
         :bad         bad}))))
