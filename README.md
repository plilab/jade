# Jade: The Java Decompiler

[![Java CI with Gradle](https://github.com/adamsmd/jade/actions/workflows/gradle.yml/badge.svg)](https://github.com/adamsmd/jade/actions/workflows/gradle.yml)

Jade is a Java decompiler that aims for high reliability through extensive testing.

## Requirements

The only requirement is to have a copy of the [Java
JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
installed so that `java` can be run.

TODO: it is tested on Java version 19

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

TODO: explain the following

```shell
./gradlew build installDist

./build/install/jade/bin/jade

./gradlew run --args=--help

./build/install/jade/bin/jade download-maven $(realpath ../jade2-maven-data/index/index) $(realpath ../jade2-maven-data/local-repo/) $(realpath ../jade2-maven-data/jar-lists/)
```

## Developer Guide

For more details on Jade's codebase and working on Jade, refer to Developer Guide at [docs/DeveloperGuide.md](docs/DeveloperGuide.md)

## Documentation

TODO: See docs/USAGE.md
See docs/MAVEN.md

## Source Organization

- completions:
  This directory contains the shell autocompletions that are generated for Jade.

  See the `generate-completion` section of docs/USAGE.md for how to generate and use these files.

- jq
- src

TODO: remove [*](*) for plain URLs

> [!NOTE]
> TODO

> [!TIP]
> TODO

======================

## What
What is this and what is it for? Put a meaningful, short, plain-language description of what this project is trying to accomplish and why it matters. Describe the problem this project solves.

#### Status
Is it done? Is it a prototype? Is it a zombie? Alpha, Beta, 1.1, etc. It's OK to write a sentence, too. The goal is to let interested people know where this project is at. This is also a good place to link to the [CHANGELOG](https://github.com/cfpb/ckan/blob/master/CHANGELOG.md).

#### Screenshots
![Infinite Windmills](http://i.giphy.com/SIV3ijAwkNt9C.gif)

#### Press
Links to other people talking about your work.

## Why
A more in detail explanation for why this project exists. Tell us all about it.

## Who
Who are you? Who are your partners? Links, logos, and lots of credit giving.

## How
#### Dependencies
Tell us what we need to know before we even begin with your project. Describe any dependencies that must be installed for this software to work. This includes programming languages, databases or other storage mechanisms, build tools, frameworks, and so forth. If specific versions of other software are required, or known not to work, call that out. Links to other [How To's](https://github.com/codeforamerica/howto) will help.

#### Install
Detailed instructions on how to install, configure, and get the project running. This should be frequently tested to ensure reliability.

Very clear instructions with 

`line by line commands`

`to copy and paste`

See [the CfAPI](https://github.com/codeforamerica/cfapi#installation) for an example.

#### Deploy
More required commands and links to tutorials.

#### Testing
If the software includes automated tests, detail how to run those tests.

## Contribute
A short explanation of how others can contribute. Be sure to show how to submit issues and pull requests. Include a [CONTRIBUTING.md file](https://github.com/18F/hub/blob/master/CONTRIBUTING.md). Here is a good [CfA example](https://github.com/codeforamerica/ohana-web-search/blob/master/CONTRIBUTING.md). GitHub also has some new guides on [how to contribute](https://guides.github.com/activities/contributing-to-open-source/#contributing).

## License
A link to the Code for America copyright and [LICENSE.md file](https://github.com/codeforamerica/ceviche-cms/blob/master/LICENCE.md).

## Citing

## Other Documentation

- ./docs/SECURITY.md: Instructions for how to report a security vulnerability
- ./docs/USAGE.md
- ./LICENSE.md
