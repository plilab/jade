package org.michaeldadams.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

// Create a "version" task for printing the project version
class VersionTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks.register("version") {
      description = "Displays the project version."
      group = "Help"
      doLast {
        getLogger().quiet(project.version.toString())
      }
    }
  }
}
