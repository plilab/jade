# Developer Guide

This document details the design and implementation of Jade. For detailed documentations of submodules and classes with Kotlin KDoc, refer to {link to generated KDoc/ Dokka}.

### Decompilation

At a high level, Jade works as follows:
- First, Jade takes a path to a Java `.class` file as input.
- Then, raw Java bytecodes from the `.class` file are read using [ASM library](https://asm.ow2.io/), and accessed via ASM Tree API.
- Subsequently, Jade builds a Java abstract syntax tree with data structures provided by [Javaparser](https://javaparser.org/) library.
	- Sections belonging to class-level constructs (fields, method signatures etc.) are directly translated into Javaparser data structures.
	- Sections belonging to method bodies which have more complicated control flows are converted into 3 intermediate representations: control flow graph (CFG), Static-Single Assignment (SSA) form and CFG Domination Structure form. Jade utilizes these three intermediate representations to construct a Java abstract syntax tree.
	- Finally, the abstract syntax tree is converted into Java code and written to output stream.

The following figure illustrates Jade's decompilation workflow:

![Jade Architecture](assets/jade_architecture.png)

### Subdirectories
Jade's source source consists of the following subdirectories:

- `/analysis`: This directory contains intermediate representations that are used for decompiling control flows in Java code.
- `/asm`: This directory contains wrappers around data structures from the Javaparser library, which are used for representing the Abstract Syntax Tree (AST) of Java code.
- `/classfile`: This directory contains wrappers around data structures from the Javaparser library, which are used for representing the Abstract Syntax Tree (AST) of Java code.
- `/decompile`: This directory contains the logic and algorithms that are used to decompile various Java constructs, such as classes, methods, and bytecode.
- `/javaparser`: This directory contains wrappers around data structures from the Javaparser library, which are used for representing the abstract syntax tree (AST) of Java code.
- `/jgrapht`: This directory contains wrappers around the JGraphT library, which is used for working with graphs and graph-related data structures.
- `/main`: This directory contains the main entry point for Jade's various tools and commands, which are implemented using the Clikt library for command-line interfaces.
- `/maven`: This directory contains tools and utilities that are used to acquire and test against Maven bytecodes, which are a common format for distributing Java libraries and applications.
- `/util`: This directory contains miscellaneous utilities and helper functions that are used throughout the Jade project, such as logging, debugging, and internal data structures for convenience and efficiency.

### Parsing and Decompiling Class-level Constructs
(TODO: High-level implementation strategy & key design decisions)

### Parsing and Decompiling Method Bodies
(TODO: High-level implementation strategy & key design decisions)

### Computation of Control Flow Graph
(TODO: High-level implementation strategy & key design decisions)

### Computation of Domination Structure
(TODO: High-level implementation strategy & key design decisions)

### Computation of Static Single Assignment (SSA) Form
(TODO: High-level implementation strategy & key design decisions)

### Decompilation Flow
(TODO: High-level implementation strategy & key design decisions)

## Testing against Maven bytecodes
