# Maven

Useful documentation about Maven and its repository format.

## Maven Documentation: https://maven.apache.org/index.html

- Settings Reference: `<settings>` in `settings.xml`: https://maven.apache.org/settings.html
- POM Reference: `pom.xml`: https://maven.apache.org/pom.html
- Maven Repositories: https://maven.apache.org/repositories/index.html
  - Maven Central Repository: https://maven.apache.org/repository/index.html
    - Central Index: `/.index`: https://maven.apache.org/repository/central-index.html
    - Maven2 Repository Layout: https://maven.apache.org/repository/layout.html
  - Maven Artifacts: https://maven.apache.org/repositories/artifacts.html
- Introduction to Build Profiles: https://maven.apache.org/guides/introduction/introduction-to-profiles.html

- https://maven.apache.org/guides/introduction/introduction-to-repositories.html
- https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
- https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html
- https://maven.apache.org/guides/mini/guide-multiple-repositories.html
- https://maven.apache.org/maven-ci-friendly.html
- https://maven.apache.org/maven-indexer/index.html
- https://maven.apache.org/maven-indexer/indexer-core/index.html
- https://maven.apache.org/maven-indexer/indexer-reader/index.html
- https://maven.apache.org/repositories/metadata.html
- https://maven.apache.org/repositories/remote.html
- https://maven.apache.org/repository/central-index.html
- https://maven.apache.org/repository/layout.html
- https://maven.apache.org/resolver/apidocs/org/eclipse/aether/metadata/Metadata.Nature.html
- https://maven.apache.org/resolver/maven-resolver-supplier/dependency-info.html
- https://maven.apache.org/xsd/maven-4.0.0.xsd
- https://maven.apache.org/xsd/repository-metadata-1.1.0.xsd

- https://maven.apache.org/ref/3.6.3/maven-core/apidocs/org/apache/maven/bridge/MavenRepositorySystem.html
- https://maven.apache.org/ref/3.9.8/maven-model/maven.html
- https://maven.apache.org/ref/3.9.8/maven-repository-metadata/
- https://maven.apache.org/ref/3.9.8/maven-repository-metadata/repository-metadata.html

transitive dependencies

- Coord syntax: https://maven.apache.org/resolver/apidocs/org/eclipse/aether/artifact/DefaultArtifact.html#%3Cinit%3E(java.lang.String)

- https://maven.apache.org/ref/3.9.1/apidocs/org/apache/maven/repository/RepositorySystem.html
- https://maven.apache.org/ref/3.9.1/apidocs/org/apache/maven/repository/RepositorySystem.html#getEffectiveRepositories(java.util.List)

## Maven Central Links

- https://repo1.maven.org/maven2/
  - https://repo1.maven.org/maven2/.index/
    - https://repo1.maven.org/maven2/.index/nexus-maven-repository-index.gz
  - https://repo1.maven.org/maven2/.meta/
    - https://repo1.maven.org/maven2/.meta/prefixes.txt
    - https://repo1.maven.org/maven2/.meta/repository-metadata.xml
  - https://repo1.maven.org/maven2/.m2e/
- https://repo.maven.apache.org/
  - https://repo.maven.apache.org/maven2/.meta/repository-metadata.xml
- https://repo.maven.apache.org/maven2/.meta/repository-metadata.xml
- https://storage-download.googleapis.com/maven-central/index.html


https://stackoverflow.com/questions/11612671/how-to-query-maven-repository-from-application-to-obtain-metadata
https://svn.apache.org/repos/asf/servicemix/m2-repo/

https://maven.apache.org/resolver/xref/index.html
https://maven.apache.org/resolver-archives/resolver-1.9.0/maven-resolver-demos/maven-resolver-demo-snippets/xref/org/apache/maven/resolver/examples/util/ConsoleDependencyGraphDumper.html

https://stackoverflow.com/questions/2619598/differences-between-dependencymanagement-and-dependencies-in-maven
https://howtodoinjava.com/maven/maven-dependency-management/

getBaseVersion vs version
maven parallel download
https://stackoverflow.com/questions/32299902/parallel-downloads-of-maven-artifacts
org.apache.maven.plugins parallel-downloads

## Maven Libraries

### Source Code

- https://github.com/apache/maven/blob/master/maven-compat/src/main/java/org/apache/maven/repository/RepositorySystem.java

- https://github.com/apache/maven-resolver/blob/master/maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/DefaultUpdatePolicyAnalyzer.java#L38
- https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.21/maven-resolver-supplier/src/main/java/org/eclipse/aether/supplier/RepositorySystemSupplier.java#L234

