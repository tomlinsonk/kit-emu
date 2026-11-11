JAVAC ?= javac
JAVA ?= java

.PHONY: all build run test clean

all: build

build:
	mkdir -p build/classes
	$(JAVAC) -d build/classes $(wildcard *.java)

run: build
	$(JAVA) -cp build/classes KiT $(ARGS)

test: build
	mkdir -p build/test-classes
	$(JAVAC) -cp build/classes -d build/test-classes test/CPUTest.java
	$(JAVA) -cp build/classes:build/test-classes CPUTest

clean:
	rm -rf build test/build
	find . test -maxdepth 1 -type f -name '*.class' -delete
