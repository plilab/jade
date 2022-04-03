package org.ucombinator.jade.maven

import org.ucombinator.jade.util.Log
import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.*

import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.util.artifact.JavaScopes
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
// import org.apache.maven.resolver.examples.util.ConsoleDependencyGraphDumper
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.resolution.VersionRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.graph.DependencyNode

import java.util.concurrent.atomic.AtomicInteger
import java.util.Collections
import java.time.LocalDateTime

object DownloadMaven {
  private val log = Log {}
  var noRelease = AtomicInteger()
  val artifacts = Collections.synchronizedSet(mutableSetOf<Triple<String, String, String>>())
  fun main(indexFile: File, localRepoDir: File) {
    val PREFIX = "maven2/"
    val PREFIX_LEN = PREFIX.length
    val SUFFIX = "/maven-metadata.xml"
    val SUFFIX_LEN = SUFFIX.length
    indexFile.useLines { lines ->
      val versions = lines
        .map { it.substring(0, it.lastIndexOf('\t')) }
        .mapNotNull {
          if (!it.endsWith(SUFFIX)) { null }
          else if (!it.startsWith(PREFIX)) { null }
          else if (it.startsWith("maven2/data/")) { null }
          else {
            val x = it.substring(PREFIX_LEN, it.length - SUFFIX_LEN)
            val i = x.lastIndexOf('/')
            if (i == -1) { null }
            else {
              Pair(x.substring(0, i).replace('/', '.'), x.substring(i + 1))
            }
          }
        }
        // .take(100)

      val locator = MavenRepositorySystemUtils.newServiceLocator()
      locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
      locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
      locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

      // locator.setErrorHandler( object : DefaultServiceLocator.ErrorHandler() {
      //   override fun serviceCreationFailed(type: Class<*>, impl: Class<*>, exception: Throwable) {
      //     TODO()
      //   }
      // })

      val system = locator.getService(RepositorySystem::class.java)
      val session = MavenRepositorySystemUtils.newSession()
      val localRepo = LocalRepository(localRepoDir)
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))

