package org.ucombinator.jade.maven.googlecloudstorage

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import java.io.File
import java.io.FileInputStream

object GcsBucket {
  const val MAVEN_BUCKET = "maven-central"
  const val ENV_VAR = "GOOGLE_APPLICATION_CREDENTIALS" // TODO: use environment
  const val URL = "https://www.googleapis.com/auth/cloud-platform"

  fun open(authFile: File? = null): Bucket {
    val storage =
      if (authFile !== null) {
        val credentials = GoogleCredentials.fromStream(FileInputStream(authFile)).createScoped(listOf(URL))
        StorageOptions.newBuilder().setCredentials(credentials).build().getService()
      } else {
        StorageOptions.getDefaultInstance().getService()
      }

    return storage.get(MAVEN_BUCKET)
  }
}
