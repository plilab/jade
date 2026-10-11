package org.ucombinator.jade.test

import java.io.File
import java.io.StringWriter
import javax.tools.ToolProvider


import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration


import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OptimizationTest {

    fun optimizeAndCompare(input:File, expected: File): Boolean {
        val cu = org.ucombinator.jade.optimize.Optimize.optimizeOne(input)
        val expectedCu = StaticJavaParser.parse(expected)
        return (cu == expectedCu)
    }

    @Test
    fun optTest() {
        val directory = File("./src/test/resources/optimization")
        // Each testcase is in a seperate directory
        val subDirs = directory
            .walkTopDown()
            .filter {it.isDirectory && it != directory}
            .toList()
        
        subDirs.forEach { subDir ->
            val inputFile = subDir
                .walkTopDown()
                .filter {
                    it.isFile &&
                    it.extension == "java" &&
                    !it.nameWithoutExtension.endsWith("Expected")
                }
                .toList().single()
            val expectedFile = subDir
                .walkTopDown()
                .filter {
                    it.isFile &&
                    it.extension == "java" &&
                    it.nameWithoutExtension.endsWith("Expected")
                }
                .toList().single()
            assertTrue(optimizeAndCompare(inputFile, expectedFile))
        }
    }

}
