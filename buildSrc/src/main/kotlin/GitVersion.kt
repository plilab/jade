// Based on <https://github.com/ArcticLampyrid/gradle-git-version/>.
//
// We copy that source instead of using that plugin because building with it
// throws an exception.

package org.ucombinator.jade.gradle

import org.eclipse.jgit.api.Git
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date

class GitVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    if (project.version == Project.DEFAULT_VERSION) {
      kotlin.runCatching {
        val describe: String
        val isClean: Boolean
        Git.open(project.rootDir).use { git ->
          describe = git.describe().apply {
            setAlways(true)
            setLong(false)
            setTags(true)
          }.call()
          isClean = git.status().call().isClean
        }
        project.version =
          describe
            .removePrefix("v")
            .plus(
              if (!isClean) {
                SimpleDateFormat("'-'yyyyMMdd'T'HHmmssXX").format(Date())
              } else {
                ""
              }
            )
      }.onFailure {
        println("Failed to detect version for ${project.name} based on git info")
        it.printStackTrace()
      }
    }
  }
}
