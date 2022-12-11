#!/bin/sh

set -u

ELLE_CLI_VERSION="0.1.6"
ELLE_CLI_BIN="java -jar target/elle-cli-${ELLE_CLI_VERSION}-standalone.jar"

suite_status=0

run_test() {
    exit_code=$1
    elle_cli_opts=$2

    shift
    test_status="NOT OK"
    cmd="$ELLE_CLI_BIN $elle_cli_opts"
    test_output=$($cmd)
    rc=$?
    res=$(echo $test_output | cut -d" " -f2)
    if { { [ "X$res" = Xfalse ] && [ "$exit_code" -ne 0 ]; } ||
         { [ "X$res" = Xtrue ] && [ "$exit_code" -eq 0 ]; }; } &&
       [ "$exit_code" -eq $rc ]; then
        test_status="OK"
    else
        suite_status=1
    fi
    printf "%8s %s\n" "[$test_status]" "$cmd"
}

run_test 1 "--model cas-register histories/knossos/cas-register/bad/bad-analysis.edn"
run_test 1 "--model cas-register histories/knossos/cas-register/bad/cas-failure.edn"
run_test 1 "--model cas-register histories/knossos/cas-register/bad/mongodb-v0-ack-rollback-6.edn"
run_test 1 "--model cas-register histories/knossos/cas-register/bad/rethink-fail.edn"
run_test 1 "--model cas-register histories/knossos/cas-register/bad/rethink-fail-minimal.edn"
run_test 1 "--model cas-register histories/knossos/cas-register/bad/rethink-fail-smaller.edn"
run_test 0 "--model cas-register histories/knossos/cas-register/good/memstress3-9.edn"
run_test 0 "--model cas-register histories/knossos/cas-register/good/memstress3-9.json"

run_test 1 "--model mutex histories/knossos/mutex/bad/etcd.edn"
run_test 1 "--model mutex histories/knossos/mutex/bad/etcd.json"
run_test 1 "--model mutex histories/knossos/mutex/bad/hazelcast.edn"

run_test 0 "--model rw-register histories/elle/rw-register.edn"
run_test 0 "--model rw-register histories/elle/rw-register.json"
run_test 1 "--model list-append histories/elle/paper-example.edn"
run_test 1 "--model list-append histories/elle/paper-example.json"
run_test 1 "--model list-append histories/elle/paper-example.json --plot-format svg"
run_test 0 "--model list-append histories/elle/paper-example.json --anomalies G-single-process --consistency-models linearizable"
run_test 1 "--model list-append histories/elle/paper-example.json --cycle-search-timeout 1000"
run_test 1 "--model list-append histories/elle/paper-example.json --plot-timeout 5000"
run_test 1 "--model list-append histories/elle/paper-example.json --max-plot-bytes 65536"
run_test 1 "--model list-append histories/elle/list-append-gh-30.edn --consistency-models serializable"

run_test 0 "--model counter histories/jepsen/counter.edn"
run_test 0 "--model counter histories/jepsen/counter.json"

run_test 1 "--model set-full histories/jepsen/set_full.edn"
run_test 1 "--model set-full histories/jepsen/set_full.json"

run_test 1 "--model bank histories/jepsen/bank.edn"
run_test 1 "--model bank histories/jepsen/bank.json"


exit $suite_status
