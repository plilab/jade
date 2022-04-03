package org.ucombinator.jade.maven

// import org.apache.maven.resolver.examples.util.Booter
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.apache.maven.resolver.examples.util.ConsoleDependencyGraphDumper
// import org.slf4j.Logger
// import org.slf4j.LoggerFactory

object MavenAuto {

  fun main() {
    val locator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

    // locator.setErrorHandler( object : DefaultServiceLocator.ErrorHandler() {
    //   override fun serviceCreationFailed(type: Class<*>, impl: Class<*>, exception: Throwable) {
    //     TODO()
    //   }
    // })

    val system = locator.getService(RepositorySystem::class.java)
    val session = MavenRepositorySystemUtils.newSession()
    val localRepo = LocalRepository("../poms-with-parents/maven2")
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))

    val artifact = DefaultArtifact("org.apache.maven.resolver:maven-resolver-impl:1.7.2")

    val remote = RemoteRepository
      .Builder(null, "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
      .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
      .build()

    val descriptorRequest = ArtifactDescriptorRequest()
    descriptorRequest.setArtifact(artifact)
    descriptorRequest.setRepositories(listOf(remote))
    val descriptorResult = system.readArtifactDescriptor(session, descriptorRequest)

    val collectRequest = CollectRequest()
    // collectRequest.setRoot(Dependency(artifact, ""))
    collectRequest.setRootArtifact(descriptorResult.artifact)
    collectRequest.setDependencies(descriptorResult.dependencies)
    collectRequest.setManagedDependencies(descriptorResult.managedDependencies)
    collectRequest.setRepositories(listOf(remote))
    val collectResult = system.collectDependencies(session, collectRequest)
    // collectResult.getRoot().accept(ConsoleDependencyGraphDumper())

    val visitor = PreorderNodeListGenerator()
    collectResult.getRoot().accept(visitor)
    for (dependency in visitor.getDependencies(true)) {
      System.out.println(dependency)
    }
  }
}
