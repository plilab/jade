package org.ucombinator.jade.maven.googlecloudstorage

import org.eclipse.aether.repository.RemoteRepository

object HttpRepository {
  // https://maven-central.storage-download.googleapis.com/maven2/
  // https://maven-central-eu.storage-download.googleapis.com/maven2/
  // https://maven-central-asia.storage-download.googleapis.com/maven2
  const val URL = "https://maven-central-asia.storage-download.googleapis.com/maven2"

  fun getBuilder() = RemoteRepository.Builder("google-maven-central", "default", URL)
}
