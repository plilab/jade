package org.ucombinator.jade.test

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.*

/* This file constains a simple in-memory file management system to satisfy the requirement of compiler task
 */


// Virtual file for input java files
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

// Virtual file for output bytecode files
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

// Virtual file manager
// The function getJavaFileForOutput is in charge of the format of output file
// So it is overriden to prevent saving file to the disk
class InMemoryFileManager(
    fileManager: StandardJavaFileManager
) : ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {

    val bytecodeFiles = HashMap<String, BytecodeJavaFileObject>()

    override fun getJavaFileForOutput(
        // TODO: 
        // 1. Integrate module location after Jade is intergrated with module
        // 2. Integrate path location
        // is it trying to read from the class path/local directory... module management
        location: JavaFileManager.Location,
        className: String,
        kind: JavaFileObject.Kind,
        // Can be safely ignored due to in-memory implementation
        sibling: FileObject?
    ): JavaFileObject {
        
        // Verify the file kind is class file
        require(kind == JavaFileObject.Kind.CLASS)
        val bytecodeFile = BytecodeJavaFileObject(className)
        val key = location.name + className
        bytecodeFiles[key] = bytecodeFile
        //bytecodeFiles[className] = bytecodeFile
        return bytecodeFile
    }
}