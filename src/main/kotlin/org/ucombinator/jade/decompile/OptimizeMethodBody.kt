package org.ucombinator.jade.decompile

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.LabeledStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.ContinueStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.body.VariableDeclarator
import org.ucombinator.jade.util.Log

/**
 * Handles optimization and cleanup of decompiled method bodies to make them more readable
 * and closer to the original Java source code.
 */
object OptimizeMethodBody {
    private val log = Log {}

    /**
     * Main entry point for optimizing a decompiled method body.
     * Applies a series of optimization passes to improve readability and structure.
     *
     * @param methodBody The decompiled method body to optimize
     * @return The optimized method body
     */
    fun optimize(methodBody: BlockStmt): BlockStmt {
        log.debug { "Starting method body optimization..." }
        
        var optimizedBody = methodBody.clone()
        
        // Apply optimization passes in order
        optimizedBody = deleteEmptyStatements(optimizedBody)
        optimizedBody = simplifyVariableNames(optimizedBody)
        optimizedBody = removeUnusedVariables(optimizedBody)
        optimizedBody = simplifyControlFlowAndFlatten(optimizedBody)
        optimizedBody = reconstructHighLevelConstructs(optimizedBody)
        optimizedBody = cleanupPhiVariables(optimizedBody)
        optimizedBody = removeEmptyStatements(optimizedBody)
        
        log.debug { "Method body optimization completed." }
        return optimizedBody
    }

    /**
     * Removes redundant comments from the method body.
     * 
     * @param body The method body to process
     * @return The method body with redundant comments removed
     */
    private fun deleteEmptyStatements(body: BlockStmt): BlockStmt {
        log.debug { "Applying deleteEmptyStatements optimization..." }

        body.walk { node ->
            if (node is EmptyStmt) {
                node.remove()
            }
        }
        return body
    }

    /**
     * Simplifies variable names by replacing SSA-style names with more natural ones.
     * 
     * @param body The method body to process
     * @return The method body with simplified variable names
     */
    private fun simplifyVariableNames(body: BlockStmt): BlockStmt {
        log.debug { "Applying simplifyVariableNames optimization..." }
        // TODO: Implement variable name simplification
        // - Replace insnVar1, insnVar2, etc. with meaningful names like temp1, temp2
        // - Replace copyVar1_1, copyVar2_3 with simpler names
        // - Try to infer better names based on usage context
        return body
    }

    /**
     * Removes unused variable declarations and assignments.
     * 
     * @param body The method body to process
     * @return The method body with unused variables removed
     */
    private fun removeUnusedVariables(body: BlockStmt): BlockStmt {
        log.debug { "Applying removeUnusedVariables optimization..." }
        // TODO: Implement dead code elimination
        // - Identify variables that are declared but never used
        // - Remove assignments to variables that are never read
        // - Be careful not to remove variables with side effects
        return body
    }

    /**
     * Unified function that both removes unused labels and flattens blocks in a single pass.
     * This approach is more efficient and avoids issues that arise from doing these operations separately.
     * 
     * @param body The method body to process
     * @return The method body with simplified control flow and flattened blocks
     */
    private fun simplifyControlFlowAndFlatten(body: BlockStmt): BlockStmt {
        log.debug { "Applying simplifyControlFlowAndFlatten optimization..." }
        
        val optimizedBody = body.clone()
        
        // First pass: collect all label references (break/continue statements)
        val referencedLabels = collectReferencedLabels(optimizedBody)
        
        // Second pass: recursively flatten and remove unused labels
        flattenAndRemoveUnusedLabels(optimizedBody, referencedLabels)
        
        return optimizedBody
    }
    
    /**
     * Recursively flattens blocks and removes unused labeled statements in a single pass.
     * When a LabeledStmt -> BlockStmt structure is found and the label is unused,
     * it replaces the LabeledStmt directly with the children of the BlockStmt.
     */
    private fun flattenAndRemoveUnusedLabels(body: BlockStmt, referencedLabels: Set<String>) {
        var changed = true
        
        while (changed) {
            changed = false
            val newStatements = mutableListOf<Statement>()
            
            for (statement in body.statements) {
                val processedStatements = processStatementForFlatteningAndLabelRemoval(statement, referencedLabels)
                if (processedStatements != listOf(statement)) {
                    changed = true
                }
                newStatements.addAll(processedStatements)
            }
            
            if (changed) {
                body.statements.clear()
                body.statements.addAll(newStatements)
            }
        }
        
        // Recursively process nested structures
        body.walk { node ->
            if (node is BlockStmt && node != body) {
                flattenAndRemoveUnusedLabels(node, referencedLabels)
            }
        }
    }
    
