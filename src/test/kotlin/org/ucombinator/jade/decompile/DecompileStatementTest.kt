package org.ucombinator.jade.decompile

import com.github.javaparser.ast.stmt.WhileStmt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.ucombinator.jade.compile.Compile
import org.ucombinator.jade.util.JavaParser

class DecompileStatementTest {
    @Test
    fun `test while loop`() {
        val source = """
            class Test {
                void test() {
                    while (true) {
                        int x = 1;
                    }
                }
            }
        """.trimIndent()
        val-class-files = Compile.compile(mapOf("Test.java" to source))
        val classNode = ClassNode()
        val classReader = ClassReader(classFiles["Test.class"])
        classReader.accept(classNode, 0)

        val compilationUnit = DecompileClass.decompileClass(classNode)
        val methods = JavaParser.findDeclarations(compilationUnit, WhileStmt::class.java)
        assertEquals(1, methods.size)
        val whileStmt = methods[0]
        assertEquals("while (true)", whileStmt.condition.toString())
    }
} 