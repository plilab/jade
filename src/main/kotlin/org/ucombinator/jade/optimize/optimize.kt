package org.ucombinator.jade.optimize

import javax.tools.ToolProvider
import java.io.StringWriter

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration

import java.io.File

object Optimize {

    fun main(input: File){
        val cu = optimizeOne(input)
        input.writeText(cu.toString())
    }

    fun optimizeOne(file: File) : CompilationUnit {

        val cu = StaticJavaParser.parse(file)

        val methods = cu.findAll(MethodDeclaration::class.java)

        if (methods.isEmpty()) {
            // There is no method body to optimize
            return cu
        }

        for (method in methods) {

            val body = method.body
            // Only optimize if the method has a valid body
            if (body.isPresent) {
                val originalBody = body.get()
                val optimizedBody = org.ucombinator.jade.decompile.OptimizeMethodBody.optimize(originalBody)
                method.setBody(optimizedBody)
            } 
        }

        return cu
    }
    
}