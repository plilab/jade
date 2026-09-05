package org.ucombinator.jade.test

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.*

class StringJavaFileObject(
    className: String,
    sourceCode: String
) : SimpleJavaFileObject(
    URI.create("string:///$className.java"),
    JavaFileObject.Kind.SOURCE
) {
    private val code = sourceCode

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return code
    }
}

class BytecodeJavaFileObject(
    className: String
) : SimpleJavaFileObject(
    URI.create("bytes:///$className.class"),
    JavaFileObject.Kind.CLASS
) {
    val output = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return output
    }
}

class InMemoryFileManager(
    fileManager: StandardJavaFileManager
) : ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {

    lateinit var bytecodeFile: BytecodeJavaFileObject

    override fun getJavaFileForOutput(
        location: JavaFileManager.Location,
        className: String,
        kind: JavaFileObject.Kind,
        sibling: FileObject?
    ): JavaFileObject {
        bytecodeFile = BytecodeJavaFileObject(className)
        return bytecodeFile
    }
}