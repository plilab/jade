#!/bin/sh

rm -rf **/.gradle **/build &&
./gradlew build installDist check diktatCheck installDist dokkaHtml &&
./jade about generate-completion bash >completions/jade.bash &&
./jade about generate-completion zsh >completions/jade.zsh &&
./jade about generate-completion fish >completions/jade.fish
