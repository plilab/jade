# Jade: The Java Decompiler

Jade is a Java decompiler that aims for high reliability through extensive testing.

## Requirements

The only requirement is to have a copy of the [Java
JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
installed so that `java` can be run.

Building the tool automatically downloads the other parts that are needed.

## Building

To compile the project for use, simply run the following:

```shell
./gradlew build
```

## Running Jade from command line
TODO

## Generating HTML KDoc with Dokka

To generate KDoc, run the following:

```shell
./gradlew dokkaHtml
```

The generated html can be found in `/build/dokka/html`. To view the documentation, you can open `index.html` with your browser.

## Developer Guide

For more details on Jade's codebase, refer to [Developer Guide](/docs/Developer Guide.md)