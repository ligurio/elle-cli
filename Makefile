LEIN ?= lein

.PHONY: all build check test clean

all: build

build:
	$(LEIN) deps
	$(LEIN) uberjar

check:
	$(LEIN) check
	$(LEIN) compile

test:
	sh ./test.sh

clean:
	$(LEIN) clean