- https://maven.apache.org/maven-indexer/

- https://github.com/apache/maven-resolver/
- https://github.com/apache/maven-resolver/blob/master/maven-resolver-util/src/main/java/org/eclipse/aether/util/repository/DefaultMirrorSelector.java
- https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.21/maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/DefaultArtifactResolver.java

- https://github.com/apache/maven-resolver/blob/master/maven-resolver-api/src/main/java/org/eclipse/aether/repository/RemoteRepository.java
- https://github.com/apache/maven-resolver/blob/master/maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/EnhancedLocalRepositoryManager.java
- https://github.com/apache/maven-resolver/tree/master

- https://github.com/apache/maven-indexer
- https://github.com/apache/maven-indexer/blob/master/indexer-reader/src/main/java/org/apache/maven/index/reader/Record.java
- https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.20/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/traverser/FatArtifactTraverser.java
- https://github.com/apache/maven/blob/c3f1cd6f76bd296a4e7c552990eff27afa1c4825/maven-core/src/main/java/org/apache/maven/plugin/prefix/internal/DefaultPluginPrefixResolver.java#L169
- https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.20/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/transformer/SimpleOptionalitySelector.java
- https://github.com/apache/maven-resolver/blob/master/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/transformer/JavaDependencyContextRefiner.java

### API Documentation

- https://maven.apache.org/resolver/apidocs/org/eclipse/aether/util/graph/transformer/NearestVersionSelector.html

- https://javadoc.io/doc/org.apache.maven.resolver/maven-resolver-util/1.9.20/org/eclipse/aether/util/graph/transformer/ConflictResolver.html
- https://javadoc.io/static/org.apache.maven.resolver/maven-resolver-util/1.9.20/org/eclipse/aether/util/graph/transformer/JavaDependencyContextRefiner.html
- https://javadoc.io/static/org.apache.maven.resolver/maven-resolver-util/1.9.20/org/eclipse/aether/util/graph/transformer/ConflictResolver.ScopeSelector.html
- https://javadoc.io/static/org.apache.maven.resolver/maven-resolver-util/1.9.20/org/eclipse/aether/util/graph/transformer/ConflictResolver.OptionalitySelector.html
- https://javadoc.io/static/org.apache.maven.resolver/maven-resolver-util/1.9.20/org/eclipse/aether/util/graph/transformer/ConflictResolver.ScopeDeriver.html
- https://maven.apache.org/ref/3.8.5/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html

## Third Party

https://github.com/borisbaldassari/maven-index-exporter/blob/main/docs/maven_repositories/list_maven_repositories_with_index.txt

https://github.com/javasoze/clue

### Maven Index Explorer

- https://github.com/borisbaldassari/maven-index-exporter/blob/main/README.md
- https://github.com/borisbaldassari/maven-index-exporter/blob/main/docs/maven_repositories.md
- https://github.com/borisbaldassari/maven-index-exporter/blob/main/docs/maven_repositories/list_maven_repositories_with_index.txt

## Aether

- https://wiki.eclipse.org/Aether
- https://wiki.eclipse.org/Aether/Resolving_Dependencies
- https://maven.apache.org/resolver/apidocs/org/eclipse/aether/metadata/Metadata.html
- https://stackoverflow.com/questions/15094751/how-to-get-all-maven-dependencies-using-aether

- https://maven.apache.org/resolver/apidocs/org/eclipse/aether/class-use/RepositorySystem.html
- https://maven.apache.org/resolver/apidocs/org/eclipse/aether/util/graph/traverser/FatArtifactTraverser.html

## MVN Repository

- https://mvnrepository.com/repos
- https://mvnrepository.com/artifact/org.apache.lucene/lucene-analyzers-common
- https://mvnrepository.com/artifact/org.apache.lucene/lucene-analysis-common
- https://mvnrepository.com/artifact/org.mock-server/mockserver-netty/5.15.0
- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.17.1
- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations/2.17.1
- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core/2.17.1
- https://repo1.maven.org/maven2/org/mock-server/mockserver/5.15.0/mockserver-5.15.0.pom
- https://repo1.maven.org/maven2/com/fasterxml/jackson/jackson-base/2.17.1/jackson-base-2.17.1.pom

## "Interesting" Artifacts

