package org.ucombinator.jade.playground.util

import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.asm.Insn

fun ControlFlowGraph.toDot(): String {
    val dotGenerator = CFGDotGenerator()
    return dotGenerator.generateDot(this)
}

private class CFGDotGenerator {
    private var nodeCounter = 0
    private val nodeMap = mutableMapOf<Insn, String>()
    private val edges = mutableListOf<String>()
    private val nodes = mutableListOf<String>()
    
    fun generateDot(cfg: ControlFlowGraph): String {
        // Reset state
        nodeCounter = 0
        nodeMap.clear()
        edges.clear()
        nodes.clear()
        
        // Process all vertices (instructions) in the control flow graph
        for (insn in cfg.graph.vertexSet()) {
            processInstruction(insn)
        }
        
        // Process all edges in the control flow graph
        for (edge in cfg.graph.edgeSet()) {
            processEdge(edge)
        }
        
        // Build the DOT string
        val sb = StringBuilder()
        sb.appendLine("digraph CFG {")
        
        // Layout settings for control flow graphs
        sb.appendLine("  rankdir=TB;")  // Top to bottom layout
        sb.appendLine("  splines=polyline;")  // Use polyline for cleaner curves
        sb.appendLine("  overlap=false;")  // Prevent node overlap
        sb.appendLine("  concentrate=true;")  // Merge parallel edges
        
        // Node styling for CFG
        sb.appendLine("  node [")
        sb.appendLine("    fontname=\"Courier New\", fontsize=9, style=filled,")
        sb.appendLine("    margin=0.1, height=0.4")
        sb.appendLine("  ];")
        sb.appendLine("  edge [fontname=\"Arial\", fontsize=8, arrowsize=0.7];")
        
        // Add entry node highlighting
        val entryNodeId = getNodeId(cfg.entry)
        sb.appendLine("  $entryNodeId [style=\"filled,bold\", fillcolor=\"lightgreen\"];")
        
        // Add all nodes
        nodes.forEach { sb.appendLine("  $it") }
        
        // Add all edges
        edges.forEach { sb.appendLine("  $it") }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    private fun processInstruction(insn: Insn) {
        val nodeId = getNodeId(insn)
        val nodeLabel = getNodeLabel(insn)
        val nodeShape = getNodeShape(insn)
        val nodeColor = getNodeColor(insn)
        
        nodes.add("$nodeId [label=\"$nodeLabel\", shape=$nodeShape, fillcolor=\"$nodeColor\"];")
    }
    
    private fun processEdge(edge: ControlFlowGraph.Edge) {
        val sourceId = getNodeId(edge.source)
        val targetId = getNodeId(edge.target)
        edges.add("$sourceId -> $targetId;")
    }
    
    private fun getNodeId(insn: Insn): String {
        return nodeMap.getOrPut(insn) { "insn${nodeCounter++}" }
    }
    
    private fun getNodeLabel(insn: Insn): String {
        val index = insn.index()
        val insnText = insn.shortString()
        return "$index: $insnText".replace("\"", "\\\"")  // Escape quotes for DOT format
    }
    
    private fun getNodeShape(insn: Insn): String {
        // Determine shape based on instruction type
        val insnText = insn.shortString().uppercase()
        return when {
            insnText.contains("IF") || insnText.contains("GOTO") || insnText.contains("SWITCH") -> "diamond"
            insnText.contains("RETURN") -> "octagon"
            insnText.contains("INVOKE") -> "ellipse"
            insnText.contains("LABEL") -> "point"
            else -> "box"
        }
    }
    
    private fun getNodeColor(insn: Insn): String {
        // Color nodes based on instruction type for better visualization
        val insnText = insn.shortString().uppercase()
        return when {
            insnText.contains("IF") || insnText.contains("GOTO") -> "orange"
            insnText.contains("RETURN") -> "lightcoral"
            insnText.contains("INVOKE") -> "lightblue"
            insnText.contains("LOAD") || insnText.contains("STORE") -> "lightyellow"
            insnText.contains("LABEL") -> "white"
            insnText.contains("NEW") || insnText.contains("GETFIELD") || insnText.contains("PUTFIELD") -> "lightpink"
            else -> "lightgray"
        }
    }
}