package org.ucombinator.jade.decompile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.type.Type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.util.Log

/**
 * Decompiles JVM `invokespecial` instructions.
 *
 * `invokespecial` invokes instance initialization methods and selected methods of the current
 * class or its supertypes.
 * See https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-6.html#jvms-6.5.invokespecial.
 */
object DecompileInvokeSpecial {
  private val log = Log {}

  /**
   * Decompiles [node] using the current class context in [classNode].
   */
  fun decompile(
    node: AbstractInsnNode,
    classNode: ClassNode,
    argumentVariables: List<Var>,
    argumentExpressions: Array<Expression>,
  ): DecompiledInsn {
    assert(node.opcode == Opcodes.INVOKESPECIAL)

    val insn = node as MethodInsnNode

    // <init> is a special name and cannot be used directly since it's not a valid identifier.
    if (insn.name == "<init>") {
      return decompileInitMethod(insn, classNode, argumentVariables, argumentExpressions)
    }

    // Otherwise, this is a method call.
    val (head, tail) = splitArgumentExpressions(insn, argumentExpressions)
    val target = methodCallTarget(insn, classNode, head)
    val typeArguments = NodeList<Type>()
    return DecompiledInsn.Expression(MethodCallExpr(target, typeArguments, insn.name, tail))
  }

  /**
   * Decompiles a method with the special name `<init>`.
   *
   * The method must be either of the following:
   * 1. An explicit constructor invocation, such as `this(...)` or `super(...)`.
   * 2. An object creation expression that initializes a value produced by `new`.
   */
  private fun decompileInitMethod(
    insn: MethodInsnNode,
    classNode: ClassNode,
    argumentVariables: List<Var>,
    argumentExpressions: Array<Expression>,
  ): DecompiledInsn {
    assert(insn.name == "<init>")

    val receiver = argumentVariables.firstOrNull()
    val (head, tail) = splitArgumentExpressions(insn, argumentExpressions)

    // Calling `this(...)` or `super(...)`
    if (isCurrentInstance(receiver)) {
      return when (insn.owner) {
        classNode.name -> DecompiledInsn.Statement(
          ExplicitConstructorInvocationStmt(true, null, tail),
        )
        classNode.superName -> DecompiledInsn.Statement(
          ExplicitConstructorInvocationStmt(false, null, tail),
        )
        else -> {
          log.warn {
            """
            Constructor invocation target should either be the current class or its superclass.
            Got owner=${insn.owner}, class=${classNode.name}, superclass=${classNode.superName}.
            """.trimIndent()
          }
          DecompiledInsn.Unsupported(insn)
        }
      }
    }

    // Construct a new object and assign it (i.e. `foo = new Foo()`).
    val allocationOwner = newAllocationOwner(receiver)
    if (allocationOwner != null) {
      return when (allocationOwner) {
        insn.owner -> DecompiledInsn.Expression(
          AssignExpr(
            head,
            ObjectCreationExpr(null, ClassName.classNameType(allocationOwner), tail),
            AssignExpr.Operator.ASSIGN,
          ),
        )
        else -> {
          log.warn {
            """
            Constructor owner should match allocation owner
            Got owner=${insn.owner} allocationOwner=${allocationOwner}.
            """.trimIndent()
          }
          DecompiledInsn.Unsupported(insn)
        }
      }
    }

    log.warn {
      """
      Method with the special name <init> must be either:
      1. Explicit constructor invocation (i.e. this(...) and super(...))
      2. Creating a new object and assigning it
      """.trimIndent()
    }
    return DecompiledInsn.Unsupported(insn)
  }

  private fun isCurrentInstance(variable: Var?): Boolean =
    when (variable) {
      is Var.Parameter -> variable.isThis
      is Var.Copy -> isCurrentInstance(variable.source)
      else -> false
    }

  private fun newAllocationOwner(variable: Var?): String? =
    when (variable) {
      is Var.Instruction ->
        (variable.insn.insn as? TypeInsnNode)
          ?.takeIf { it.opcode == Opcodes.NEW }
          ?.desc
      is Var.Copy -> newAllocationOwner(variable.source)
      else -> null
    }

  private fun splitArgumentExpressions(
    insn: MethodInsnNode,
    argumentExpressions: Array<Expression>,
  ): Pair<Expression, NodeList<Expression>> {
    val (argumentTypes, _) = Descriptor.methodDescriptor(insn.desc)
    return Pair(
      argumentExpressions[0],
      NodeList(argumentTypes.indices.map { argumentExpressions[it + 1] }),
    )
  }

  /**
   * Find the target of the method call.
   *
   * The method can either belong to the class, its superclass, or an interface.
   * The class's interfaces need to be checked because it's possible to invoke an interface method
   * with the `super` keyword (i.e. `FooInterface.super.fooMethod()`), even though most interface
   * method invocations should be handled under `invokeinterface`.
   */
  private fun methodCallTarget(
    insn: MethodInsnNode,
    classNode: ClassNode,
    head: Expression,
  ): Expression =
    when {
      insn.itf && insn.owner in classNode.interfaces -> SuperExpr(ClassName.className(insn.owner))
      insn.owner == classNode.superName -> SuperExpr()
      else -> head
    }
}
