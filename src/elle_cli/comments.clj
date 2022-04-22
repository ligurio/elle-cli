(ns elle-cli.comments
  "Checks for a strict serializability anomaly in which T1 < T2, but T2 is
  visible without T1.
  We perform concurrent blind inserts across n tables, and meanwhile, perform
  reads of both tables in a transaction. To verify, we replay the history,
  tracking the writes which were known to have completed before the invocation
  of any write w_i. If w_i is visible, and some w_j < w_i is *not* visible,
  we've found a violation of strict serializability.
  Splits keys up onto different tables to make sure they fall in different
  shard ranges"
  (:require [jepsen.checker :as checker]
            [knossos.model :as model]
            [knossos.op :as op]
            [clojure.core.reducers :as r]
            [clojure.set :as set]))

; Source: https://github.com/jepsen-io/jepsen/blob/main/cockroachdb/src/jepsen/cockroach/comments.clj
(defn checker
  []
  (reify checker/Checker
    (check [this test history opts]
      ; Determine first-order write precedence graph
      (let [expected (loop [completed  (sorted-set)
                            expected   {}
                            [op & more :as history] history]
                       (cond
                         ; Done
                         (not (seq history))
                         expected

                         ; We know this value is definitely written
                         (= :write (:f op))
                         (cond ; Write is beginning; record precedence
                               (op/invoke? op)
                               (recur completed
                                      (assoc expected (:value op) completed)
                                      more)

                               ; Write is completing; we can now expect to see
                               ; it
                               (op/ok? op)
                               (recur (conj completed (:value op))
                                      expected more)

                               true
                               (recur completed expected more))

                         true
                         (recur completed expected more)))
            errors (->> history
                        (r/filter op/ok?)
                        (r/filter #(= :read (:f %)))
                        (reduce (fn [errors op]
                                  (let [seen         (:value op)
                                        our-expected (->> seen
                                                          (map expected)
                                                          (reduce set/union))
                                        missing (set/difference our-expected
                                                                seen)]
                                    (if (empty? missing)
                                      errors
                                      (conj errors
                                            (-> op
                                                (dissoc :value)
                                                (assoc :missing missing)
                                                (assoc :expected-count
                                                       (count our-expected)))))))
                                []))]
        {:valid? (empty? errors)
         :errors errors}))))
