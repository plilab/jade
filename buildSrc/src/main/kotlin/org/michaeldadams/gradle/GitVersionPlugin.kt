// Set the version based on Git tags.
// Create a new version with `git tag -a v2023.01.01`.
// Based on <https://github.com/ArcticLampyrid/gradle-git-version/>.

package org.michaeldadams.gradle

import org.eclipse.jgit.api.Git
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date

// TODO: cannot do this because project.version is not a Property that can be based on a Provider
// import org.gradle.api.model.ObjectFactory
// import org.gradle.api.provider.Property
// abstract class GitVersionExtension constructor(objectFactory: ObjectFactory) {
//   val versionSuffix: Property<String> = objectFactory.property(String::class.java).convention("'-'yyyyMMdd'T'HHmmssXX")
//   val tagPrefix: Property<String> = objectFactory.property(String::class.java).convention("v")
//   val always: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
//   val long: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)
//   val tags: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
// }

class GitVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    if (project.version == Project.DEFAULT_VERSION) {
      try {
        project.version = Git.open(project.rootDir).use { git ->
          val tag = git.describe().setAlways(true).setLong(false).setMatch("v*").setTags(true).call()
          val suffix = SimpleDateFormat("'-'yyyyMMdd'T'HHmmssXX").format(Date())
          tag.removePrefix("v") + if (!git.status().call().isClean) { suffix } else { "" }
        }
      } catch (e: Throwable) {
        println("Failed to detect version for ${project.name} based on git info")
        e.printStackTrace()
      }
    }
  }
}
