# Notes

https://maven.apache.org/resolver-archives/resolver-1.9.0/maven-resolver-demos/maven-resolver-demo-snippets/xref/org/apache/maven/resolver/examples/util/ConsoleDependencyGraphDumper.html

## TODO

- Use internal compiler to generate class file for tests
- Rename packages away from ucombinator?
- Github Action to build and report warnings
- Could we use JavaParser's type inferences to solve generics
- `$ ./gradlew -p buildSrc --warning-mode all dependencyUpdates`
- Require KDoc on single line functions (some allow not)
- Make ./jade be a bash script? (jade.sh, jade.bat) (use /usr/bin/env bash) (symlink to .bat)
- GitHub Packages
- Disable GitHub Discussions and so on
- GitHub Action for License Report
- GitHub Action for Updates
- Use JavaDoc for Java source tests
- JavaParser Tests
- CodeQL

// TODO: search to "TODO" typos


compiler
jar
test

For tests use code from:
  https://github.com/javaparser/javaparser/blob/master/javaparser-core-testing/src/test/resources/com/github/javaparser/Sample.java

look for throw that could be require or check

Put link to standard in Flags.txt and "becomes Flags.kt in build"

VSCode: Auto Indent on Move

val currLine = Throwable().stackTrace[0].lineNumber

### Other


## How to update ...

### Gradle

Check the current version with:

```bash
$ ./gradlew -version
```

Update by running the following **TWICE**.  See
<https://docs.gradle.org/8.11/userguide/gradle_wrapper.html#sec:upgrading_wrapper>.

```bash
$ ./gradlew wrapper --gradle-version latest
```

### Plugins

https://plugins.gradle.org/


## Bugs

### Clikt

Automatic secondary flag names (i.e., "--foo" adds "--no-foo")

Support "@-" for @argfile
