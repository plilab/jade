# Jade Usage

## Classfile Commands

TODO: should these be in own subcommand

### Decompile

TODO

### Compile

TODO

### Diff

TODO

## `maven` Commands

### Used Commands

- `jade` (obviously)
- `bash` (or similar shell)
- `cat` (not generally needed but makes shell commands easier to format)
- `zstd` (`gzip` is more widely supported and produces similarly sized size, but `zstd` much faster to compress and (especially) decompress)
- `jq` (for processing JSON)

### TODO: not implemented:

- List repositories
  - Download index from central
  - Download poms from central
  - Search poms for repositories
  - Download poms again?

### `mirrors`: TODO

TODO

### `index`: Download Index

```console
$ mkdir ../repo/index

$ time ./jade maven index https://maven-central.storage-download.googleapis.com/maven2/ ../repo/index
INFO  maven.Index: downloading nexus-maven-repository-index.properties from https://maven-central.storage-download.googleapis.com/maven2/.index/ to ../repo/index3
INFO  maven.Index: downloaded nexus-maven-repository-index.properties from https://maven-central.storage-download.googleapis.com/maven2/.index/ to ../repo/index3 containing 1130 bytes
INFO  maven.Index: downloading nexus-maven-repository-index.gz from https://maven-central.storage-download.googleapis.com/maven2/.index/ to ../repo/index3
INFO  maven.Index: downloaded nexus-maven-repository-index.gz from https://maven-central.storage-download.googleapis.com/maven2/.index/ to ../repo/index3 containing 2458624054 bytes

real	4m0.168s
user	0m6.485s
sys	0m8.849s
```

Alternately:

```console
$ time ./jade maven index https://repo1.maven.org/maven2/ ../repo/index
INFO  maven.Index: downloading nexus-maven-repository-index.properties from https://repo1.maven.org/maven2/.index/ to ../repo/index2
INFO  maven.Index: downloaded nexus-maven-repository-index.properties from https://repo1.maven.org/maven2/.index/ to ../repo/index2 containing 1130 bytes
INFO  maven.Index: downloading nexus-maven-repository-index.gz from https://repo1.maven.org/maven2/.index/ to ../repo/index2
INFO  maven.Index: downloaded nexus-maven-repository-index.gz from https://repo1.maven.org/maven2/.index/ to ../repo/index2 containing 2458624054 bytes

real	33m4.825s
user	0m22.434s
sys	0m20.230s
```

### `index-to-json`: Convert Index to JSON

```console
$ time ./jade maven index-to-json ../repo/index | zstd --stdout >../repo/index/nexus-maven-repository-index.jsonl.zst

real	14m41.136s
user	15m53.276s
sys	3m44.319s
```

### List Artifacts

```console
$ time zstd --stdout --decompress <../repo/index/nexus-maven-repository-index.jsonl.zst |
jq --compact-output 'select(.kind == "EXPANDED_RECORD" and .type == "ARTIFACT_ADD")' | # Select only ADD records
jq --compact-output 'select(has("classifier") | not)' | # Select only non-javadoc, non-source, etc.
jq --compact-output 'select("\(.groupId)\(.artifactId)" | (contains(":") or contains(" ")) | not)' | # Remove bad artifact names
jq --raw-output '"\(.groupId):\(.artifactId)"' | # Output artifact name
jq --raw-input '.' | jq --slurp --raw-output 'unique[]' >../repo/artifact-ids.txt # Keep only unique ids

real	16m42.805s
user	34m24.959s
sys	1m10.576s

$ wc -lc ../repo/artifact-ids.txt
  624415 25110145 ../repo/artifact-ids.txt
```

#### Bad Artifacts

Some artifacts have a groupId or artifactId that contian ':' or ' ', which is not allowed per DefaultArtifact.COORDINATE_PATTERN.
See https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.20/maven-resolver-api/src/main/java/org/eclipse/aether/artifact/DefaultArtifact.java#L76-L80

