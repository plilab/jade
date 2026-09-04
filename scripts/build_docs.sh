#!/bin/sh

mkdir -p "site"
mkdir -p "site/api"

./gradlew dokkaHtml --no-daemon
python3 -m mkdocs build --strict --site-dir "site"

cp -R build/dokka/html/. "site/api/"

printf 'Documentation site built at %s\n' "site"
