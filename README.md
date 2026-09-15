# Jade: The Java Decompiler

[![Java CI with Gradle](https://github.com/adamsmd/jade/actions/workflows/gradle-wrapper-validation.yml/badge.svg)](https://github.com/adamsmd/jade/actions/workflows/gradle-wrapper-validation.yml)
[![Build](https://github.com/adamsmd/jade/actions/workflows/build.yml/badge.svg)](https://github.com/adamsmd/jade/actions/workflows/build.yml)

Jade is a Java decompiler that aims for high reliability through extensive testing.

## Requirements

- Install [Java](https://www.oracle.com/java/technologies/downloads/)
- Install [Gradle](https://docs.gradle.org/current/userguide/installation.html)

Jade uses [Gradle 8.11.1](gradle/wrapper/gradle-wrapper.properties), so you will not be able to run `./gradlew` if your Java version is >= 24.
You can check if your Java version is compatible [here](https://docs.gradle.org/current/userguide/compatibility.html).
If you machine has multiple Java installations, consider using [`jenv`](https://github.com/jenv/jenv) to manage the environment.

Building the tool automatically downloads the other parts that are needed.

## Getting Started

To build the project, simply run the following:

```sh
./gradlew build
```

To run Jade, use `./gradlew run`.
For example, to check the available commands:

```sh
./gradlew run --args=--help
```

To create an installable distribution:

```shell
./gradlew installDist
./build/install/jade/bin/jade --help
```

To download JARs from Maven:

```sh
./build/install/jade/bin/jade download-maven \
  $(realpath ../jade2-maven-data/index/index) \
  $(realpath ../jade2-maven-data/local-repo/) \
  $(realpath ../jade2-maven-data/jar-lists/)
```

## Project Layout

```
.
├── buildSrc/ (custom Gradle plugins used by the build)
├── completions/ (generated shell completion scripts)
├── config/ (linter settings)
├── docs/ (markdown files for MkDocs)
├── dokka/ (metadata for Dokka)
├── gradle/ (self-explanatory)
├── playground/ (experimentation and visualization)
├── scripts/ (utils)
└── src/
    ├── main/kotlin/org/ucombinator/jade/ (Jade's implementation)
    └── test/
        ├── kotlin/org/ucombinator/jade/ (unit tests)
        └── resources/ (Java and class-file test inputs)
```

## Documentation

Dcoumentation is generated using MkDocs and Dokka, and hosted on GitHub Pages.
See https://plilab.github.io/jade/.
