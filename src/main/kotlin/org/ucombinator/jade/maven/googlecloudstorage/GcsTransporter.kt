package org.ucombinator.jade.maven.googlecloudstorage

import org.eclipse.aether.spi.connector.transport.AbstractTransporter
import org.eclipse.aether.spi.connector.transport.TransportTask
import org.eclipse.aether.spi.connector.transport.GetTask
import org.eclipse.aether.spi.connector.transport.PeekTask
import org.eclipse.aether.spi.connector.transport.PutTask
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Blob
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import kotlinx.coroutines.*

class GcsTransporter : AbstractTransporter() {
  val bucket = GcsBucket.open() // TODO: authFile
  val resourceBase = "maven2/" // TODO: make configurable

  override fun classify(error: Throwable): Int =
    if (error is ResourceNotFoundException) ERROR_NOT_FOUND
    else ERROR_OTHER;

  private fun getBlob(task: TransportTask): Blob {
    val resource = resourceBase + task.location.path
    val blob = bucket.get(resource, Storage.BlobGetOption.fields(Storage.BlobField.SIZE))
    if (blob === null || !blob.exists())
      throw ResourceNotFoundException("Could not locate $resource")
    return blob
  }

  protected override fun implPeek(task: PeekTask) { getBlob(task) }

  protected override fun implGet(task: GetTask) {
    val blob = getBlob(task)
    val outputStream = ByteArrayOutputStream()
    blob.downloadTo(outputStream)
    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
    utilGet(task, inputStream, true, blob.size, false)
  }

  protected override fun implPut(task: PutTask) =
    // We do not implement this as we do not use it, and thus would not be testing it
    throw UnsupportedOperationException("Uploading to Google Cloud Services is not implemented")

  protected override fun implClose() { /* Do nothing */ }
}