```console
$ zstd --stdout --decompress <../repo/index/nexus-maven-repository-index.jsonl.zst |
jq --compact-output 'select(.kind == "EXPANDED_RECORD" and .type == "ARTIFACT_ADD")' | # Select only ADD records
jq --compact-output 'select(has("classifier") | not)' | # Select only non-javadoc, non-source, etc.
jq --compact-output 'select("\(.groupId)\(.artifactId)" | (contains(":") or contains(" ")))' | # Select bad artifact names
jq --raw-output '"|\(.groupId)|\(.artifactId)|"' | # Output artifact name with alternate seperator
jq --raw-input '.' | jq --slurp --raw-output 'unique[]' # Keep only unique ids

|app.ubie|app.ubie:brave-kt|
|com.foilen|com.foilen:database-tools|
|com.github.mjdev|libaums:http|
|com.github.mjdev|libaums:storageprovider|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 2.inkapplications.spondee|math-mingwx64|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 2.inkapplications.spondee|measures-mingwx64|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|math-js|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|math-jvm|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|math|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|measures-js|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|measures-jvm|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 3.inkapplications.spondee|measures|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|math-linuxarm32hfp|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|math-linuxmips32|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|math-linuxx64|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|measures-linuxarm32hfp|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|measures-linuxmips32|
|com.inkapplications.spondee.math-macosx64.0.0.3.com 4.inkapplications.spondee|measures-linuxx64|
|com.tachen.android|utilex |
|org.webjars.npm|reactivex:rxjs|

real	15m28.474s
user	27m17.804s
sys	0m57.036s
```

### `versions`: Select Versions

```console
$ time ./jade maven versions --shuffle ../repo/local-repo ../repo/versions @../repo/artifact-ids.txt

TODO ...
...
👷jobs 624_415 (cache 612_662/wait 0/run 0/done 11_753)✅pass 611_376 (cache 610_067/new 1_309)❌fail 13_039 (cache 2_595/new 0/glitch 10_444) ✅normal exit

Currently running (seconds):
  <none>

Fails (Cache):
  2593 	org.eclipse.aether.transfer.MetadataTransferException:org.eclipse.aether.transfer.ChecksumFailureException
  2 	org.ucombinator.jade.maven.NoVersioningTagException

Fails (New):
  <none>

Fails (Glitch):
  10444 	org.eclipse.aether.transfer.MetadataNotFoundException
...

$ time find ../repo/versions -type f -name \*.version -exec cat {} + >../repo/versions.txt

real	0m36.838s
user	0m0.883s
sys	0m15.095s

$ time wc -lc ../repo/versions.txt
  610065 29066873 ../repo/versions.txt

real	0m0.016s
user	0m0.005s
sys	0m0.012s

$ find ../repo/versions -name \*.version.err

TODO ...
```

### `dependencies`: List Dependencies

```console
$ time ./jade maven dependencies --shuffle ../repo/local-repo ../repo/dependencies @../repo/versions.txt

TODO ...
...
👷jobs 610_065 (cache 607_147/wait 0/run 6/done 2_912)✅pass 522_367 (cache 522_355/new 12)❌fail 87_692 (cache 84_792/new 233/glitch 2_667) 🛑JVM shutdown

...

Fails (Cache):
  58198 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
  10698 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.building.ModelBuildingException
  6050 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
  2835 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.VersionRangeResolutionException
  2809 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.client.HttpResponseException
  2094 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.UnknownHostException
  1013 	org.ucombinator.jade.maven.DollarInCoordinateException
  642 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.collection.UnsolvableVersionConflictException
  363 	org.ucombinator.jade.maven.CaretInVersionException
  90 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.ChecksumFailureException
```

#### Single file with indent

```console
$ zstd --stdout --decompress ../repo/dependencies/abbot/abbot/1.4.0/abbot-1.4.0.jar.dependencies.json.zst |
jq --compact-output 'def with_depth(i): [i, del(.children)], (.children[] | with_depth(i + 1)) ; with_depth(0)' |
jq --raw-output '("    " * .[0]) + (.[1].artifact | [.groupId, .artifactId, .extension, .classifier, .version] | map(select(. != "")) | join(":"))'

abbot:abbot:jar:1.4.0
    junit:junit:jar:4.8.2
```

