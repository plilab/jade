# Commands

## Adding Jade to your path

For convenience, consider adding `jade` to `$PATH`:

```sh
# Build Jade if you haven't already done so
./gradlew installDist

export PATH="$PWD/build/install/jade/bin:$PATH"
jade --help
```

The documentation below assumes that you have added `jade` to your `$PATH`.
Otherwise, replace `jade` with `./build/install/jade/bin/jade` instead.

## Commands

`jade` consists of several commands and subcommands:

- `decompile`: Decompile `.class` files
- `compile`: Compile `.java` file`
- `diff`:  Compare `.class` files
- `maven`: Commands for operating with Maven
- `about`: Commands about Jade

### `jade decompile`

Decompile `.class` files into `.java` files:

```sh
jade decompile <source_path>... <output_directory>
```

### `jade compile`

!!! warning
    This is currently broken. Consider using `javac` instead.

Compile `.java` files into `.class` files:

```sh
jade compile <source_path>...
```

### `jade diff`

Compare two `.class` files:

```sh
jade diff <path_1> <path_2>
```

If the outputs are the same, it would print:

```console
Bytecode is the same!
```

Otherwise, the `diff` subcommand should print out the differences between the class files:

```console
Old name ...
New name ...
Difference in: attrs
Difference in: fields
...
```

Currently, it just runs a basic check on all the fields of the `ClassNode`.


### `jade maven`

`jade maven` contains subcommands for interacting with Maven:

- [`mirrors`](#jade-maven-mirrors): Print the mirrors of a Maven repository
- [`index`](#jade-maven-index): Download the index from a remote Maven repository
- [`index-to-json`](#jade-maven-index-to-json): Print a Maven index to stdout as JSON lines
- [`versions`](#jade-maven-versions): Select artifact versions
- [`dependencies`](#jade-maven-dependencies): 
- [`download`](#jade-maven-download): TODO
- [`clear-locks`](#jade-maven-clear-locks): TODO

#### `jade maven mirrors`

Prints the mirrors of a Maven repository.

```sh
jade maven mirrors --remote=<remote_url>
```

By default, `remote` is set to <https://maven-central.storage-download.googleapis.com/maven2/>.

#### `jade maven index`:

Download an index from Maven, which contains a catalog of artifacts from a Maven repository.

```sh
jade maven index --remote=<remote_url> <output_dir>
```

This is stored as `.gz` file: `nexus-maven-repository-index.gz`.

For example:

```sh
mkdir -p ../repo/index
jade maven index ../repo/index

ls -lh ../repo/index
```

```console
total 6352704
-rw-r--r--@ 1 owner  group   3.0G XXX  5 XX:XX nexus-maven-repository-index.gz
-rw-r--r--@ 1 owner  group   1.1K XXX  5 XX:XX nexus-maven-repository-index.properties
```

By default, `remote` is set to <https://maven-central.storage-download.googleapis.com/maven2/>.

#### `jade maven index-to-json`

Converts an index to JSON.

!!! warning
    This seems to be broken.

```sh
jade maven index-to-json ../repo/index | zstd --stdout >../repo/index/nexus-maven-repository-index.jsonl.zst
```

!!! todo
    Migrate docs from [archive](../internal/archive.md).

### `jade maven versions`

!!! todo
    Migrate docs from [archive](../internal/archive.md).

#### `jade maven dependencies`

!!! todo
    Migrate docs from [archive](../internal/archive.md).

#### `jade maven download`

!!! todo
    Migrate docs from [archive](../internal/archive.md).

#### `jade maven clear-locks`

!!! todo
    Migrate docs from [archive](../internal/archive.md).

### `jade about`

`jade about` provides subcommands for debugging:

- [`build-info`](#jade-about-build-info): Show information about how `jade` was built
- [`generate-completion`](#jade-about-generate-completion): Generate a tab-complete script for the given shell
- [`loggers`](#jade-about-loggers): List available loggers

#### `jade about build-info`

Show information about how `jade` was built. This command will give information about the JDK, Libraries, OS and so on:

```sh
jade about build-info
```

```console
jade version 0.0.0-122-gd41be7b-20240722T202551+0800 (https://github.org/ucombinator/jade)
Build tools: Kotlin 1.9.22, Gradle 8.7, Java 19.0.2
Build time: 2024-07-22T20:25:51.888+08:00
Dependencies:
  ch.qos.logback:logback-classic:1.5.6 (configuration: default)
  com.github.ajalt.clikt:clikt:4.4.0 (configuration: default)
  com.github.javaparser:javaparser-core-serialization:3.25.10 (configuration: default)
  ...
Compile-time system properties:
  ...
Runtime system properties:
  ...
```

#### `jade about generate-completion`

Generate a tab-complete script for the given shell.

##### Bash

```sh
jade about generate-completion bash >completions/jade.bash
source completions/jade.bash
```

##### Fish

```sh
jade about generate-completion fish >completions/jade.fish
source completions/jade.fish
```

##### Zsh

```sh
jade about generate-completion zsh >completions/jade.zsh
source completions/jade.zsh
```

#### `jade about loggers`

List available loggers.

```sh
jade about loggers
```

```console
ROOT
org
org.eclipse
org.eclipse.aether
...
```

You can also send a test message to the different loggers:

```sh
jade about loggers --test
```

```console
ROOT
ERROR .ROOT: error in Logger[ROOT]
WARN  .ROOT: warn in Logger[ROOT]
INFO  .ROOT: info in Logger[ROOT]

org
ERROR .org: error in Logger[org]
WARN  .org: warn in Logger[org]
INFO  .org: info in Logger[org]

org.eclipse
ERROR .org.eclipse: error in Logger[org.eclipse]
WARN  .org.eclipse: warn in Logger[org.eclipse]
INFO  .org.eclipse: info in Logger[org.eclipse]

...
```
