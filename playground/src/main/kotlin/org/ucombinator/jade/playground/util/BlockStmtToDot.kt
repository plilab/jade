package org.ucombinator.jade.playground.util

import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.type.*

fun BlockStmt.toDot(): String {
    val dotGenerator = DotGenerator()
    return dotGenerator.generateDot(this)
}

private class DotGenerator {
    private var nodeCounter = 0
    private val nodeMap = mutableMapOf<Node, String>()
    private val edges = mutableListOf<String>()
    private val nodes = mutableListOf<String>()
    private val blockStmtRanks = mutableMapOf<String, MutableList<String>>()
    
    fun generateDot(blockStmt: BlockStmt): String {
        // Reset state
        nodeCounter = 0
        nodeMap.clear()
        edges.clear()
        nodes.clear()
        blockStmtRanks.clear()
        
        // Generate the graph
        processNode(blockStmt, null, null)
        
        // Build the DOT string with tree-like layout
        val sb = StringBuilder()
        sb.appendLine("digraph AST {")
        
        // Natural tree layout settings with better spacing
        sb.appendLine("  rankdir=TB;")  // Top to bottom layout
        sb.appendLine("  splines=polyline;")  // Use polyline for cleaner curves
        sb.appendLine("  overlap=false;")  // Prevent node overlap
        
        // Tree-like node styling 
        sb.appendLine("  node [")
        sb.appendLine("    fontname=\"Arial\", fontsize=10, style=filled,")
        sb.appendLine("    margin=0.15, height=0.5, width=1.0")
        sb.appendLine("  ];")
        sb.appendLine("  edge [fontname=\"Arial\", fontsize=8, arrowsize=0.8];")
        
        // Remove strict rank constraints - let GraphViz handle layout naturally
        
        // Add all nodes
        nodes.forEach { sb.appendLine("  $it") }
        
        // Add ranking constraints for BlockStmt children to ensure left-to-right ordering
        blockStmtRanks.forEach { (blockId, childIds) ->
            if (childIds.size > 1) {
                sb.appendLine("  { rank=same; ${childIds.joinToString("; ")}; }")
                // Add invisible edges to enforce left-to-right ordering
                for (i in 0 until childIds.size - 1) {
                    sb.appendLine("  ${childIds[i]} -> ${childIds[i + 1]} [style=invis];")
                }
            }
        }
        
        // Add all edges with proper ports for tree structure
        edges.forEach { sb.appendLine("  $it") }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun shouldFilterNode(node: Node): Boolean {
        return when (node) {
            is Type -> true  // Filter out all type nodes (PrimitiveType, ClassOrInterfaceType, etc.)
            else -> false
        }
    }
    
    private fun processNode(node: Node, parentId: String?, edgeLabel: String?): String {
        val nodeId = getNodeId(node)
        val nodeLabel = getNodeLabel(node)
        val nodeShape = getNodeShape(node)
        
        // Create node definition with color
        val nodeColor = getNodeColor(node)
        nodes.add("$nodeId [label=\"$nodeLabel\", shape=$nodeShape, fillcolor=\"$nodeColor\"];")
        
        // Create edge from parent if exists 
        if (parentId != null && edgeLabel != null) {
            val edgeLabelStr = if (edgeLabel.isNotEmpty()) " [label=\"$edgeLabel\"]" else ""
            // Use natural edge layout without strict ports
            edges.add("$parentId -> $nodeId$edgeLabelStr;")
        }
        
        // Process children based on node type
        when (node) {
            is BlockStmt -> {
                val childIds = mutableListOf<String>()
                node.statements.forEachIndexed { index, stmt ->
                    val childId = processNode(stmt, nodeId, "stmt$index")
                    childIds.add(childId)
                }
                if (childIds.isNotEmpty()) {
                    blockStmtRanks[nodeId] = childIds
                }
            }
            is ExpressionStmt -> {
                node.expression?.let { expr ->
                    processNode(expr, nodeId, "")
                }
            }
            is VariableDeclarationExpr -> {
                node.variables.forEachIndexed { index, variable ->
                    processNode(variable, nodeId, "var$index")
                }
            }
            is VariableDeclarator -> {
                // Only process initializer, skip type information
                node.initializer.ifPresent { init ->
                    processNode(init, nodeId, "init")
                }
            }
            is AssignExpr -> {
                processNode(node.target, nodeId, "target")
                processNode(node.value, nodeId, "value")
            }
            is BinaryExpr -> {
                processNode(node.left, nodeId, "left")
                processNode(node.right, nodeId, "right")
            }
            is MethodCallExpr -> {
                node.scope.ifPresent { scope ->
                    processNode(scope, nodeId, "scope")
                }
                node.arguments.forEachIndexed { index, arg ->
                    processNode(arg, nodeId, "arg$index")
                }
            }
            is FieldAccessExpr -> {
                processNode(node.scope, nodeId, "scope")
            }
            is IfStmt -> {
                processNode(node.condition, nodeId, "condition")
                processNode(node.thenStmt, nodeId, "then")
                node.elseStmt.ifPresent { elseStmt ->
                    processNode(elseStmt, nodeId, "else")
                }
            }
            is LabeledStmt -> {
                processNode(node.statement, nodeId, "statement")
            }
            is BreakStmt -> {
                // Leaf node - no children to process
            }
            is ContinueStmt -> {
                // Leaf node - no children to process  
            }
            is ReturnStmt -> {
                node.expression.ifPresent { expr ->
                    processNode(expr, nodeId, "value")
                }
            }
            is EmptyStmt -> {
                // Leaf node - no children to process
            }
            // Add more node types as needed
            else -> {
                // Generic handling for other node types - filter out type nodes
                node.childNodes.filterNot { shouldFilterNode(it) }.forEachIndexed { index, child ->
                    processNode(child, nodeId, "child$index")
                }
            }
        }
        
        return nodeId
    }
    
    private fun getNodeId(node: Node): String {
        return nodeMap.getOrPut(node) { "node${nodeCounter++}" }
    }
    
    private fun getNodeLabel(node: Node): String {
        return when (node) {
            is BlockStmt -> "BlockStmt"
            is ExpressionStmt -> "ExpressionStmt"
            is VariableDeclarationExpr -> "VarDecl"
            is VariableDeclarator -> "${node.type} ${node.nameAsString}"
            is AssignExpr -> "Assign: ${node.operator}"
            is BinaryExpr -> "BinaryExpr: ${node.operator}"
            is NameExpr -> "Name: ${node.nameAsString}"
            is LiteralExpr -> "Literal: ${node.toString()}"
            is MethodCallExpr -> "MethodCall: ${node.nameAsString}"
            is FieldAccessExpr -> "FieldAccess: ${node.nameAsString}"
            is IfStmt -> "IfStmt"
            is LabeledStmt -> "LabeledStmt: ${node.label}"
            is BreakStmt -> node.label.map { "break ${it.asString()}" }.orElse("break")
            is ContinueStmt -> node.label.map { "continue ${it.asString()}" }.orElse("continue")
            is ReturnStmt -> "ReturnStmt"
            is EmptyStmt -> "EmptyStmt"
            else -> node.javaClass.simpleName
        }.replace("\"", "\\\"") // Escape quotes for DOT format
    }
    
    private fun getNodeShape(node: Node): String {
        return when (node) {
            is BlockStmt -> "rectangle"
            is LabeledStmt -> "rectangle"
            is ExpressionStmt -> "ellipse"
            is VariableDeclarationExpr -> "ellipse"
            is VariableDeclarator -> "ellipse"
            is AssignExpr -> "ellipse"
            is BinaryExpr -> "ellipse"
            is NameExpr -> "ellipse"
            is LiteralExpr -> "ellipse"
            is MethodCallExpr -> "ellipse"
            is FieldAccessExpr -> "ellipse"
            is IfStmt -> "diamond"
            is BreakStmt -> "octagon"
            is ContinueStmt -> "octagon"
            is ReturnStmt -> "octagon"
            is EmptyStmt -> "point"
            else -> "box"
        }
    }
    
    private fun getNodeColor(node: Node): String {
        return when (node) {
            is BlockStmt -> "lightblue"
            is LabeledStmt -> "lightcoral"
            is ExpressionStmt -> "lightgreen"
            is VariableDeclarationExpr -> "lightyellow"
            is VariableDeclarator -> "wheat"
            is AssignExpr -> "lightpink"
            is BinaryExpr -> "lightcyan"
            is NameExpr -> "palegreen"
            is LiteralExpr -> "lavender"
            is MethodCallExpr -> "lightsteelblue"
            is FieldAccessExpr -> "peachpuff"
            is IfStmt -> "orange"
            is BreakStmt -> "tomato"
            is ContinueStmt -> "tomato"
            is ReturnStmt -> "lightgray"
            is EmptyStmt -> "white"
            else -> "lightgray"
        }
    }
}