      val remote = RemoteRepository
        .Builder(null, "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
        .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
        .build()

      runBlocking {

        for ((groupId, artifactId) in versions) {
          // if (groupId.startsWith("com.")) continue
          // if (groupId.startsWith("org.")) continue
          // if (groupId.startsWith("io.")) continue
          // if (groupId[0] != 'a') continue

          // if (groupId != "org.webjars.npm") continue
          // if (!groupId.startsWith("org.webjars")) continue
          if ((groupId == "org.webjars.npm" &&
            listOf(
              "3dmol",
              "admin-lte",
              "adminlte-reactjs",
              "aframe",
              "alfresco-js-api",
              "formio",
              "github-com-MyEtherWallet-MEWconnect-web-client",
              "github-com-palantir-blueprint",
              "jest",
              "jest__reporters",
              "jest-runtime",
              "nuxt",
              "angular-data-grid",
              "angular-devkit__build-angular",
              "angular-patternfly",
              "apollo-language-server",
              "asyncapi__web-component",
              "atlassian__aui",
              "aurelia-cli",
              "aurelia-webpack-plugin",
              "ava",
              "babel-cli",
              "babel-plugin-add-module-exports",
              "babel-plugin-transform-amd-system-wrapper",
              "babel-plugin-transform-cjs-system-wrapper",
              "babel-plugin-transform-decorators-legacy",
              "babel-plugin-transform-global-system-wrapper",
              "babel-preset-es2015-loose",
              "babel-preset-es2015-webpack",
              "bourbon-bitters",
              "brfs",
              "canvas2pdf",
              "cli-color",
              "cssnano-preset-default",
              "cssnano",
              "cucumber",
              "d3-geomap",
              "debug-fabulous",
              "derequire",
              "documentation",
              "dr-svg-sprites",
              "duration",
              "elastic__eui",
              "email-builder-core",
              "ember-cli-babel",
              "ember-data",
              "ember-source",
              "emissary",
              "enzyme",
              "es6-iterator",
              "escope",
              "esniff",
              "excaliburjs",
              "fbjs-scripts",
              "first-mate",
              "fmin",
              "foliojs-fork__fontkit",
              "foliojs-fork__pdfkit",
              "fomantic-ui",
              "fontkit",
              "formio-workers",
              "frisby",
              "fs2",
              "github-com-angular-flex-layout",
              "github-com-ant-design-ant-design",
              "github-com-arnellebalane-hermes",
              "github-com-aurelia-webpack-plugin",
              "github-com-bpampuch-pdfmake",
              "github-com-brightcove-videojs-overlay",
              "github-com-brightcove-videojs-playlist",
              "github-com-ColorlibHQ-AdminLTE",
              "github-com-d2-projects-d2-admin",
              "github-com-DesignRevision-shards-dashboard",
              "github-com-EmergingTechnologyAdvisors-node-serialport",
              "github-com-facebook-regenerator",
              "github-com-haraldrudell-react-swagger-ui",
              "github-com-JetBrains-ring-ui",
              "github-com-jitsi-jitsi-meet",
              "github-com-jmurphyau-ember-truth-helpers",
              "github-com-knsv-mermaid",
              "github-com-krystalcampioni-vue-hotel-datepicker",
              "github-com-marmelab-admin-on-rest",
              "github-com-medikoo-memoizee",
              "github-com-modularcode-modular-admin-html",
              "github-com-naomiaro-waveform-playlist",
              "github-com-polymer-polymer-cli",
              "github-com-Polymer-polymer-cli",
              "github-com-swagger-api-swagger-ui",
              "github-com-uber-react-map-gl",
              "github-com-yegor256-tacit",
              "gitlab-com-meno-dropzone",
              "graphql-import",
              "graphql-playground-react",
              "graphql-toolkit__apollo-engine-loader",
              "graphql-voyager",
              "gulp-cli",
              "gulp-copy",
              "gulp-diff",
              "gulp-eslint",
              "gulp-sass",
              "gulp-useref",
              "gulp",
              "highlights",
              "htmlhint",
              "htmlnano",
              "is-hundred",
              "jest__test-sequencer",
              "jest-cli",
              "jest-environment-jsdom",
              "jest-jasmine2",
              "jest-resolve-dependencies",
              "jest-snapshot",
              "jscs",
              "json-schema-to-typescript",
              "kafka-node",
              "last-run",
              "leaflet-realtime",
              "lerna",
              "linebreak",
              "marathon-ui",
              "material-ui__utils",
              "material-ui",
              "memoizee",
              "metro-babel-register",
              "metro-react-native-babel-transformer",
              "minimize",
              "mqtt",
              "ncjsm",
              "node-sass",
              "oas-raml-converter",
              "ol3-layerswitcher",
              "pdfkit",
              "pdfmake",
              "plotly.js",
              "polyfill-service",
              "postcss-svgo",
              "property-accessors",
              "ra-core",
              "ra-ui-materialui",
              "rails__webpacker",
              "raml2html",
              "react-admin",
              "react-bootstrap-select",
              "react-bootstrap-typeahead",
              "react-confirm-bootstrap",
              "react-map-gl",
              "react-native",
              "react-scripts",
              "react-styleguidist",
              "react-web",
              "regl-line2d",
              "rollup-plugin-postcss",
              "sass-lint",
              "scope-analyzer",
              "scssify",
              "serialport",
              "static-module",
              "supermap__iclient-ol",
              "svg-to-pdfkit",
              "svgo",
              "swagger-editor",
              "swagger-ui",
              "systemjs-builder",
              "tableexport.jquery.plugin",
              "ts-jest",
              "turf__turf",
              "types__protractor",
              "types__react-loadable",
              "unassertify",
              "undertaker",
              "venn.js",
              "vinyl-fs",
              "vue-element-utils",
              "vue-hotel-datepicker",
              "vueify",
              "waveform-playlist",
              "web3-bzz",
              "web3-core-helpers",
              "web3-core-method",
              "web3-core-requestmanager",
              "web3-core-subscriptions",
              "web3-core",
              "web3-eth-abi",
              "web3-eth-accounts",
              "web3-eth-contract",
              "web3-eth-ens",
              "web3-eth-iban",
              "web3-eth-personal",
              "web3-eth",
              "web3-net",
              "web3-providers-http",
              "web3-providers-ws",
              "web3-utils",
              "web3",
              "webpack-dev-server",
              "wrtc",
            ).contains(artifactId)
          )) continue
          // println("??? <$groupId> <$artifactId>")

          // $ grep '^#' out.txt | perl -ne 'chomp; s/^# //; my ($group, $art, $ver) = split(":"); $group =~ s[\.][/]g; $jars{"maven2/$group/$art/$ver/$art-$ver.jar"} = 1; END { open my $f, "../index-full.tsv"; while (my $line = <$f>) { chomp $line; my ($file, $size) = split("\t", $line); if ($jars{$file}) { $total += $size; } } print("$total\n"); }'|head

          // println(".... $coords")
          async(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val name = "$groupId:$artifactId"
            val d = async(Dispatchers.IO) {
              // println("     + $name")
              val start1 = System.currentTimeMillis()
              try {
                // val start2 = withTimeout(15 * 60 * 1000L) {
                runInterruptible {
                  val start3 = System.currentTimeMillis()
                  getArtifact(system, session, remote, groupId, artifactId)
                  start3
                }
                // val end = System.currentTimeMillis()
                // println("       $name (${(end - start2) / 1000.0} s)")
              } catch (e:  TimeoutCancellationException) {
                val end = System.currentTimeMillis()
                val time = (end - start1) / 1000.0
                // println(" ($time s)")
                if (time >= 90 * 60.0) println("XXXX   $name ($time s)")
                else println("!!!!   $name ($time s)")
              }
            }
            try {
              withTimeout(20 * 60 * 1000L) { d.await() }
            } catch (e:  TimeoutCancellationException) {
              val end = System.currentTimeMillis()
              val time = (end - start) / 1000.0
              // println("ZZZZ   $name ($time s)")
              // else println("!!!!")
            }
          }
        }

      // val x = dir.resolve("${groupId.replace(".", "/")}/${artifactId}/maven-metadata.xml")
      // if (file != x) {
      //   // wrongFileName++
      //   // log.error("File name doesn't match: $groupId $artifactId ${file.toString()} $x")
      //   return null
      // }
      }

      // for ((groupId, artifactId, version) in artifacts.toList().sortedBy({it.third}).sortedBy({it.second}).sortedBy({it.first})) {
      //   println("# $groupId:$artifactId:$version")
      // }
    }
  }

  fun isA(exception: Exception?, vararg classes: kotlin.reflect.KClass<*>): Boolean =
    isA(exception as Throwable?, *classes)
  fun isA(exception: Throwable?, vararg classes: kotlin.reflect.KClass<*>): Boolean {
    var e: Throwable? = exception
    for (c in classes) {
      if (e === null) { return false }
      if (!c.isInstance(e)) { return false }
      e = e.cause
    }
    return e === null
  }

  // val metadata = DefaultMetadata(groupId, artifactId, null, "maven-metadata.xml", Metadata.Nature.RELEASE)
  // // metadata.set
  // val metadataRequest = MetadataRequest()
  // metadataRequest.metadata = metadata
  // metadataRequest.setRepository(remote)
  // // metadataRequest.setRequestContext("")
  // metadataRequest.setRequestContext("maven-metadata.xml")
  // val metadataResults = system.resolveMetadata(session, listOf(metadataRequest))
  // println("$groupId/$artifactId/(${metadataResults.size})")
  // for (metadataResult in metadataResults) {
  //   // metadataResult.exception.printStackTrace()
  //   println("???? $coords-> ${metadataResult}")// - ${metadataResult.metadata}")
  // }

  fun getArtifact(system: RepositorySystem, session: DefaultRepositorySystemSession, remote: RemoteRepository, groupId: String, artifactId: String) {
    try {

      val releaseArtifact = DefaultArtifact(groupId, artifactId, null, org.apache.maven.artifact.Artifact.RELEASE_VERSION)
      val versionRequest = VersionRequest(releaseArtifact, listOf(remote), null)
      val versionResult = try {
        system.resolveVersion(session, versionRequest)
      } catch (e: Exception) {
        when {
          isA(
            e,
            org.eclipse.aether.resolution.VersionResolutionException::class,
            org.eclipse.aether.transfer.MetadataNotFoundException::class) -> {
              noRelease.incrementAndGet()
              // TODO: try LATEST?
              return
            }

          else -> throw e
        }
      }
      val version = versionResult.version
      if (version === null || version == org.apache.maven.artifact.Artifact.RELEASE_VERSION) throw Exception("bad version ($groupId, $artifactId): $version")

      val artifact = DefaultArtifact(groupId, artifactId, "jar", version)
      // val artifactRequest = ArtifactRequest(artifact, listOf(remote), null)
      // val artifactResult = system.resolveArtifact(session, artifactRequest)

      val descriptorRequest = ArtifactDescriptorRequest()
      descriptorRequest.setArtifact(artifact)
      descriptorRequest.setRepositories(listOf(remote))
      val descriptorResult = try {
        system.readArtifactDescriptor(session, descriptorRequest)
      } catch (e: org.eclipse.aether.resolution.ArtifactDescriptorException) {
        when {
          e.cause is org.apache.maven.model.resolution.UnresolvableModelException &&
          e.cause?.cause is org.eclipse.aether.resolution.ArtifactResolutionException ->
          when {
            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactNotFoundException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
              org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
              org.eclipse.aether.transfer.NoTransporterException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              java.net.UnknownHostException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              org.apache.http.client.HttpResponseException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              javax.net.ssl.SSLHandshakeException::class,
              java.io.EOFException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              org.apache.http.conn.HttpHostConnectException::class,
              java.net.ConnectException::class) ->
              {}

            isA(
              e.cause,
              org.eclipse.aether.transfer.ArtifactTransferException::class,
              org.apache.http.conn.ConnectTimeoutException::class,
              java.net.SocketTimeoutException::class) ->
              {}

            else ->
              log.error(e) { "Failed to get dependencies of $groupId:$artifactId" }
          }

          else ->
            log.error(e) { "Failed to get dependencies of $groupId:$artifactId" }
        }

        return
      }

      val collectRequest = CollectRequest()
      // collectRequest.setRoot(Dependency(artifact, JavaScopes.COMPILE))
      collectRequest.setRootArtifact(descriptorResult.artifact)
      collectRequest.setDependencies(descriptorResult.dependencies)
      collectRequest.setManagedDependencies(descriptorResult.managedDependencies)
      collectRequest.setRepositories(listOf(remote))
      val collectResult = try {
        system.collectDependencies(session, collectRequest)
      } catch (e: org.eclipse.aether.collection.DependencyCollectionException) {
        when {
          isA(
            e.cause,
            org.eclipse.aether.collection.UnsolvableVersionConflictException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.VersionRangeResolutionException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.VersionRangeResolutionException::class,
            org.eclipse.aether.version.InvalidVersionSpecificationException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.apache.maven.model.resolution.UnresolvableModelException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactNotFoundException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.apache.maven.model.resolution.UnresolvableModelException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            java.net.UnknownHostException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            java.net.UnknownHostException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            javax.net.ssl.SSLPeerUnverifiedException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.client.HttpResponseException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.conn.HttpHostConnectException::class,
            java.net.ConnectException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
            org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
            org.eclipse.aether.transfer.NoRepositoryLayoutException::class,
            org.eclipse.aether.transfer.NoRepositoryLayoutException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            javax.net.ssl.SSLHandshakeException::class,
            java.lang.Throwable::class, //sun.security.validator.ValidatorException::class,
            java.security.cert.CertPathValidatorException::class,
            java.security.cert.CertificateExpiredException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.conn.ConnectTimeoutException::class,
            java.net.SocketTimeoutException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.VersionResolutionException::class,
            org.eclipse.aether.transfer.MetadataNotFoundException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.conn.ConnectTimeoutException::class,
            java.net.SocketTimeoutException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.eclipse.aether.transfer.ChecksumFailureException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
            org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
            org.eclipse.aether.transfer.NoTransporterException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.client.ClientProtocolException::class,
            org.apache.http.client.CircularRedirectException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.apache.maven.model.resolution.UnresolvableModelException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            org.apache.http.client.HttpResponseException::class) ->
            {}

          isA(
            e.cause,
            org.eclipse.aether.resolution.ArtifactDescriptorException::class,
            org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactTransferException::class,
            javax.net.ssl.SSLHandshakeException::class,
            java.io.EOFException::class) ->
            {}

        }

        return
      }

      // collectResult.getRoot().accept(ConsoleDependencyGraphDumper())
      fun foo(x: DependencyNode) {
        when (x.dependency?.scope) {
          // null ->
          // compile
          // provided
          "runtime" -> return
          "test" -> return
          // system
          // import
        }
        // if (x.artifact !== x.dependency?.artifact) {
        //   log.error("dependency.artifact === artifact: ${x.artifact === x.dependency?.artifact} for ${x.artifact} ${x.dependency} ${groupId}:${artifactId}")
        // }
        if (false) {
          // println(x.aliases)
          println("------------")
          println("artifact: ${x.artifact}")
          println("artifact.artifactId: ${x.artifact.artifactId}")
          println("artifact.baseVersion: ${x.artifact.baseVersion}")
          println("artifact.classifier: ${x.artifact.classifier}")
          println("artifact.extension: ${x.artifact.extension}")
          println("artifact.file: ${x.artifact.file}")
          println("artifact.groupId: ${x.artifact.groupId}")
          println("artifact.properties: ${x.artifact.properties}")
          println("artifact.version: ${x.artifact.version}")
          println("data: ${x.data}")
          println("dependency: ${x.dependency}")
          println("dependency.artifact == artifact: ${x.artifact === x.dependency?.artifact}")
          println("dependency.exclusions: ${x.dependency?.exclusions}")
          println("dependency.optional: ${x.dependency?.optional}")
          println("dependency.scope: ${x.dependency?.scope}")
          println("managedBits: ${x.managedBits}")
          println("managedBigs.MANAGED_EXCLUSIONS: ${x.managedBits and DependencyNode.MANAGED_EXCLUSIONS != 0}")
          println("managedBigs.MANAGED_OPTIONAL: ${x.managedBits and DependencyNode.MANAGED_OPTIONAL != 0}")
          println("managedBigs.MANAGED_PROPERTIES: ${x.managedBits and DependencyNode.MANAGED_PROPERTIES != 0}")
          println("managedBigs.MANAGED_SCOPE: ${x.managedBits and DependencyNode.MANAGED_SCOPE != 0}")
          println("managedBigs.MANAGED_VERSION: ${x.managedBits and DependencyNode.MANAGED_VERSION != 0}")
          // println(x.relocations)
          // println(x.repositories)
          println("requestContext: ${x.requestContext}")
          println("version: ${x.version}")
          println("versionConstraint: ${x.versionConstraint}")

          // String premanaged = DependencyManagerUtils.getPremanagedVersion( node );
          // premanaged = DependencyManagerUtils.getPremanagedScope( node );
          // DependencyNode winner = (DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
          // if ( winner != null && !ArtifactIdUtils.equalsId( a, winner.getArtifact() ) )

        }

        if (x.artifact.extension == "jar") {
          artifacts.add(Triple(artifact.groupId, artifact.artifactId, artifact.version))
        }

        for (z in x.children) {
          foo(z)
        }
      }
      foo(collectResult.getRoot())
      // WARN  .org.apache.http.client.protocol.ResponseProcessCookies Invalid cookie header: ...
      // WARN  .org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer: Non-parseable repository update policy 'interval:1m', assuming 'interval:1440'
      // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
      // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: Broken pipe (Write failed)

    } catch (e: Exception) {
      when {
        isA(
          e,
          java.lang.IllegalStateException::class) ->
          {}
        // java.lang.IllegalStateException: Could not acquire write lock for 'artifact:org.activiti:activiti-dependencies:7.0.0-SNAPSHOT'

        isA(
          e,
          org.eclipse.aether.transfer.ChecksumFailureException::class) ->
          {}

        isA(
          e,
          java.lang.StringIndexOutOfBoundsException::class) ->
          {}

        isA(
          e,
          org.eclipse.aether.transfer.ChecksumFailureException::class) ->
          {}

          // WARN  .org.eclipse.aether.internal.impl.WarnChecksumPolicy: Could not validate integrity of download from http://maven.aliyun.com/mvn/repository/org/glassfish/javax.el/maven-metadata.xml
          // org.eclipse.aether.transfer.ChecksumFailureException: Checksum validation failed, expected <!doctype but is 2321baca4e611c8a1f8aebd0123a1fb15ceae0c8
          //         at org.eclipse.aether.connector.basic.ChecksumValidator.validateExternalChecksums(ChecksumValidator.java:174)
          //         at org.eclipse.aether.connector.basic.ChecksumValidator.validate(ChecksumValidator.java:103)
          //         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$GetTaskRunner.runTask(BasicRepositoryConnector.java:460)
          //         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$TaskRunner.run(BasicRepositoryConnector.java:364)
          //         at org.eclipse.aether.util.concurrency.RunnableErrorForwarder.lambda$wrap$0(RunnableErrorForwarder.java:73)
          //         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$DirectExecutor.execute(BasicRepositoryConnector.java:627)
          //         at org.eclipse.aether.connector.basic.BasicRepositoryConnector.get(BasicRepositoryConnector.java:235)
          //         at org.eclipse.aether.internal.impl.DefaultMetadataResolver$ResolveTask.run(DefaultMetadataResolver.java:582)
          //         at org.eclipse.aether.util.concurrency.RunnableErrorForwarder.lambda$wrap$0(RunnableErrorForwarder.java:73)
          //         at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
          //         at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
          //         at java.base/java.lang.Thread.run(Thread.java:829)
          // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: _octo=GH1.1.2098921104.1648787344; Path=/; Domain=github.com; Expires=Sat, 01 Apr 2023 04:29:04 GMT; Secure; SameSite=Lax". Invalid 'expires' attribute: Sat, 01 Apr 2023 04:29:04 GMT
          // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: logged_in=no; Path=/; Domain=github.com; Expires=Sat, 01 Apr 2023 04:29:04 GMT; HttpOnly; Secure; SameSite=Lax". Invalid 'expires' attribute: Sat, 01 Apr 2023 04:29:04 GMT

          // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: _octo=GH1.1.1341627974.1648787733; Path=/; Domain=github.com; Expires=Sat, 01 Apr 2023 04:35:33 GMT; Secure; SameSite=Lax". Invalid 'expires' attribute: Sat, 01 Apr 2023 04:35:33 GMT
          // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: logged_in=no; Path=/; Domain=github.com; Expires=Sat, 01 Apr 2023 04:35:33 GMT; HttpOnly; Secure; SameSite=Lax". Invalid 'expires' attribute: Sat, 01 Apr 2023 04:35:33 GMT


          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: Broken pipe (Write failed)
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: Broken pipe (Write failed)
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (org.apache.http.NoHttpResponseException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: The target server failed to respond
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: Broken pipe (Write failed)
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (org.apache.http.NoHttpResponseException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: The target server failed to respond
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
          // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (org.apache.http.NoHttpResponseException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: The target server failed to respond
          // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443

  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (org.apache.http.NoHttpResponseException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: The target server failed to respond
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443
  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://repository.jboss.org:443: Connection reset
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://repository.jboss.org:443
  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://repository.jboss.com:443: Connection reset by peer (Write failed)
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://repository.jboss.com:443
  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://repository.jboss.org:443: Connection reset by peer (Write failed)
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://repository.jboss.org:443
  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://repository.jboss.org:443: Connection reset by peer (Write failed)
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://repository.jboss.org:443
  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://repository.jboss.com:443: Connection reset by peer (Write failed)
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://repository.jboss.com:443

  // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (org.apache.http.NoHttpResponseException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: The target server failed to respond
  // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443

        else ->
          log.error(e) { "Failed to get dependencies of $groupId:$artifactId" }
      }
      // println("!!!! $coords (${(end - start) / 1000.0} s)")
    }
  }
}