#### All together

```console
$ time find ../repo/dependencies -name \*.dependencies.json.zst -exec zstd --stdout --decompress {} + |
jq --compact-output 'def with_depth(i): [i, del(.children)], (.children[] | with_depth(i + 1)) ; with_depth(0)' |
jq --raw-output '.[1].artifact | [.groupId, .artifactId, .extension, .classifier, .version] | map(select(. != "")) | join(":")' |
jq --raw-input '.' | jq --slurp --raw-output 'unique[]' |
cat >../repo/dependencies.txt

real	17m22.780s
user	28m4.016s
sys	1m9.788s
```

TODO: `jq --raw-output 'include jq/coord.jq; .[1].artifact | coord' |`

TODO: Packaging vs fileExtension:

```json
{"kind":"EXPANDED_RECORD","type":"ARTIFACT_ADD","version":"2.0.5","groupId":"xdoclet","packaging":"maven-plugin","hasJavadoc":false,"sha1":"3207930de044f75d5bbed9c80521fd56119e3490","recordModified":1318434041457,"fileSize":12948,"fileExtension":"jar","name":"Maven2 XDoclet2 Plugin","artifactId":"maven2-xdoclet2-plugin","hasSources":false,"hasSignature":false,"fileModified":1216330098000}
```

### Artifact Sizes

```shell
$ time zstd --stdout --decompress <../repo/index/nexus-maven-repository-index.jsonl.zst |
jq --compact-output 'select(.kind == "EXPANDED_RECORD" and .type == "ARTIFACT_ADD")' | # Select only ADD records
jq --compact-output --slurpfile d <(
  jq --raw-input '.' ../repo/dependencies.txt | jq --slurp 'INDEX(.)'
) 'select($d[0]["\(.groupId):\(.artifactId):\(.fileExtension // ""):\(.classifier // ""):\(.version)"]) | .fileSize' |
jq --slurp 'add'

96_209_590_077

real	14m19.441s
user	23m9.543s
sys	0m44.759s
```

96_209_590_077 (TODO: is this right?)

385_848_748_952
00:18:30.39

### Download Dependencies

TODO: filenames as classpath

TODO: artifact-ids.txt and artifact-jsons/

```console
$ time ./jade maven download --shuffle ../repo/local-repo ../repo/artifacts @../repo/dependencies.txt
```

```shell
jq <../repo/dependencies/${file} '../.children | .artifact | coord' >../repo/dependencies
jq <../repo/dependencies/${file} '../.children | .artifact | .file' >../repo/dependencies
```

### Class Path

```shell
tail +1 ${dependency-file} | perl -pe 's/\n/:/'
```

```shell
zstd --stdout --decompress ../repo/dependencies/.../foo.json.zstd |
jq --compact-output 'def with_depth(i): [i, del(.children)], (.children[] | with_depth(i + 1)) ; with_depth(0)' |
cat >../repo/depdencies/${file}
```

```shell
zstd --stdout --decompress ../repo/dependencies/org/apache/flink/flink-avro/1.19.1/flink-avro-1.19.1.jar.dependencies.json.zst |
jq --compact-output 'def with_depth(i): [i, del(.children)], (.children[] | with_depth(i + 1)) ; with_depth(0)' |
jq --compact-output '.[1].artifact | "\(.groupId | gsub("\\."; "/"))/\(.artifactId)/\(.version)/\(.artifactId)-\(.version)\(.classifier | if . != "" then "-" + . end).\(.fileExtension // "jar")"'

```

## `about` Commands

### `build-info`: Show information about how `jade` was built

