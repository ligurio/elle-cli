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
   "mutex"        ["etcd"
                   "hazelcast"]
   "rw-register"  ["rw-register"]
   "list-append"  ["paper-example"
                   "list-append-gh-30"]
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
  {"list-append-gh-30" {:consistency-models [:serializable]}
   "long-fork"         {:group-size 3}})

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
    (testing (str model ": " case)
      (assert-result model case (check-case model case)))))

(deftest check-options
  (doseq [{:keys [model case opts]} option-cases]
    (testing (str model ": " case " with options " (pr-str opts))
      (assert-result model case (check-case model case opts)))))
