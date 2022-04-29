(ns elle_cli.cli
  "History verification in CLI"
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer [info warn]]
            [jepsen [checker :as jepsen-model]]
            [jepsen.tests.bank :as jepsen-bank]
            [jepsen.tests.long-fork :as jepsen-long-fork]
            [elle.list-append :as elle-list-append]
            [elle.rw-register :as elle-rw-register]
            [elle.consistency-model :as elle-consistency-model]
            [knossos [history :as history]
                     [competition :as competition]
                     [model :as knossos-model]])

  (:gen-class)
  (:import (java.io PushbackReader)))

(defn read-edn-history
  "Takes a path to file and loads a history from it, in EDN format. If an
  initial [ or ( is present, loads the entire file in one go as a collection,
  expecting it to be a collection of op maps. If the initial character is {,
  loads it piecewise as op maps."
  [filepath]
  (let [h (with-open [r (PushbackReader. (io/reader filepath))]
            (->> (repeatedly #(edn/read {:eof nil} r))
                 (take-while identity)
                 vec))
        ; Handle the presence or absence of an enclosing vector.
        h (if (and (= 1 (count h))
                   (sequential? (first h)))
            (vec (first h))
            h)]
    h
    ))

(defn vl
  [v]
  (if (string? v)
    (keyword v)
    v)
  (if (vector? v) (vl v)))

(defn value-reader [key value]
  (cond
    (or (= key :type) (= key :f)) (keyword value)
    ;; e.g. {:value [[:append :x 1] [:r :y nil]]}
    (and
      (= key :value)
      (vector? value)  (not (empty? value))
      (vector? (first value)) (not (empty? (first value)))
      (string? (ffirst value)))    (map #(assoc %1 0 (keyword (first %1))) value)
    :else value))

(defn read-json-history
  "Takes a path to file and loads a history from it, in JSON format."
  [filepath]
  (json/read-str (slurp filepath)
                  :key-fn keyword
                  :value-fn value-reader))

(defn str->keywords [s]
  (if (empty? s)
    []
    (mapv keyword (str/split s #","))))

(def models
  {"knossos-register"        knossos-model/register
   "knossos-cas-register"    knossos-model/cas-register
   "knossos-mutex"           knossos-model/mutex
   "register"                knossos-model/register
   "cas-register"            knossos-model/cas-register
   "mutex"                   knossos-model/mutex
   "jepsen-bank"             jepsen-bank/checker
   "jepsen-long-fork"        jepsen-long-fork/checker
   "jepsen-counter"          jepsen-model/counter
   "jepsen-set"              jepsen-model/set
   "jepsen-set-full"         jepsen-model/set-full
   "bank"                    jepsen-bank/checker
   "long-fork"               jepsen-long-fork/checker
   "counter"                 jepsen-model/counter
   "set"                     jepsen-model/set
   "set-full"                jepsen-model/set-full
   "elle-rw-register"        elle-rw-register/check
   "elle-list-append"        elle-list-append/check
   "rw-register"             elle-rw-register/check
   "list-append"             elle-list-append/check})

(def history-read-fn
  {"edn"        read-edn-history
   "json"       read-json-history})

(def opts
  "tools.cli options"

   ; General options.
  [["-m" "--model MODEL"
    "(General) A name of consistency model for checking."
    :validate [identity
               (str "Must be one of " (str/join ", " (sort (keys models))))]]
   ["-f" "--format FORMAT"
    "(General) Format of file with history. Either 'edn' or 'json'."
    :parse-fn history-read-fn
    :validate [identity
               (str "Must be one of " (str/join ", " (sort (keys history-read-fn))))]]
   ["-v" "--verbose"
    "(General) Enable verbose mode."]
   ["-h" "--help"
    "(General) Print usage."]

   ; Elle-specific options.
   ["-c" "--consistency-models CONSISTENCY-MODELS"
    "(Elle) A collection of consistency models we expect this history to obey."
    :default [:strict-serializable]
    :parse-fn str->keywords]
   ["-a" "--anomalies ANOMALIES"
    "(Elle) A collection of specific anomalies you'd like to look for."
    :default [:G0]
    :parse-fn str->keywords]
   ["-s" "--cycle-search-timeout CYCLE-SEARCH-TIMEOUT"
    "(Elle) Number of ms for searching a single SCC for a cycle."
    :default 1000
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--directory DIRECTORY"
    "(Elle) Where to output files, if desired."
    :default nil]
   ["-p" "--plot-format PLOT-FORMAT"
    "(Elle) Either 'png' or 'svg'."
    :default :svg
    :parse-fn keyword]
   ["-t" "--plot-timeout PLOT-TIMEOUT"
    "(Elle) How many milliseconds will we wait to render a SCC plot?"
    :default 5000
    :parse-fn #(Integer/parseInt %)]
   ["-b" "--max-plot-bytes MAX-PLOT-BYTES"
    "(Elle) Maximum size of a cycle graph (in bytes of DOT)."
    :default 65536
    :parse-fn #(Integer/parseInt %)]

   ; Jepsen-specific options.
   ; None.

   ; Knossos-specific options.
   ; None.
   ])

(defn usage [options-summary]
  (->> ["elle-cli - command-line transactional safety checker."
        ""
        "Usage: elle-cli -m model [options] files"
        ""
        "Supported models:"
        "  rw-register - an checker for write-read registers."
        "  list-append - an checker for append and read histories."
        "  bank - a checker for bank histories."
        "  counter - a checker for counter histories."
        "  set - a checker for a set histories."
        "  set-full - a checker for a set histories."
        "  long-fork - a checker for an anomaly in parallel snapshot isolation."
        "  cas-register - a checker for CAS (Compare-And-Set) registers."
        "  mutex - a checker for a mutex histories."
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn check-history
  "Check a specified history according to model specified by model name"
  [model-name history options]

  (let [checker-fn (get models model-name)]
    (case model-name
       ; Operations in a histories passed to a Knossos additionally normalized,
       ; see src/knossos/cli.clj:read-history.
       "register" (competition/analysis (checker-fn) (history/parse-ops history))
       "cas-register" (competition/analysis (checker-fn) (history/parse-ops history))
       "mutex" (competition/analysis (checker-fn) (history/parse-ops history))
       "knossos-register" (competition/analysis (checker-fn) (history/parse-ops history))
       "knossos-cas-register" (competition/analysis (checker-fn) (history/parse-ops history))
       "knossos-mutex" (competition/analysis (checker-fn) (history/parse-ops history))
       "list-append" (checker-fn options history)
       "rw-register" (checker-fn options history)
       "elle-list-append" (checker-fn options history)
       "elle-rw-register" (checker-fn options history)
       "bank" (jepsen-model/check-safe (checker-fn {:negative-balances? true}) nil history)
       "counter" (jepsen-model/check-safe (checker-fn) nil history)
       "set-full" (jepsen-model/check-safe (checker-fn) nil history)
       "jepsen-bank" (jepsen-model/check-safe (checker-fn {:negative-balances? true}) nil history)
       "jepsen-counter" (jepsen-model/check-safe (checker-fn) nil history)
       "jepsen-set-full" (jepsen-model/check-safe (checker-fn) nil history))))

(defn read-fn-by-extension
  "Take a path to file and returns a function for reading that file."
  [filepath]
  (let [ext (second (re-find #"\.([a-zA-Z0-9]+)$" filepath))]
    (get history-read-fn ext)))

(defn -main
  [& args]
  (try
    (let [{:keys [options arguments summary errors]} (cli/parse-opts args opts)
          model-name (:model options)
          read-history (:format options)
          help (:help options)]
      (when-not (empty? errors)
        (doseq [e errors]
          (println e))
        (System/exit 1))

      (if (or (nil? model-name) (true? help)) (
          (println (usage summary))
          (System/exit 0)))

      (doseq [filepath arguments]
        (if (not (.exists (io/as-file filepath)))
          (throw (Exception. "File not found")))

        (let [read-history  (or read-history (read-fn-by-extension filepath))
              history       (read-history filepath)
              analysis      (check-history model-name history options)]

          (if (true? (:verbose options))
            (json/pprint analysis)
            (println filepath "\t" (:valid? analysis)))))

      (System/exit 0))

    (catch Throwable t
      (println)
      (.printStackTrace t)
      (System/exit 255))))