```console
$ ./jade about build-info

jade version 0.0.0-122-gd41be7b-20240722T202551+0800 (https://github.org/ucombinator/jade)
Build tools: Kotlin 1.9.22, Gradle 8.7, Java 19.0.2
Build time: 2024-07-22T20:25:51.888+08:00
Dependencies:
  ch.qos.logback:logback-classic:1.5.6 (configuration: default)
  com.github.ajalt.clikt:clikt:4.4.0 (configuration: default)
  com.github.javaparser:javaparser-core-serialization:3.25.10 (configuration: default)
  com.github.javaparser:javaparser-core:3.25.10 (configuration: default)
  com.github.javaparser:javaparser-symbol-solver-core:3.25.10 (configuration: default)
  com.github.luben:zstd-jni:1.5.6-3 (configuration: default)
  com.pinterest.ktlint:ktlint-cli-reporter-baseline:1.2.1 (configuration: default)
  com.pinterest.ktlint:ktlint-cli:1.2.1 (configuration: default)
  com.pinterest.ktlint:ktlint-ruleset-standard:1.2.1 (configuration: default)
  io.github.detekt.sarif4k:sarif4k:0.5.0 (configuration: default)
  io.github.detekt.sarif4k:sarif4k:0.5.0 (configuration: default)
  io.github.microutils:kotlin-logging-jvm:3.0.5 (configuration: default)
  io.github.oshai:kotlin-logging:5.1.0 (configuration: default)
  io.github.oshai:kotlin-logging:5.1.0 (configuration: default)
  io.github.oshai:kotlin-logging:5.1.0 (configuration: default)
  io.gitlab.arturbosch.detekt:detekt-cli:1.23.6 (configuration: default)
  io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.6 (configuration: default)
  io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.6 (configuration: default)
  org.apache.commons:commons-compress:1.26.1 (configuration: default)
  org.apache.maven.indexer:indexer-core:7.1.3 (configuration: default)
  org.apache.maven.indexer:indexer-reader:7.1.3 (configuration: default)
  org.apache.maven.resolver:maven-resolver-api:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-connector-basic:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-impl:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-spi:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-supplier:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-transport-file:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-transport-http:1.9.20 (configuration: default)
  org.apache.maven.resolver:maven-resolver-util:1.9.20 (configuration: default)
  org.apache.maven:maven-resolver-provider:3.9.6 (configuration: default)
  org.jacoco:org.jacoco.agent:0.8.11 (configuration: default)
  org.jetbrains.dokka:analysis-kotlin-descriptors:1.9.20 (configuration: default)
  org.jetbrains.dokka:analysis-kotlin-descriptors:1.9.20 (configuration: default)
  org.jetbrains.dokka:analysis-kotlin-descriptors:1.9.20 (configuration: default)
  org.jetbrains.dokka:analysis-kotlin-descriptors:1.9.20 (configuration: default)
  org.jetbrains.dokka:dokka-base:1.9.20 (configuration: default)
  org.jetbrains.dokka:dokka-base:1.9.20 (configuration: default)
  org.jetbrains.dokka:dokka-base:1.9.20 (configuration: default)
  org.jetbrains.dokka:dokka-base:1.9.20 (configuration: default)
  org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22 (configuration: default)
  org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.22 (configuration: default)
  org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.22 (configuration: default)
  org.jetbrains.kotlin:kotlin-stdlib:1.9.22 (configuration: default)
  org.jetbrains.kotlin:kotlin-test:1.9.22 (configuration: default)
  org.jetbrains.kotlin:kotlin-test:1.9.22 (configuration: default)
  org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1 (configuration: default)
  org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0 (configuration: default)
  org.jetbrains.kotlinx:kover-jvm-agent:0.8.0 (configuration: default)
  org.jetbrains.kotlinx:kover-jvm-agent:0.8.0 (configuration: default)
  org.jgrapht:jgrapht-core:1.5.2 (configuration: default)
  org.jgrapht:jgrapht-ext:1.5.2 (configuration: default)
  org.jgrapht:jgrapht-io:1.5.2 (configuration: default)
  org.jgrapht:jgrapht-opt:1.5.2 (configuration: default)
  org.junit.jupiter:junit-jupiter-params:5.6.3 (configuration: default)
  org.junit.platform:junit-platform-launcher:1.10.2 (configuration: default)
  org.ow2.asm:asm-analysis:9.7 (configuration: default)
  org.ow2.asm:asm-commons:9.7 (configuration: default)
  org.ow2.asm:asm-tree:9.7 (configuration: default)
  org.ow2.asm:asm-util:9.7 (configuration: default)
  org.ow2.asm:asm:9.7 (configuration: default)
Compile-time system properties:
  java.class.path=/home/adamsmd/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/lib/gradle-launcher-8.7.jar
  java.class.version=63.0
  java.home=/nix/store/r12q2z73ap6g09x815shkvsq3hsi740d-openjdk-19.0.2+7/lib/openjdk
  java.io.tmpdir=/tmp
  java.library.path=/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib
  java.runtime.name=OpenJDK Runtime Environment
  java.runtime.version=19.0.2+7-nixos
  java.specification.name=Java Platform API Specification
  java.specification.vendor=Oracle Corporation
  java.specification.version=19
  java.vendor=N/A
  java.vendor.url=https://openjdk.java.net/
  java.vendor.url.bug=https://bugreport.java.com/bugreport/
  java.version=19.0.2
  java.version.date=2023-01-17
  java.vm.compressedOopsMode=32-bit
  java.vm.info=mixed mode, sharing
  java.vm.name=OpenJDK 64-Bit Server VM
  java.vm.specification.name=Java Virtual Machine Specification
  java.vm.specification.vendor=Oracle Corporation
  java.vm.specification.version=19
  java.vm.vendor=Oracle Corporation
  java.vm.version=19.0.2+7-nixos
  os.arch=amd64
  os.name=Linux
  os.version=6.1.69
Runtime system properties:
  java.class.path=/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jade-0.0.0-122-gd41be7b-20240722T202551+0800.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/clikt-jvm.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlin-logging-jvm-3.0.5.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlin-stdlib-jdk8-1.8.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlinx-coroutines-core-jvm-1.8.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlinx-serialization-core-jvm-1.7.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlinx-serialization-json-jvm-1.7.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/mordant-jvm.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlin-stdlib-jdk7-1.8.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/colormath-jvm.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/markdown-jvm-0.7.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/kotlin-stdlib-2.0.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/logback-classic-1.5.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/javaparser-core-serialization-3.25.10.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/javaparser-symbol-solver-core-3.25.10.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/javaparser-core-3.25.10.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/commons-compress-1.26.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/zstd-jni-1.5.6-3.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jgrapht-ext-1.5.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jgrapht-io-1.5.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jgrapht-opt-1.5.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jgrapht-core-1.5.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/asm-commons-9.7.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/asm-util-9.7.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/asm-analysis-9.7.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/asm-tree-9.7.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/asm-9.7.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/indexer-core-7.1.3.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/indexer-reader-7.1.3.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-supplier-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-connector-basic-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-provider-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-impl-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-transport-file-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-transport-http-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-spi-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-util-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-api-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/annotations-23.0.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/logback-core-1.5.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-resolver-named-locks-1.9.20.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jcl-over-slf4j-1.7.36.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/slf4j-api-2.0.13.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jakarta.json-api-2.1.3.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/javassist-3.30.2-GA.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/guava-33.1.0-jre.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/commons-codec-1.16.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/commons-io-2.15.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/commons-text-1.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-model-builder-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-artifact-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/commons-lang3-3.14.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jheaps-0.14.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/apfloat-1.10.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jgraphx-4.2.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/antlr4-runtime-4.12.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/fastutil-8.5.12.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/javax.inject-1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-queryparser-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-analysis-common-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-backward-codecs-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-highlighter-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-sandbox-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-queries-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-memory-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/lucene-core-9.10.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-model-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/httpclient-4.5.14.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/httpcore-4.4.16.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-repository-metadata-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/plexus-utils-3.5.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/failureaccess-1.0.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jsr305-3.0.2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/checker-qual-3.42.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/error_prone_annotations-2.26.1.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/plexus-interpolation-1.26.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/maven-builder-support-3.9.6.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/org.eclipse.sisu.inject-0.9.0.M2.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/jna-5.14.0.jar:/home/adamsmd/r/utah/jade/jade2/build/install/jade/lib/fastutil-core-8.5.12.jar
  java.class.version=63.0
  java.home=/nix/store/r12q2z73ap6g09x815shkvsq3hsi740d-openjdk-19.0.2+7/lib/openjdk
  java.io.tmpdir=/tmp
  java.library.path=/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib
  java.runtime.name=OpenJDK Runtime Environment
  java.runtime.version=19.0.2+7-nixos
  java.specification.name=Java Platform API Specification
  java.specification.vendor=Oracle Corporation
  java.specification.version=19
  java.vendor=N/A
  java.vendor.url=https://openjdk.java.net/
  java.vendor.url.bug=https://bugreport.java.com/bugreport/
  java.version=19.0.2
  java.version.date=2023-01-17
  java.vm.compressedOopsMode=Zero based
  java.vm.info=mixed mode, sharing
  java.vm.name=OpenJDK 64-Bit Server VM
  java.vm.specification.name=Java Virtual Machine Specification
  java.vm.specification.vendor=Oracle Corporation
  java.vm.specification.version=19
  java.vm.vendor=Oracle Corporation
  java.vm.version=19.0.2+7-nixos
  os.arch=amd64
  os.name=Linux
  os.version=6.1.69
```

