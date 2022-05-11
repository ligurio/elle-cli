#!/bin/sh

set -eu

ELLE_CLI_VERSION="0.1.3"
ELLE_CLI_BIN="java -jar target/elle-cli-${ELLE_CLI_VERSION}-standalone.jar"
ELLE_CLI_OPT="--model"

$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/bad-analysis.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/cas-failure.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/mongodb-v0-ack-rollback-6.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/rethink-fail.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/rethink-fail-minimal.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/bad/rethink-fail-smaller.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/good/memstress3-9.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT cas-register histories/knossos/cas-register/good/memstress3-9.json

$ELLE_CLI_BIN $ELLE_CLI_OPT mutex histories/knossos/mutex/bad/etcd.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT mutex histories/knossos/mutex/bad/etcd.json
$ELLE_CLI_BIN $ELLE_CLI_OPT mutex histories/knossos/mutex/bad/hazelcast.edn

$ELLE_CLI_BIN $ELLE_CLI_OPT rw-register histories/elle/rw-register.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT rw-register histories/elle/rw-register.json
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json --plot-format svg
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json --anomalies G-single-process --consistency-models ''
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json --cycle-search-timeout 1000
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json --plot-timeout 5000
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/paper-example.json --max-plot-bytes 65536
$ELLE_CLI_BIN $ELLE_CLI_OPT list-append histories/elle/list-append-gh-30.edn --consistency-models serializable

$ELLE_CLI_BIN $ELLE_CLI_OPT counter histories/jepsen/counter.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT counter histories/jepsen/counter.json

$ELLE_CLI_BIN $ELLE_CLI_OPT set-full histories/jepsen/set_full.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT set-full histories/jepsen/set_full.json

$ELLE_CLI_BIN $ELLE_CLI_OPT bank histories/jepsen/bank.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT bank histories/jepsen/bank.json
