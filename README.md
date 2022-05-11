# elle-cli

[![Testing](https://github.com/ligurio/elle-cli/actions/workflows/test.yaml/badge.svg)](https://github.com/ligurio/elle-cli/actions/workflows/test.yaml)

is a command-line tool with black-box transactional safety checkers. In
comparison to Jepsen library it is standalone and language-agnostic tool. You
can use it with tests written in any programming language and everywhere where
JVM is available. Under the hood `elle-cli` uses libraries
[Elle](https://github.com/jepsen-io/elle),
[Knossos](https://github.com/jepsen-io/knossos) and
[Jepsen](https://github.com/jepsen-io/jepsen) and provides the same correctness
guarantees.

Jepsen, Elle and Knossos supports histories only in [EDN (Extensible Data
Notation)](https://github.com/edn-format/edn), it is a format for serializing
data that was invented by Rich Hickey, author of Clojure, for using in Clojure
applications. Typical data serialized to EDN looks quite similar to JSON:

```clojure
{:type :invoke, :f :read,     :process 2, :time 53137939465, :index 0}
{:type :invoke, :f :transfer, :process 3, :time 53137939133, :index 1, :value {:from 5, :to 2, :amount 2}}
{:type :invoke, :f :read,     :process 1, :time 53139785248, :index 2}
{:type :invoke, :f :transfer, :process 0, :time 53139856763, :index 3, :value {:from 7, :to 9, :amount 4}}
{:type :invoke, :f :read,     :process 4, :time 53155597745, :index 4}
```

However, outside of the Clojure ecosystem EDN format is practically not used.
`elle-cli` operates with operations history both in EDN and [JSON (JavaScript
Object Notation)](https://www.json.org/) formats and can be successfully used
with histories produced by Jepsen tests in EDN format as well as with other
Jepsen-similar frameworks that produce histories in JSON format.

## Usage

If you have a file with history written in EDN or JSON format, either as a
series of operation maps, or as a single vector or list containing those
operations, you can ask `elle-cli` to check it for you at the command line like
so:

```sh
$ git clone https://github.com/ligurio/elle-cli
$ cd elle-cli
$ lein deps
$ lein uberjar
Compiling elle_cli.cli
Created /home/sergeyb/sources/ljepsen/elle-cli/target/elle-cli-0.1.3.jar
Created /home/sergeyb/sources/ljepsen/elle-cli/target/elle-cli-0.1.3-standalone.jar
$ java -jar target/elle-cli-0.1.3-standalone.jar --model rw-register histories/elle/rw-register.json
histories/elle/rw-register.edn        true
```

`elle-cli` converts files with histories in JSON format automatically to
Clojure data structures and prints out the names of all files you asked it to
check, followed by a tab, and then whether the history was valid. There are
three validity states:

- `true`      means the history was valid
- `false`     means the history was valid
- `:unknown`  means checker was unable to complete the analysis; e.g. it ran
              out of memory.

In some cases conversion of history from JSON format to Clojure data structures
may fail and it is definitely a bug that should be reported. To workaround I
recommend to use a tool [jet](https://github.com/borkdude/jet), it is a CLI to
transform between JSON and EDN, and then pass file in EDN format to `elle-cli`.

## Supported models

### rw-register

An Elle's checker for write-read registers. Options are:

- **consistency-models** - a collection of consistency models we expect this
  history to obey. Defaults to `strict-serializable`. Possible values are:
  `consistent-view`, `conflict-serializable`, `cursor-stability`,
  `forward-consistent-view`, `monotonic-snapshot-read`, `monotonic-view`,
  `read-committed`, `read-uncommitted`, `repeatable-read`, `serializable`,
  `snapshot-isolation`, `strict-serializable`, `update-serializable`.
- **anomalies** - a collection of specific anomalies you'd like to look for.
  Defaults to `G0`. Possible values are: `G0`, `G0-process`, `G0-realtime`,
  `G1a`, `G1b`, `G1c`, `G1c-process`, `G-single`, `G-single-process`,
  `G-single-realtime`, `G-nonadjacent`, `G-nonadjacent-process`,
  `G-nonadjacent-realtime`, `G2-item`, `G2-item-process`, `G2-item-realtime`,
  `G2-process`, `GSIa`, `GSIb`, `incompatible-order`, `dirty-update`.
- **cycle-search-timeout** - how many milliseconds are we willing to search a
  single SCC for a cycle? Default value is `1000`.
- **directory** - where to output files, if desired. Default value is `nil`.
- **plot-format** - either `png` or `svg`. Default value is `svg`.
- **plot-timeout** - how many milliseconds will we wait to render a SCC plot?
  Default value is `5000`.
- **max-plot-bytes** - maximum size of a cycle graph (in bytes of DOT) which
  we're willing to try and render. Default value is `65536`.

Example of history:

```clojure
{:type :invoke, :f :txn :value [[:w :x 2]],   :process 0, :index 1}
{:type :ok,     :f :txn :value [[:w :x 2]],   :process 0, :index 2}
{:type :invoke, :f :txn :value [[:r :x nil]], :process 0, :index 3}
{:type :ok,     :f :txn :value [[:r :x 3]],   :process 0, :index 4}
{:type :invoke, :f :txn :value [[:r :x nil]], :process 0, :index 5}
{:type :ok,     :f :txn :value [[:r :x 2]],   :process 0, :index 6}
```

### list-append

An Elle's checker for append and read histories.
Options are:

- **consistency-models** - a collection of consistency models we expect this
  history to obey. Defaults to `strict-serializable`. Possible values are:
  `consistent-view`, `conflict-serializable`, `cursor-stability`,
  `forward-consistent-view`, `monotonic-snapshot-read`, `monotonic-view`,
  `read-committed`, `read-uncommitted`, `repeatable-read`, `serializable`,
  `snapshot-isolation`, `strict-serializable`, `update-serializable`.
- **anomalies** - a collection of specific anomalies you'd like to look for.
  Defaults to `G0`. Possible values are: `G0`, `G0-process`, `G0-realtime`,
  `G1a`, `G1b`, `G1c`, `G1c-process`, `G-single`, `G-single-process`,
  `G-single-realtime`, `G-nonadjacent`, `G-nonadjacent-process`,
  `G-nonadjacent-realtime`, `G2-item`, `G2-item-process`, `G2-item-realtime`,
  `G2-process`, `GSIa`, `GSIb`, `incompatible-order`, `dirty-update`.
- **cycle-search-timeout** - how many milliseconds are we willing to search a
  single SCC for a cycle? Default value is `1000`.
- **directory** - where to output files, if desired. Default value is `nil`.
- **plot-format** - either `png` or `svg`. Default value is `svg`.
- **plot-timeout** - how many milliseconds will we wait to render a SCC plot?
  Default value is `5000`.
- **max-plot-bytes** - maximum size of a cycle graph (in bytes of DOT) which
  we're willing to try and render. Default value is `65536`.

Example of history:

```clojure
{:index 2 :type :invoke, :value [[:append 255 8] [:r 253 nil]]}
{:index 3 :type :ok,     :value [[:append 255 8] [:r 253 [1 3 4]]]}
{:index 4 :type :invoke, :value [[:append 256 4] [:r 255 nil] [:r 256 nil] [:r 253 nil]]}
{:index 5 :type :ok,     :value [[:append 256 4] [:r 255 [2 3 4 5 8]] [:r 256 [1 2 4]] [:r 253 [1 3 4]]]}
{:index 6 :type :invoke, :value [[:append 250 10] [:r 253 nil] [:r 255 nil] [:append 256 3]]}
{:index 7 :type :ok      :value [[:append 250 10] [:r 253 [1 3 4]] [:r 255 [2 3 4 5]] [:append 256 3]]}
```

### bank

A Jepsen's checker for bank histories. Option `negative-balances` is always
enabled.

Example of history:

```clojure
{:type :invoke, :f :transfer, :process 0, :time 12613722542, :index 34, :value {:from 1, :to 0, :amount 5}}
{:type :fail,   :f :transfer, :process 0, :time 12686176735, :index 35, :value {:from 1, :to 0, :amount 5}}
{:type :invoke, :f :read,     :process 0, :time 12686563291, :index 36}
{:type :ok,     :f :read,     :process 0, :time 12799165489, :index 37, :value {0 97, 1 0, 2 0, 3 0, 4 0, 5 3, 6 0, 7 0, 8 0, 9 0}}
{:type :invoke, :f :transfer, :process 0, :time 12799587097, :index 38, :value {:from 6, :to 5, :amount 3}}
{:type :fail,   :f :transfer, :process 0, :time 12903632203, :index 39, :value {:from 6, :to 5, :amount 3}}
{:type :invoke, :f :read,     :process 0, :time 12903998176, :index 40}
{:type :ok,     :f :read,     :process 0, :time 13005165731, :index 41, :value {0 97, 1 0, 2 0, 3 0, 4 0, 5 3, 6 0, 7 0, 8 0, 9 0}}
{:type :invoke, :f :read,     :process 0, :time 13005675266, :index 42}
{:type :ok,     :f :read,     :process 0, :time 13109721155, :index 43, :value {0 97, 1 0, 2 0, 3 0, 4 0, 5 3, 6 0, 7 0, 8 0, 9 0}}
{:type :invoke, :f :read,     :process 0, :time 13110070211, :index 44}
{:type :ok,     :f :read,     :process 0, :time 13210540811, :index 45, :value {0 97, 1 0, 2 0, 3 0, 4 0, 5 3, 6 0, 7 0, 8 0, 9 0}}
{:type :invoke, :f :read,     :process 0, :time 13210921850, :index 46}
```

### counter

A Jepsen's checker for counter histories. A counter starts at zero; add
operations should increment it by that much, and reads should return the
present value. This checker validates that at each read, the value is greater
than the sum of all `:ok` increments, and lower than the sum of all attempted
increments. Note that this counter verifier assumes the value monotonically
increases and decrements are not allowed.

Example of history:

```clojure
{:type :invoke, :f :add, :value 1, :op-index 1, :process 0, :time 10474104701, :index 0}
{:type :ok,     :f :add, :value 1, :op-index 1, :process 0, :time 10584742951, :index 1}
{:type :invoke, :f :add, :value 1, :op-index 2, :process 0, :time 10686291797, :index 2}
{:type :ok,     :f :add, :value 1, :op-index 2, :process 0, :time 10810489852, :index 3}
{:type :invoke, :f :add, :value 1, :op-index 3, :process 0, :time 10912309790, :index 4}
{:type :ok,     :f :add, :value 1, :op-index 3, :process 0, :time 11040666263, :index 5}
```

### long-fork

A Jepsen's checker for an anomaly in parallel snapshot isolation (but which is
prohibited in normal snapshot isolation). In long-fork, concurrent write
transactions are observed in conflicting order.

### set

A Jepsen's checker for a set histories. Given a set of `:add` operations
followed by a final `:read`, verifies that every successfully added element is
present in the read, and that the read contains only elements for which an add
was attempted.

Example of history:

```clojure
{:type :invoke, :f :add, :value [0 0], :process 0, :time 10529279413, :index 0}
{:type :ok,     :f :add, :value [0 0], :process 0, :time 10661777878, :index 1}
{:type :invoke, :f :add, :value [0 1], :process 0, :time 10761664977, :index 2}
{:type :ok,     :f :add, :value [0 1], :process 0, :time 10888511828, :index 3}
{:type :invoke, :f :add, :value [0 2], :process 0, :time 11077906807, :index 4}
{:type :ok,     :f :add, :value [0 2], :process 0, :time 11209256522, :index 5}
```

### set-full

A Jepsen's checker for a set histories. It is a more rigorous set analysis. We
allow `:add` operations which add a single element, and `:reads` which return
all elements present at that time.

```clojure
{:type :invoke, :f :add, :value [0 0], :process 0, :time 10529279413, :index 0}
{:type :ok,     :f :add, :value [0 0], :process 0, :time 10661777878, :index 1}
{:type :invoke, :f :add, :value [0 1], :process 0, :time 10761664977, :index 2}
{:type :ok,     :f :add, :value [0 1], :process 0, :time 10888511828, :index 3}
{:type :invoke, :f :add, :value [0 2], :process 0, :time 11077906807, :index 4}
{:type :ok,     :f :add, :value [0 2], :process 0, :time 11209256522, :index 5}
{:type :invoke, :f :add, :value [0 3], :process 0, :time 11330024782, :index 6}
{:type :ok,     :f :add, :value [0 3], :process 0, :time 11457989603, :index 7}
{:type :invoke, :f :add, :value [0 4], :process 0, :time 11620593669, :index 8}
{:type :ok,     :f :add, :value [0 4], :process 0, :time 11745589449, :index 9}
{:type :invoke, :f :add, :value [0 5], :process 0, :time 11786251931, :index 10}
```

### cas-register

A Knossos checker for CAS (Compare-And-Set) registers. By default
`competition/analysis` algorithm is used.

Example of history:

```clojure
{:process 7, :type :invoke, :f :cas,   :value [2 3]}
{:process 7, :type :fail,   :f :cas,   :value [2 3]}
{:process 8, :type :invoke, :f :write, :value 2}
{:process 8, :type :ok,     :f :write, :value 2}
{:process 1, :type :invoke, :f :read,  :value nil}
{:process 1, :type :ok,     :f :read,  :value 2}
{:process 4, :type :invoke, :f :read,  :value nil}
{:process 4, :type :ok,     :f :read,  :value 2}
```

### mutex

A Knossos checker for a mutex histories. Applicable to a test with single mutex
responding to `:acquire` and `:release` messages. By default
`competition/analysis` algorithm is used.

Example of history:

```clojure
{:type :invoke, :f :release, :process 1, :time 341187643, :index 0}
{:type :fail,   :f :release, :process 1, :time 342667129, :error :not-held, :index 1}
{:type :invoke, :f :acquire, :process 3, :time 371408519, :index 2}
{:type :invoke, :f :release, :process 4, :time 584312016, :index 3}
{:type :fail,   :f :release, :process 4, :time 585400396, :error :not-held, :index 4}
{:type :invoke, :f :release, :process 0, :time 584353142, :index 5}
{:type :fail,   :f :release, :process 0, :time 585436373, :error :not-held, :index 6}
{:type :invoke, :f :release, :process 1, :time 584300961, :index 7}
{:type :fail,   :f :release, :process 1, :time 585478186, :error :not-held, :index 8}
{:type :invoke, :f :acquire, :process 2, :time 584335820, :index 9}
{:type :invoke, :f :release, :process 0, :time 679093895, :index 10}
```

## License

Copyright © 2021-2022 Sergey Bronnikov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
