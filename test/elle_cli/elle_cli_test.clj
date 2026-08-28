(ns elle-cli.elle-cli-test
  "Unit tests that run the elle-cli checker on operation histories from
  test-data and compare the checker's result with the expected result stored
  in <case>-result.json. If a result file is missing, it is generated
  automatically from the checker's output."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [jepsen.history :as h]
            [elle_cli.cli :as cli]))

(def data-dir
  "Directory with test histories and expected results."
  "test/test-data")

(def model-cases
  "A mapping of model name to test cases. Each case corresponds to a history
  file <case>.edn or <case>.json in data-dir."
  {"cas-register" ["bad-analysis"
                   "cas-failure"
                   "mongodb-v0-ack-rollback-6"
                   "rethink-fail"
                   "rethink-fail-minimal"
                   "rethink-fail-smaller"
                   "memstress3-9"]
   "comments"     [; A committed read fails to observe an earlier completed write.
                   "comments"]
   "mutex"        ["etcd"
                   "hazelcast"]
   "rw-register"  ["rw-register"
                   ; Empty history with no operations.
                   "rw-register-valid-empty"
                   ; Single write of x=2 followed by read returning 2.
                   "rw-register-valid-single-write-read"
                   ; Operations on different keys (x and y), no interaction.
                   "rw-register-valid-different-keys"
                   ; Write x=1, then write x=2, then read returns 2 (consistent).
                   "rw-register-valid-overwrite-read"
                   ; Read of a key that was never written (returns nil).
                   "rw-register-valid-read-nil"
                   ; Only write operations, no reads at all.
                   "rw-register-valid-write-only"
                   ; Same transaction writes x=2 then reads x=3 (inconsistent).
                   "rw-register-anomaly-internal"
                   ; G1a: aborted read - a transaction reads a value written by a failed transaction.
                   "rw-register-anomaly-g1a-aborted-read"
                   ; G1b: intermediate read - a transaction reads an overwritten intermediate write.
                   "rw-register-anomaly-g1b-intermediate-read"
                   ; Lost update: two transactions read the same value of x and both write to x.
                   "rw-register-anomaly-lost-update"
                   ; G-single-item: a cycle with one read-write anti-dependency via nil initial state.
                   "rw-register-anomaly-g-single-item"
                   ; G2-item: a cycle with two read-write anti-dependencies via nil initial state.
                   "rw-register-anomaly-g2-item"
                   ; Version order inferred from per-key sequential consistency.
                   "rw-register-keys-sequential-valid"
                   ; Sequential version inference over a process-order cycle.
                   "rw-register-keys-sequential-anomaly"
                   ; Version order inferred from per-key linearizability.
                   "rw-register-keys-linearizable-valid"
                   ; Cyclic version order inferred from per-key linearizability.
                   "rw-register-keys-linearizable-anomaly"
                   ; Version order inferred from writes-follow-reads within a transaction.
                   "rw-register-keys-wfr-valid"
                   ; Version order inferred from an externally supplied transaction order.
                   "rw-register-transaction-order-valid"
                   ; G1c-process cycle: only visible when process order is considered.
                   "rw-register-process-anomaly"
                   ; Same G2-item history checked under different consistency models.
                   "rw-register-model-serializable"
                   "rw-register-model-snapshot-isolation"
                   "rw-register-model-strong-snapshot"
                   ; History with an :info completion and an orphan invocation.
                   "rw-register-partial-info"
                   ; Same transaction reads the same key twice.
                   "rw-register-valid-repeat-read"]
   "list-append"  ["paper-example"
                   "list-append-gh-30"
                   ; Empty history with no operations.
                   "list-append-valid-empty"
                   ; Single append of value 1 to key 1, followed by read of key 1 returning [1].
                   "list-append-valid-single-append-read"
                   ; Multiple appends to same key with consistent read.
                   "list-append-valid-multiple-appends"
                   ; Operations on different keys, no interaction.
                   "list-append-valid-different-keys"
                   ; Read of a key that was never modified (returns an empty list).
                   "list-append-valid-read-empty"
                   ; Only append operations, no reads at all.
                   "list-append-valid-append-only"
                   ; Multiple processes appending to same key with consistent read.
                   "list-append-valid-multiple-processes"
                   ; G0: write-write cycle - two transactions append to two keys in conflicting order.
                   "list-append-anomaly-g0-cycle"
                   ; G1b: intermediate read - a transaction reads an intermediate (overwritten) append.
                   "list-append-anomaly-g1b-intermediate-read"
                   ; G1c: circular information flow - write-write edge combined with write-read edge forming a cycle.
                   "list-append-anomaly-g1c-circular-information-flow"
                   ; G-single-item: one read-write anti-dependency in a cycle.
                   "list-append-anomaly-g-single-item"
                   ; G2-item: two read-write anti-dependencies forming a cycle.
                   "list-append-anomaly-g2-item"
                   ; Observed list values for a key are not prefix-compatible.
                   "list-append-anomaly-incompatible-order"
                   ; Lost update: two transactions read the same list state and both append to it.
                   "list-append-anomaly-lost-update"
                   ; Dirty update: a committed append follows a failed one in the version order.
                   "list-append-anomaly-dirty-update"
                   ; Future read: a transaction reads a value it will later append in the same transaction.
                   "list-append-anomaly-future-read"
                   ; Duplicate elements: a read returns the same element more than once.
                   "list-append-anomaly-duplicate-elements"
                   ; G1a: aborted read - a transaction reads data written by a failed transaction.
                   "list-append-anomaly-g1a-aborted-read"
                   ; Non-repeatable read within a single transaction.
                   "list-append-anomaly-internal"
                   "list-append-anomaly-partial-read-diff-processes"
                   ; Same transaction appending multiple values to same key, two reads
                   ; in one txn observing different subsets.
                   "list-append-anomaly-partial-read-single-txn"
                   ; Single txn appends three values, read observes only first two.
                   "list-append-anomaly-append-too-many-partial-read"
                   ; Single txn appends three values, two reads observe different subsets.
                   "list-append-anomaly-append-too-many-partial-read-ext"
                   ; Two separate appends, one txn with two reads observing different subsets.
                   "list-append-anomaly-partial-read-twice"
                   ; Internal read mismatch against an unknown prefix of the list.
                   "list-append-internal-unknown-prefix"
                   ; Dirty update and aborted reads with :ok/:info/:fail appends.
                   "list-append-dirty-update-info-fail"
                   ; Same paper-example history checked under weaker consistency models.
                   "list-append-model-read-uncommitted"
                   "list-append-model-snapshot-isolation"
                   "list-append-model-read-committed"
                   ; :info completion that reads a nil (crashed) state.
                   "list-append-valid-info-read"]
   "bank"         ["bank"
                   "bank-tidb"
                   "bank-neg"
                   "bank-negative-balances"]
   "counter"      ["counter"]
   "set"          ["set"]
   "set-full"     ["set_full"]
   "long-fork"    ["long-fork"]})