- https://maven-central-asia.storage-download.googleapis.com/maven2/com/inkapplications/spondee/math-macosx64/0/0/3/com%202/inkapplications/spondee/maven-metadata.xml
- https://mvnrepository.com/artifact/incubator-derby/derby
- https://repo1.maven.org/maven2/app/ubie/
- https://repo1.maven.org/maven2/com/inkapplications/spondee/math-macosx64/
- https://repo1.maven.org/maven2/com/inkapplications/spondee/math-macosx64/0.0.3/
- https://repo1.maven.org/maven2/com/inkapplications/spondee/math-macosx64/0/0/3/com%202
- https://repo1.maven.org/maven2/com/inkapplications/spondee/math-macosx64/maven-metadata.xml
- https://repo1.maven.org/maven2/org/apache-extras/camel-extra/camel-extra-itest-karaf/maven-metadata.xml
- https://repo1.maven.org/maven2/org/apache-extras/camel-extra/tests/maven-metadata.xml
- https://repo1.maven.org/maven2/org/fusesource/fabric/fabric-webui/
- https://repo1.maven.org/maven2/org/fusesource/fabric/fabric-webui/7.1.0.fuse-014/fabric-webui-7.1.0.fuse-014.pom
- https://repo1.maven.org/maven2/org/umlg/umlg-archetype/maven-metadata.xml
- https://repo1.maven.org/maven2/xml-apis/xml-apis/
- https://repo1.maven.org/maven2/xyz/raylab/raylab-modules-spring-boot-starter/
- ai.kognition.pilecv4j:opencv

## "Interesting" Repositories

- https://repo.typesafe.com/ (now redirected)
- https://maven.wso2.org/nexus/content/repositories/releases/com/
- https://mvnrepository.com/repos/pentaho-omni
- https://maven.antelink.com/content/repositories/central/
- https://netbeans.apache.org/front/main/about/oracle-transition.html
- https://bits.netbeans.org/maven2/


## Dependency Fields

```
"dependency": {
  "artifact": {
    "properties": {
      "language": "none",
      "constitutesBuildPath": "false",
      "type": "jar",
      "includesDependencies": "false"
    }
  },
  "scope": "compile",
  "optional": false,
  "exclusions": []
}
"requestContext": "",
"data": {}
```

## Artifact Not Found

```
ERROR maven.Dependencies: org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
org.eclipse.aether.collection.DependencyCollectionException: Failed to collect dependencies at ai.starlake:starlake-spark2_2.11:jar:0.3.26 -> org.apache.hadoop:hadoop-common:jar:3.3.4 -> com.sun.jersey:jersey-json:jar:1.19 -> org.eclipse.persistence:org.eclipse.persistence.moxy:jar:2.3.2
	at org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate.collectDependencies(DependencyCollectorDelegate.java:260)
	at org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector.collectDependencies(DefaultDependencyCollector.java:87)
	at org.eclipse.aether.internal.impl.DefaultRepositorySystem.collectDependencies(DefaultRepositorySystem.java:306)
	at org.ucombinator.jade.maven.Dependencies$main$4.invoke(Dependencies.kt:462)
	at org.ucombinator.jade.maven.Dependencies$main$4.invoke(Dependencies.kt:348)
	at org.ucombinator.jade.util.Parallel$run$2$1.invokeSuspend(Parallel.kt:118)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:104)
	at kotlinx.coroutines.internal.LimitedDispatcher$Worker.run(LimitedDispatcher.kt:111)
	at kotlinx.coroutines.scheduling.TaskImpl.run(Tasks.kt:99)
	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:811)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:715)
	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:702)
Caused by: org.eclipse.aether.resolution.ArtifactDescriptorException: Failed to read artifact descriptor for org.eclipse.persistence:org.eclipse.persistence.moxy:jar:2.3.2
	at org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.loadPom(DefaultArtifactDescriptorReader.java:245)
	at org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.readArtifactDescriptor(DefaultArtifactDescriptorReader.java:175)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.resolveCachedArtifactDescriptor(DfDependencyCollector.java:382)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.getArtifactDescriptorResult(DfDependencyCollector.java:368)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:218)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:156)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.process(DfDependencyCollector.java:138)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.doRecurse(DfDependencyCollector.java:343)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:277)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:156)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.process(DfDependencyCollector.java:138)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.doRecurse(DfDependencyCollector.java:343)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:277)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:156)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.process(DfDependencyCollector.java:138)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.doCollectDependencies(DfDependencyCollector.java:108)
	at org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate.collectDependencies(DependencyCollectorDelegate.java:222)
	... 13 common frames omitted
Caused by: org.eclipse.aether.resolution.ArtifactResolutionException: The following artifacts could not be resolved: org.eclipse.persistence:org.eclipse.persistence.moxy:pom:2.3.2 (absent): Could not find artifact org.eclipse.persistence:org.eclipse.persistence.moxy:pom:2.3.2 in google-maven-central-ap (https://maven-central-asia.storage-download.googleapis.com/maven2)
	at org.eclipse.aether.internal.impl.DefaultArtifactResolver.resolve(DefaultArtifactResolver.java:473)
	at org.eclipse.aether.internal.impl.DefaultArtifactResolver.resolveArtifacts(DefaultArtifactResolver.java:261)
	at org.eclipse.aether.internal.impl.DefaultArtifactResolver.resolveArtifact(DefaultArtifactResolver.java:243)
	at org.ucombinator.jade.maven.Dependencies$main$system$1$getArtifactResolver$1$1.resolveArtifact(Dependencies.kt:258)
	at org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.loadPom(DefaultArtifactDescriptorReader.java:234)
	at org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.readArtifactDescriptor(DefaultArtifactDescriptorReader.java:175)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.resolveCachedArtifactDescriptor(DfDependencyCollector.java:382)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.getArtifactDescriptorResult(DfDependencyCollector.java:368)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:218)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:156)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.process(DfDependencyCollector.java:138)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.doRecurse(DfDependencyCollector.java:343)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:277)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:156)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.process(DfDependencyCollector.java:138)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.doRecurse(DfDependencyCollector.java:343)
	at org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector.processDependency(DfDependencyCollector.java:277)
	... 25 common frames omitted
Caused by: org.eclipse.aether.transfer.ArtifactNotFoundException: Could not find artifact org.eclipse.persistence:org.eclipse.persistence.moxy:pom:2.3.2 in google-maven-central-ap (https://maven-central-asia.storage-download.googleapis.com/maven2)
	at org.eclipse.aether.connector.basic.ArtifactTransportListener.transferFailed(ArtifactTransportListener.java:42)
	at org.eclipse.aether.connector.basic.BasicRepositoryConnector$TaskRunner.run(BasicRepositoryConnector.java:417)
	at org.eclipse.aether.connector.basic.BasicRepositoryConnector.get(BasicRepositoryConnector.java:260)
	at org.eclipse.aether.internal.impl.DefaultArtifactResolver.performDownloads(DefaultArtifactResolver.java:537)
	at org.eclipse.aether.internal.impl.DefaultArtifactResolver.resolve(DefaultArtifactResolver.java:449)
	... 41 common frames omitted
```

