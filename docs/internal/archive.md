# Archive

!!! warning
    This archive contains content that I don't know how to migrate.
    A lot of it is broken / seems outdated.

## `jade maven` documentation

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

TODO: `jq --raw-output 'include src/main/jq/coord.jq; .[1].artifact | coord' |`

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

$ ./jade download-maven --shuffle ~/a/local/jade2/index/index ~/a/local/jade2/local-repo ~/a/local/jade2/dependency-lists ~/a/local/jade2/jar-lists