(def default-opts
  "Default checker options, mirroring the CLI defaults."
  {:consistency-models [:strict-serializable]
   :anomalies [:G0]
   :cycle-search-timeout 1000
   :plot-format :svg
   :plot-timeout 5000
   :max-plot-bytes 65536
   :group-size 0
   :allow-negative-balances false})

(def case-opts
  "Per-case checker options overriding the defaults."
  {"list-append-gh-30"                       {:consistency-models [:serializable]}
   "long-fork"                               {:group-size 3}
   "rw-register-keys-sequential-valid"       {:sequential-keys? true}
   "rw-register-keys-sequential-anomaly"     {:sequential-keys? true}
   "rw-register-keys-linearizable-valid"     {:linearizable-keys? true}
   "rw-register-keys-linearizable-anomaly"   {:linearizable-keys? true}
   "rw-register-keys-wfr-valid"              {:wfr-keys? true}
   "rw-register-transaction-order-valid"     {:transaction-order {1 0, 3 1, 5 2}}
   "rw-register-process-anomaly"             {:consistency-models [:strong-session-serializable]}
   "rw-register-model-serializable"          {:consistency-models [:serializable]}
   "rw-register-model-snapshot-isolation"    {:consistency-models [:snapshot-isolation]}
   "rw-register-model-strong-snapshot"       {:consistency-models [:strong-snapshot-isolation]}
   "rw-register-partial-info"                {:linearizable-keys? true}
   "list-append-model-read-uncommitted"      {:consistency-models [:read-uncommitted]}
   "list-append-model-snapshot-isolation"    {:consistency-models [:snapshot-isolation]}
   "list-append-model-read-committed"        {:consistency-models [:read-committed]}})

