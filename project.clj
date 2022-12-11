(defproject elle-cli "0.1.6"
  :description "Command-line frontend to transactional consistency checkers for black-box databases"
  :url "https://github.com/ligurio/elle-cli"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :jvm-opts ["-Xmx32g"
             "-Djava.awt.headless=true"
             "-server"]
  :main elle_cli.cli
  :aot [elle_cli.cli]
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/data.json "2.4.0"]
                 [spootnik/unilog "0.7.28"] ; required by elle
                 [elle "0.1.6"]
                 [jepsen "0.3.0"]
                 [knossos "0.3.8"]])
