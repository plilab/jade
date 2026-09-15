# Commands

## Recommended CLI tools

- [`zstd`](https://facebook.github.io/zstd/): compression / decompression (`gzip` is more widely supported and produces similarly sized size, but `zstd` much faster to compress and (especially) decompress)
- [`jq`](https://jqlang.org/): processing JSON

## Linting

Check for linter warnings:

```sh
./gradlew detekt diktatCheck ktlintCheck
```

Fix linter warnings:

```sh
./gradlew diktatFix
./gradlew ktlintFormat
```

## Building and Deploying the Documentation Site

Documentation is generated from two sources:

1. Markdown files in `docs/` are used to generate static HTML using [Hugo](https://gohugo.io/).
2. Kotlin API documentation is generated using [Dokka](https://kotlinlang.org/docs/dokka-introduction.html).

A pinned Hugo binary is downloaded and cached by Gradle, so no system-wide Hugo
or Python installation is required. To build the complete site:

```sh
./gradlew hugoBuild
```

The generated site is written to `site/`. To preview the project documentation
and API reference with live reload:

```sh
./gradlew hugoServer
```

Then open <http://localhost:1313>. The API reference is available at
<http://localhost:1313/api/>.

GitHub Actions uses the same Gradle task before publishing the site to GitHub Pages.

## Gradle-related Commands

Check the current version with:

```sh
./gradlew -version
```

Update by running the following **TWICE**.
See <https://docs.gradle.org/8.11/userguide/gradle_wrapper.html#sec:upgrading_wrapper>.

```sh
./gradlew wrapper --gradle-version latest
```
