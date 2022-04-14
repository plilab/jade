package org.ucombinator.jade.maven.googlecloudstorage

import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.spi.connector.transport.Transporter
import org.eclipse.aether.spi.connector.transport.TransporterFactory

class GcsTransporterFactory : TransporterFactory {
  override fun newInstance(var1: RepositorySystemSession, var2: RemoteRepository): Transporter = GcsTransporter()

  override fun getPriority(): Float = 10.0F
}