## To Be Filed

- https://svn.apache.org/repos/infra/websites/production/maven/content/pom.html
- https://repo1.maven.org/maven2/org/ow2/jasmine/monitoring/jasmine-monitoring-integration-tests-modules-timerchecker/1.2.7/
- https://cwiki.apache.org/confluence/display/MAVEN/Maven+3.x+Compatibility+Notes#Maven3.xCompatibilityNotes-PluginMetaversionResolution
- https://cloud.google.com/artifact-registry/docs/java/authentication
- https://cloudplatform.googleblog.com/2015/11/faster-builds-for-Java-developers-with-Maven-Central-mirror.html

In Maven, a repository content type refers to the format or structure of the artifacts (such as JAR files, WAR files, etc.) stored within a Maven repository.
The main repository content types in Maven are:
1. **Maven 2/3 Repository (default)**: This is the default and most commonly used repository content type in Maven. It follows the Maven 2/3 repository layout, where artifacts are organized into directories based on their group ID, artifact ID, and version.
2. **Legacy Maven 1 Repository**: This content type is used for repositories that follow the older Maven 1 repository layout, which was used in earlier versions of Maven.
3. **Ivy Repository**: This content type is used for repositories that follow the Ivy repository layout, which is a different format used by the Ivy dependency management tool.
4. **P2 Repository**: This content type is used for repositories that follow the P2 (Eclipse Plug-in) repository layout, which is commonly used for Eclipse plugin dependencies.
5. **Raw Repository**: This content type is used for repositories that do not follow any specific layout and simply store the artifacts as-is, without any directory structure.
The repository content type is important because it determines how Maven will interact with the repository and how it will locate and download the required artifacts. When configuring a Maven repository, you need to specify the correct content type to ensure that Maven can properly access and manage the artifacts stored in the repository.

mvn://
https://www.iana.org/assignments/uri-schemes/prov/mvn
   mvn:org.ops4j.pax.web.bundles/service/0.2.0-SNAPSHOT
   mvn:http://user:password&repository.ops4j.org/maven2!org.ops4j.pax.web.bundles/service/0.2.0

What percent succeed with all maven?
What percent succeed with just central?

No Metadata: https://repo1.maven.org/maven2/acegisecurity/acegi-security-adapters/