### `loggers`: List available loggers

#### List Loggers

```console
$ ./jade about loggers

ROOT
org
org.eclipse
org.eclipse.aether
org.eclipse.aether.internal
org.eclipse.aether.internal.impl
org.eclipse.aether.internal.impl.DefaultArtifactResolver
org.eclipse.aether.internal.impl.DefaultInstaller
org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider
org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager
org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider
org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher
org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider
org.eclipse.aether.internal.impl.DefaultTrackingFileManager
org.eclipse.aether.internal.impl.DefaultTransporterProvider
org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer
org.eclipse.aether.internal.impl.checksum
org.eclipse.aether.internal.impl.checksum.SparseDirectoryTrustedChecksumsSource
org.eclipse.aether.internal.impl.checksum.SummaryFileTrustedChecksumsSource
org.eclipse.aether.internal.impl.collect
org.eclipse.aether.internal.impl.collect.bf
org.eclipse.aether.internal.impl.collect.bf.BfDependencyCollector
org.eclipse.aether.internal.impl.collect.df
org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector
org.eclipse.aether.internal.impl.filter
org.eclipse.aether.internal.impl.filter.GroupIdRemoteRepositoryFilterSource
org.eclipse.aether.internal.impl.filter.PrefixesRemoteRepositoryFilterSource
org.eclipse.aether.internal.impl.synccontext
org.eclipse.aether.internal.impl.synccontext.named
org.eclipse.aether.internal.impl.synccontext.named.DiscriminatingNameMapper
org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactoryImpl
org.eclipse.aether.named
org.eclipse.aether.named.providers
org.eclipse.aether.named.providers.FileLockNamedLockFactory
org.eclipse.aether.named.providers.LocalReadWriteLockNamedLockFactory
org.eclipse.aether.named.providers.LocalSemaphoreNamedLockFactory
org.eclipse.aether.named.providers.NoopNamedLockFactory
org.ucombinator
org.ucombinator.jade
org.ucombinator.jade.maven
org.ucombinator.jade.maven.JadeMetadataResolver
org.ucombinator.jade.maven.Maven
org.ucombinator.jade.util
org.ucombinator.jade.util.Log
```

#### Test Loggers

```console
$ ./jade about loggers --test

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

org.eclipse.aether
ERROR .org.eclipse.aether: error in Logger[org.eclipse.aether]
WARN  .org.eclipse.aether: warn in Logger[org.eclipse.aether]
INFO  .org.eclipse.aether: info in Logger[org.eclipse.aether]

...
```

### `generate-completion`: Generate a tab-complete script for the given shell

#### Bash

```console
$ ./jade about generate-completion bash >completions/jade.bash
$ source completions/jade.bash
```

#### Zsh

```console
$ ./jade about generate-completion zsh >completions/jade.zsh
$ source completions/jade.zsh
```

#### Fish

```console
$ ./jade about generate-completion fish >completions/jade.fish
$ source completions/jade.fish
```
