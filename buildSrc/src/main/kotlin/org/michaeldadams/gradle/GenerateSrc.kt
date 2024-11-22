package org.michaeldadams.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

fun Project.generateSrc(fileName: String, code: String) {
  // TODO: supprt filenames with directories in them
  val generatedSrcDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().getAsFile()
  generatedSrcDir.mkdirs()
  val sourceSets = project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets
  sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).kotlin.srcDir(generatedSrcDir)
  val file = File(generatedSrcDir, fileName)
  val outdated = try { code != file.readText() } catch (_: Throwable) { true }
  if (outdated) { file.writeText(code) }
}
