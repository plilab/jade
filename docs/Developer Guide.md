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

- `/analysis`: Contains intermediate representations for decompiling control flows.
- `/asm`: Contains wrappers around ASM library used for reading bytecodes.
- `/classfile`: Contains data structures for representing class-level constructs.
- `/decompile`: Contains logic to decompile various Java constructs.
- `/javaparser`: Contains wrappers around data structures from Javaparser library for abstract syntax tree of Java code.
- `/jgrapht`: Contains wrappers around JGraphT library for working with graphs.
- `/main`: Contains Jade Clikt commands which serves as command line interface entrypoint for Jade's various tools.
- `/maven`: Contains tools used to acquire and test against Maven bytecodes.
- `/util`: Contains miscellaneous utilities such as logging, debugging and internal data structures for convenience.

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
