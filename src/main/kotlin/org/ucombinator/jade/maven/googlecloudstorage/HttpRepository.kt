package org.ucombinator.jade.maven.googlecloudstorage

import org.eclipse.aether.repository.RemoteRepository

/** TODO:doc. */
object HttpRepository {
  // https://maven-central.storage-download.googleapis.com/maven2/
  // https://maven-central-eu.storage-download.googleapis.com/maven2/
  // https://maven-central-asia.storage-download.googleapis.com/maven2

  /** TODO:doc. */
  const val URL = "https://maven-central-asia.storage-download.googleapis.com/maven2"

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun getBuilder(): RemoteRepository.Builder = RemoteRepository.Builder("google-maven-central", "default", URL)
}
