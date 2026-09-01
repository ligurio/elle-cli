LEIN ?= lein

.PHONY: all build check coverage deps test clean

# Dependency namespaces to instrument on top of the project's own
# source, so that coverage for Elle, Knossos, and Jepsen is
# reported too.
COVERAGE_NS := elle.bfs
COVERAGE_NS += elle.closed-predicate
COVERAGE_NS += elle.consistency-model
COVERAGE_NS += elle.core
COVERAGE_NS += elle.graph
COVERAGE_NS += elle.list-append
COVERAGE_NS += elle.rels
COVERAGE_NS += elle.rw-register
COVERAGE_NS += elle.txn
COVERAGE_NS += elle.util elle.viz
COVERAGE_NS += jepsen.checker
COVERAGE_NS += jepsen.history
COVERAGE_NS += jepsen.history.core
COVERAGE_NS += jepsen.history.fold
COVERAGE_NS += jepsen.history.task
COVERAGE_NS += jepsen.independent
COVERAGE_NS += jepsen.tests.long-fork
COVERAGE_NS += knossos.analysis
COVERAGE_NS += knossos.competition
COVERAGE_NS += knossos.core
COVERAGE_NS += knossos.history
COVERAGE_NS += knossos.model
COVERAGE_NS += knossos.op

all: build

deps:
	$(LEIN) deps

build:
	$(LEIN) deps
	$(LEIN) uberjar

check:
	$(LEIN) check
	$(LEIN) compile

test:
	$(LEIN) test

coverage:
	$(LEIN) cloverage --html $(COVERAGE_NS)
	@echo "Coverage report: target/coverage/index.html"

clean:
	$(LEIN) clean
