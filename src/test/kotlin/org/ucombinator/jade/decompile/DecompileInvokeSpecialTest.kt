package org.ucombinator.jade.decompile

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.ThisExpr
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.asm.Insn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DecompileInvokeSpecialTest {
  /*
   * class Animal {
   *   Animal() {}
   * }
   */
  @Test
  fun classWithoutExplicitSuperclassCallsObjectConstructor() {
    assertStatement(
      "super();",
      decompileSpecial(
        className = "example/Animal",
        superName = "java/lang/Object",
        owner = "java/lang/Object",
        name = "<init>",
      ),
    )
  }

  /*
   * class Animal {
   *   Animal(String name) {}
   * }
   *
   * class Dog extends Animal {
   *   Dog(String name) {
   *     super(name);
   *   }
   * }
   */
  @Test
  fun superclassConstructorCallPreservesArguments() {
    assertStatement(
      "super(name);",
      decompileSpecial(
        className = "example/Dog",
        superName = "example/Animal",
        owner = "example/Animal",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        arguments = listOf(argument(NameExpr("name"), local = 1)),
      ),
    )
  }

  /*
   * class Dog extends Animal {
   *   Dog(String name, boolean useName) {
   *     this(name);
   *   }
   *
   *   Dog(String name) {
   *     super(name);
   *   }
   * }
   */
  @Test
  fun sameClassConstructorCallUsesThis() {
    assertStatement(
      "this(name);",
      decompileSpecial(
        className = "example/Dog",
        superName = "example/Animal",
        owner = "example/Dog",
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        arguments = listOf(argument(NameExpr("name"), local = 1)),
      ),
    )
  }

  /*
   * class Animal {
   *   private void makeSound() {}
   *
   *   void call() {
   *     this.makeSound();
   *   }
   * }
   */
  @Test
  fun privateSpecialMethodRemainsAnInstanceCall() {
    assertExpression(
      "this.makeSound()",
      decompileSpecial(
        className = "example/Animal",
        superName = "java/lang/Object",
        owner = "example/Example",
        name = "makeSound",
      ),
    )
  }

  /*
   * class Main {
   *   static void main(String[] args) {
   *     Dog dog = new Dog("Alice");
   *   }
   * }
   */
  @Test
  fun objectConstructorBecomesObjectCreation() {
    val owner = "example/Dog"

    assertExpression(
      "insnVar0 = new example.Dog(\"Alice\")",
      decompileSpecial(
        className = "example/Main",
        superName = "java/lang/Object",
        owner = owner,
        name = "<init>",
        descriptor = "(Ljava/lang/String;)V",
        receiver = newObject(owner),
        arguments = listOf(argument(StringLiteralExpr("Alice"), local = 0)),
      ),
    )
  }

  /*
   * class Dog extends Animal {
   *   void makeSound() {
   *     super.makeSound();
   *   }
   * }
   */
  @Test
  fun superclassMethodUsesSuperScope() {
    assertExpression(
      "super.makeSound()",
      decompileSpecial(
        className = "example/Dog",
        superName = "example/Animal",
        owner = "example/Animal",
        name = "makeSound",
      ),
    )
  }

  /*
   * interface Animal {
   *   default void makeSound() {}
   * }
   *
   * class Dog implements Animal {
   *   public void makeSound() {
   *     Animal.super.makeSound();
   *   }
   * }
   */
  @Test
  fun defaultInterfaceMethodUsesQualifiedSuperScope() {
    assertExpression(
      "example.Animal.super.makeSound()",
      decompileSpecial(
        className = "example/Dog",
        superName = "java/lang/Object",
        interfaces = listOf("example/Animal"),
        owner = "example/Animal",
        name = "makeSound",
        isInterface = true,
      ),
    )
  }

  @Test
  fun currentInstanceConstructorCallWithUnexpectedOwnerIsUnsupported() {
    assertIs<DecompiledInsn.Unsupported>(
      decompileSpecial(
        className = "example/Dog",
        superName = "example/Animal",
        owner = "example/Unrelated",
        name = "<init>",
      ),
    )
  }

  @Test
  fun objectConstructorMustMatchAllocationOwner() {
    assertIs<DecompiledInsn.Unsupported>(
      decompileSpecial(
        className = "example/ConstructorInit",
        superName = "java/lang/Object",
        owner = "example/Cat",
        name = "<init>",
        receiver = newObject("example/Dog"),
      ),
    )
  }

  private fun decompileSpecial(
    className: String,
    superName: String?,
    interfaces: List<String> = emptyList(),
    owner: String,
    name: String,
    descriptor: String = "()V",
    receiver: Operand = currentInstance(className),
    arguments: List<Operand> = emptyList(),
    isInterface: Boolean = false,
  ): DecompiledInsn {
    val node = MethodInsnNode(Opcodes.INVOKESPECIAL, owner, name, descriptor, isInterface)
    val operands = listOf(receiver) + arguments
    return DecompileInvokeSpecial.decompile(
      node,
      classNode(className, superName, interfaces),
      operands.map(Operand::variable),
      operands.map(Operand::expression).toTypedArray(),
    )
  }

  private fun currentInstance(owner: String): Operand {
    val method = MethodNode()
    val load = VarInsnNode(Opcodes.ALOAD, 0)
    method.instructions.add(load)
    val parameter = Var.Parameter(BasicValue(Type.getObjectType(owner)), 0, true)
    val copy = Var.Copy(parameter.basicValue, Insn(method, load), 1, parameter)
    return Operand(copy, ThisExpr())
  }

  private fun newObject(owner: String): Operand {
    val method = MethodNode()
    val allocationNode = TypeInsnNode(Opcodes.NEW, owner)
    val duplicateNode = InsnNode(Opcodes.DUP)
    method.instructions.add(allocationNode)
    method.instructions.add(duplicateNode)

    val allocation = Var.Instruction(BasicValue(Type.getObjectType(owner)), Insn(method, allocationNode))
    val duplicate = Var.Copy(allocation.basicValue!!, Insn(method, duplicateNode), 1, allocation)
    return Operand(duplicate, NameExpr(allocation.name))
  }

  private fun argument(expression: Expression, local: Int): Operand =
    Operand(Var.Parameter(BasicValue.REFERENCE_VALUE, local, false), expression)

  private fun classNode(name: String, superName: String?, interfaces: List<String>): ClassNode =
    ClassNode(Opcodes.ASM9).also {
      it.name = name
      it.superName = superName
      it.interfaces.addAll(interfaces)
    }

  private fun assertStatement(expected: String, actual: DecompiledInsn) {
    assertEquals(expected, assertIs<DecompiledInsn.Statement>(actual).statement.toString())
  }

  private fun assertExpression(expected: String, actual: DecompiledInsn) {
    assertEquals(expected, assertIs<DecompiledInsn.Expression>(actual).expression.toString())
  }

  private data class Operand(val variable: Var, val expression: Expression)
}
