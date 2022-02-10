#!/bin/sh

set -eu

ELLE_CLI_VERSION="0.1.0"
ELLE_CLI_BIN="java -jar target/elle-cli-${ELLE_CLI_VERSION}-standalone.jar"
ELLE_CLI_OPT="--model"

$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/bad-analysis.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/cas-failure.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/mongodb-v0-ack-rollback-6.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/rethink-fail.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/rethink-fail-minimal.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/bad/rethink-fail-smaller.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/good/memstress3-9.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-cas-register histories/knossos/cas-register/good/memstress3-9.json

$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-mutex histories/knossos/mutex/bad/etcd.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-mutex histories/knossos/mutex/bad/etcd.json
$ELLE_CLI_BIN $ELLE_CLI_OPT knossos-mutex histories/knossos/mutex/bad/hazelcast.edn

$ELLE_CLI_BIN $ELLE_CLI_OPT elle-rw-register histories/elle/rw-register.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT elle-rw-register histories/elle/rw-register.json
$ELLE_CLI_BIN $ELLE_CLI_OPT elle-list-append histories/elle/paper-example.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT elle-list-append histories/elle/paper-example.json
$ELLE_CLI_BIN $ELLE_CLI_OPT elle-list-append histories/elle/paper-example.json --plot-format svg

$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-counter histories/jepsen/counter.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-counter histories/jepsen/counter.json

$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-set-full histories/jepsen/set_full.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-set-full histories/jepsen/set_full.json

$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-bank histories/jepsen/bank.edn
$ELLE_CLI_BIN $ELLE_CLI_OPT jepsen-bank histories/jepsen/bank.json