    /**
     * Processes a single statement for flattening and label removal.
     * Returns a list of statements that should replace the input statement.
     */
    private fun processStatementForFlatteningAndLabelRemoval(
        statement: Statement, 
        referencedLabels: Set<String>
    ): List<Statement> {
        return when (statement) {
            is LabeledStmt -> {
                val labelName = statement.label.identifier
                val innerStatement = statement.statement
                
                // Check if this is an unused JADE_ label
                val isUnusedJadeLabel = labelName.startsWith("JADE_") && !referencedLabels.contains(labelName)
                
                when {
                    isUnusedJadeLabel && innerStatement is BlockStmt -> {
                        // Replace LabeledStmt -> BlockStmt with the children of BlockStmt
                        val childStatements = mutableListOf<Statement>()
                        for (childStatement in innerStatement.statements) {
                            childStatements.addAll(
                                processStatementForFlatteningAndLabelRemoval(childStatement, referencedLabels)
                            )
                        }
                        childStatements
                    }
                    isUnusedJadeLabel -> {
                        // Replace unused label with just its inner statement
                        processStatementForFlatteningAndLabelRemoval(innerStatement, referencedLabels)
                    }
                    innerStatement is BlockStmt -> {
                        // Keep the label but flatten the block if it contains multiple statements
                        val childStatements = mutableListOf<Statement>()
                        for (childStatement in innerStatement.statements) {
                            childStatements.addAll(
                                processStatementForFlatteningAndLabelRemoval(childStatement, referencedLabels)
                            )
                        }
                        
                        when {
                            childStatements.isEmpty() -> listOf(statement) // Keep as-is if empty
                            childStatements.size == 1 -> {
                                // Label the single child statement
                                listOf(LabeledStmt(statement.label, childStatements[0]))
                            }
                            else -> {
                                // Label the first statement, keep others unlabeled
                                val result = mutableListOf<Statement>()
                                result.add(LabeledStmt(statement.label, childStatements[0]))
                                result.addAll(childStatements.drop(1))
                                result
                            }
                        }
                    }
                    else -> {
                        // Keep labeled statement as-is
                        listOf(statement)
                    }
                }
            }
            is BlockStmt -> {
                // Flatten simple blocks (non-control-flow blocks)
                val childStatements = mutableListOf<Statement>()
                for (childStatement in statement.statements) {
                    childStatements.addAll(
                        processStatementForFlatteningAndLabelRemoval(childStatement, referencedLabels)
                    )
                }
                childStatements
            }
            // Don't flatten control flow statements - they need their block structure
            is com.github.javaparser.ast.stmt.WhileStmt,
            is com.github.javaparser.ast.stmt.ForStmt,
            is com.github.javaparser.ast.stmt.IfStmt,
            is com.github.javaparser.ast.stmt.DoStmt -> {
                listOf(statement)
            }
            else -> {
                // All other statements are kept as-is
                listOf(statement)
            }
        }
    }

    /**
     * Collects all labels that are referenced by break or continue statements.
     */
    private fun collectReferencedLabels(body: BlockStmt): Set<String> {
        val referencedLabels = mutableSetOf<String>()
        
        body.walk { node ->
            when (node) {
                is BreakStmt -> {
                    node.label.ifPresent { label ->
                        referencedLabels.add(label.identifier)
                    }
                }
                is ContinueStmt -> {
                    node.label.ifPresent { label ->
                        referencedLabels.add(label.identifier)
                    }
                }
            }
        }
        
        return referencedLabels
    }

    /**
     * Reconstructs high-level constructs (for, while, if-else) from low-level control flow.
     * 
     * @param body The method body to process
     * @return The method body with reconstructed high-level constructs
     */
    private fun reconstructHighLevelConstructs(body: BlockStmt): BlockStmt {
        log.debug { "Applying reconstructHighLevelConstructs optimization..." }
        // TODO: Implement high-level construct reconstruction
        // - Convert labeled breaks/continues to proper for/while loops
        // - Reconstruct if-else chains from conditional jumps
        // - Identify and reconstruct switch statements
        return body
    }

    /**
     * Cleans up phi variables and merges them where possible.
     * 
     * @param body The method body to process
     * @return The method body with cleaned phi variables
     */
    private fun cleanupPhiVariables(body: BlockStmt): BlockStmt {
        log.debug { "Applying cleanupPhiVariables optimization..." }
        // TODO: Implement phi variable cleanup
        // - Merge phi variables that represent the same logical variable
        // - Remove phi assignments where all inputs are the same
        // - Simplify phi variable usage patterns
        return body
    }

    /**
     * Removes empty statements and no-op operations.
     * 
     * @param body The method body to process
     * @return The method body with empty statements removed
     */
    private fun removeEmptyStatements(body: BlockStmt): BlockStmt {
        log.debug { "Applying removeEmptyStatements optimization..." }
        // TODO: Implement empty statement removal
        // - Remove empty statements (just semicolons)
        // - Remove no-op comments that don't add value
        // - Clean up whitespace and formatting
        return body
    }
} 