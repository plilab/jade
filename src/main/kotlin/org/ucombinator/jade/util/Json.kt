package org.ucombinator.jade.util

import org.eclipse.aether.RequestTrace
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.repository.ArtifactRepository
import org.eclipse.aether.repository.LocalArtifactRequest
import org.eclipse.aether.repository.LocalArtifactResult
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.Proxy
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.WorkspaceRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.version.Version
import org.eclipse.aether.version.VersionConstraint
import org.eclipse.aether.version.VersionRange

import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** TODO:doc. */
@Suppress("dekect:StringLiteralDuplication")
object Json {
  // ////// Any

  fun ofAny(value: Any?): JsonElement =
    when (value) {
      is JsonElement -> value
      is Boolean -> of(value)
      is Number -> of(value)
      is String -> of(value)
      is Nothing? -> of(value)
      is Array<*> -> of(value.map(::ofAny))
      // is Map<String, Any> -> of(value)
      else -> Errors.unmatchedType(value)
    }

  // ////// Primitive

  /** TODO:doc. */
  fun of(value: Boolean?): JsonPrimitive = JsonPrimitive(value)

  /** TODO:doc. */
  fun of(value: Number?): JsonPrimitive = JsonPrimitive(value)

  /** TODO:doc. */
  fun of(value: String?): JsonPrimitive = JsonPrimitive(value)

  /** TODO:doc. */
  fun of(@Suppress("UNUSED_PARAMETER") value: Nothing?): JsonNull = JsonNull

  // TODO: Support JsonPrimitive only

  /** TODO:doc. */
  fun <T> nullOrElse(value: T?, block: (T) -> JsonElement): JsonElement = if (value == null) JsonNull else block(value)

  // ////// Object

  /** TODO:doc. */
  @JvmName("ofMapJsonElement")
  fun of(value: Map<String, JsonElement>): JsonObject = JsonObject(value)

  /** TODO:doc. */
  fun of(vararg value: Pair<String, JsonElement>): JsonObject = of(linkedMapOf(*value))

  /** TODO:doc. */
  @JvmName("ofMapString")
  fun of(value: Map<String, String>): JsonObject = JsonObject(value.mapValues { (_, value) -> of(value) })

  // TODO: remove fun ofMapAny(value: Map<String, *>): JsonObject = JsonObject(value.mapValues { (_, value) -> ofAny(value) })

  // ////// Array

  /** TODO:doc. */
  fun of(value: Collection<JsonElement>): JsonArray = JsonArray(value.toList())

  // ////// Other

  // TODO: move to MavenJson.kt

  /** TODO:doc. */
  fun of(value: Artifact?): JsonElement =
    nullOrElse(value) {
      of(
        "groupId" to of(it.groupId),
        "artifactId" to of(it.artifactId),
        "version" to of(it.version),
        "classifier" to of(it.classifier),
        "extension" to of(it.extension),
        "file" to of(it.file?.toString()),
        "properties" to of(it.properties),
      )
    }

  /** TODO:doc. */
  fun of(value: Dependency?): JsonElement =
    nullOrElse(value) {
      of(
        "artifact" to of(it.artifact),
        "scope" to of(it.scope),
        "optional" to of(it.optional),
        "exclusions" to of(it.exclusions.map(::of)),
      )
    }

  /** TODO:doc. */
  fun of(value: Exclusion?): JsonElement =
    nullOrElse(value) {
      of(
        "groupId" to of(it.groupId),
        "artifactId" to of(it.artifactId),
        "classifier" to of(it.classifier),
        "extension" to of(it.extension),
      )
    }

  /** TODO:doc. */
  fun of(value: Proxy?): JsonElement =
    nullOrElse(value) {
      of(
        "type" to of(it.type),
        "host" to of(it.host),
        "port" to of(it.port),
        // "authentication" to of(it.authentication),
      )
    }

  // fun of(value: Authentication): JsonObject = TODO()

  /** TODO:doc. */
  fun of(value: RepositoryPolicy?): JsonElement =
    nullOrElse(value) {
      of(
        "enabled" to of(it.isEnabled),
        "updatePolicy" to of(it.updatePolicy),
        "checksumPolicy" to of(it.checksumPolicy),
      )
    }

  /** TODO:doc. */
  fun of(value: Version?): JsonElement = nullOrElse(value) { of(it.toString()) }

  /** TODO:doc. */
  fun of(value: VersionConstraint?): JsonElement =
    nullOrElse(value) {
      of(
        // NOTE: only one of these will be non-null
        "range" to of(it.range),
        "version" to of(it.version),
      )
    }

  /** TODO:doc. */
  fun of(value: VersionRange?): JsonElement =
    nullOrElse(value) {
      of(
        "lowerBound" to of(it.lowerBound),
        "upperBound" to of(it.upperBound),
      )
    }

  /** TODO:doc. */
  fun of(value: VersionRange.Bound?): JsonElement =
    nullOrElse(value) {
      of(
        "version" to of(it.version),
        "inclusive" to of(it.isInclusive),
      )
    }

