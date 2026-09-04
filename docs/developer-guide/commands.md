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

Documentation is generated in from 2 sources:

1. Markdown files in `docs/` are used to generate static HTML using [MkDocs](https://www.mkdocs.org/).
2. Kotlin API documentation is generated using [Dokka](https://kotlinlang.org/docs/dokka-introduction.html).

A static site can be generated from both sources (a Python installation is required).
To build the site:

```sh
# Create a Python environment (optional)
python3 -m venv .venv
source .venv/bin/activate

# Install site dependencies
python3 -m pip install -r docs/requirements.txt

./scripts/build_docs.sh
```

The generated site is written to `site/`. Serve it locally to view both the
project documentation and API reference:

```sh
./scripts/serve_docs.sh
```

Then open <http://localhost:8000>. The API reference is available at
<http://localhost:8000/api/>.

GitHub Actions uses the same script before publishing the site to GitHub Pages.

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