(def option-cases
  "Per-option regression cases, mirroring the individual options exercised by
  the old shell-based test runner (test.sh). Each entry checks one history with
  a single option override; the expected result is the same <case>-result.json."
  [{:model "list-append" :case "paper-example"     :opts {:plot-format :svg}}
   {:model "list-append" :case "paper-example"     :opts {:cycle-search-timeout 1000}}
   {:model "list-append" :case "paper-example"     :opts {:plot-timeout 5000}}
   {:model "list-append" :case "paper-example"     :opts {:max-plot-bytes 65536}}
   {:model "list-append" :case "list-append-gh-30" :opts {:consistency-models [:serializable]}}])

(def partial-comparison-models
  "Knossos competition/analysis results are non-deterministic: the reported
  configurations differ between runs, so only :valid? is compared for these
  models."
  #{"cas-register" "mutex"})

(def skipped-cases
  "Test cases to skip. Their histories fail to run because of
   a bug in the checker (jepsen-io/elle#32)."
  #{"list-append-anomaly-g1a-aborted-read"
    "list-append-anomaly-internal"
    "list-append-anomaly-partial-read-diff-processes"
    "list-append-anomaly-partial-read-single-txn"
    "list-append-anomaly-append-too-many-partial-read"
    "list-append-anomaly-append-too-many-partial-read-ext"
    "list-append-anomaly-partial-read-twice"
    "list-append-anomaly-dirty-update"
    "list-append-dirty-update-info-fail"
    "list-append-model-read-uncommitted"
    "list-append-model-snapshot-isolation"
    "list-append-model-read-committed"})

(defn history-file
  "Path to the history file for a test case."
  [case]
  (let [edn (io/file data-dir (str case ".edn"))
        json (io/file data-dir (str case ".json"))]
    (cond
      (.exists edn)  (.getPath edn)
      (.exists json) (.getPath json)
      :else (throw (Exception. (str "No history file found for case: " case))))))

(defn result-file
  "Path to the expected result file for a test case."
  [case]
  (io/file data-dir (str case "-result.json")))

(defn read-ops
  "Read operations from a history file, choosing the reader by extension."
  [path]
  ((cli/read-fn-by-extension path) path))

(defn check-case
  "Run the checker for a given model and case with the given extra options,
  returning the analysis."
  [model case & [extra-opts]]
  (cli/check-history model
                     (h/history (read-ops (history-file case)))
                     (merge default-opts (get case-opts case) extra-opts)))

(defn analysis->json
  "Serialize an analysis map to JSON and parse it back."
  [analysis]
  (json/read-str (json/write-str analysis) :key-fn keyword))

(defn canon
  "Order-insensitive normalization of parsed JSON data for comparison."
  [x]
  (cond
    (map? x)    (into (sorted-map) (map (fn [[k v]] [k (canon v)]) x))
    (vector? x) (vec (sort-by pr-str (map canon x)))
    (seq? x)    (canon (vec x))
    (set? x)    (canon (vec x))
    :else x))

(defn assert-result
  "Compare the checker's analysis against the stored expected result for a case,
  generating the result file if it does not exist yet."
  [model case analysis]
  (let [actual   (canon (analysis->json analysis))
        expected (result-file case)]
    (if (.exists expected)
      (let [expected (canon (json/read-str (slurp expected) :key-fn keyword))]
        (is (= (:valid? expected) (:valid? actual))
            (str "valid? differs from the expected result"))
        (when-not (partial-comparison-models model)
          (is (= expected actual)
              "checker result differs from the expected result")))
      (do (spit expected
                (with-out-str (json/pprint (if (partial-comparison-models model)
                                              (select-keys analysis [:valid?])
                                              analysis))))
          (is (.exists expected)
              (str "generated expected result: " (.getPath expected)))))))

(deftest check-histories
  (doseq [[model cases] model-cases
          case cases]
    (if (contains? skipped-cases case)
      (println "SKIPPED:" model ":" case)
      (testing (str model ": " case)
        (assert-result model case (check-case model case))))))

(deftest check-options
  (doseq [{:keys [model case opts]} option-cases]
    (testing (str model ": " case " with options " (pr-str opts))
      (assert-result model case (check-case model case opts)))))