  /** TODO:doc. */
  fun of(value: DependencyNode?): JsonElement =
    nullOrElse(value) {
      of(
        "dependency" to of(it.dependency),
        "artifact" to of(it.artifact),
        "relocations" to of(it.relocations.map(::of)),
        "aliases" to of(it.aliases.map(::of)),
        "versionConstraint" to of(it.versionConstraint),
        "version" to of(it.version),
        "managedBits" to of(it.managedBits),
        // TODO:
        //  int MANAGED_VERSION = 1;
        //  int MANAGED_SCOPE = 2;
        //  int MANAGED_OPTIONAL = 4;
        //  int MANAGED_PROPERTIES = 8;
        //  int MANAGED_EXCLUSIONS = 16;
        "repositories" to of(it.repositories.map(::of)),
        "requestContext" to of(it.requestContext),
        // "data" to ofMapAny(it.data.mapKeys { it.key.toString() }),
        "data" to of(it.data.map { it.key.toString() to ofAny(it.value) }.toMap()), // TODO: test
        "children" to of(it.children.map(::of)),
      )
    }

  /** TODO:doc. */
  fun of(value: Throwable?): JsonElement =
    nullOrElse(value) {
      of(
        "message" to of(it.message),
        "localizedMessage" to of(it.localizedMessage),
        "stackTrace" to of(it.stackTrace.map(::of)),
        "suppressed" to of(it.suppressed.map(::of)),
        "cause" to of(it.cause),
      )
    }

  /** TODO:doc. */
  fun of(value: StackTraceElement?): JsonElement =
    nullOrElse(value) {
      of(
        "fileName" to of(it.fileName),
        "lineNumber" to of(it.lineNumber),
        "className" to of(it.className),
        "methodName" to of(it.methodName),
        "nativeMethod" to of(it.isNativeMethod),
      )
    }

  /** TODO:doc. */
  fun of(value: File?): JsonElement = nullOrElse(value) { of(it.toString()) } // TODO: better way?

  /** TODO:doc. */
  fun of(value: ArtifactResult?): JsonElement =
    nullOrElse(value) {
      of(
        "request" to of(it.request),
        "artifact" to of(it.artifact),
        "exceptions" to of(it.exceptions.map(::of)),
        "repository" to of(it.repository),
        "localArtifactResult" to of(it.localArtifactResult),
      )
    }

  /** TODO:doc. */
  fun of(value: ArtifactRequest?): JsonElement =
    nullOrElse(value) {
      of(
        "artifact" to of(it.artifact),
        "dependencyNode" to of(it.dependencyNode),
        "repositories" to of(it.repositories.map(::of)),
        "requestContext" to of(it.requestContext),
        "trace" to of(it.trace),
      )
    }

  /** TODO:doc. */
  fun of(value: ArtifactRepository?): JsonElement =
    nullOrElse(value) {
      when (it) {
        is LocalRepository -> of(it)
        is RemoteRepository -> of(it)
        is WorkspaceRepository -> of(it)
        else -> Errors.unmatchedType(it)
      }
    }

  /** TODO:doc. */
  fun of(value: LocalRepository?): JsonElement =
    nullOrElse(value) {
      of(
        "id" to of(it.id),
        "contentType" to of(it.contentType),
        "basedir" to of(it.basedir),
      )
    }

  /** TODO:doc. */
  fun of(value: RemoteRepository?): JsonElement =
    nullOrElse(value) {
      of(
        "id" to of(it.id),
        "contentType" to of(it.contentType),
        "url" to of(it.url),
        // "protocol" to of(it.protocol), // TODO: derived from url (cite)
        // "host" to of(it.host),
        "releasePolicy" to of(it.getPolicy(false)),
        "snapshotPolicy" to of(it.getPolicy(true)),
        "proxy" to of(it.proxy),
        // "authentication" to of(it.authentication), // TODO: has no accessors
        "mirroredRepositories" to of(it.mirroredRepositories.map(::of)),
        "repositoryManager" to of(it.isRepositoryManager),
        "blocked" to of(it.isBlocked),
      )
    }

  /** TODO:doc. */
  fun of(value: WorkspaceRepository?): JsonElement =
    nullOrElse(value) {
      of(
        "id" to of(it.id),
        "contentType" to of(it.contentType),
        "key" to of(it.key.toString()),
      )
    }

  /** TODO:doc. */
  fun of(value: RequestTrace?): JsonElement =
    nullOrElse(value) {
      of(
        "data" to of(it.data.toString()),
        "parent" to of(it.parent),
      )
    }

  /** TODO:doc. */
  fun of(value: LocalArtifactResult?): JsonElement =
    nullOrElse(value) {
      of(
        "request" to of(it.request),
        "file" to of(it.file),
        "available" to of(it.isAvailable),
        "repository" to of(it.repository),
      )
    }

  /** TODO:doc. */
  fun of(value: LocalArtifactRequest?): JsonElement =
    nullOrElse(value) {
      of(
        "artifact" to of(it.artifact),
        "context" to of(it.context),
        "repositories" to of(it.repositories.map(::of)),
      )
    }
}
