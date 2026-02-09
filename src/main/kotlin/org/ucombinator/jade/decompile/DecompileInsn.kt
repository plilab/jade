package org.ucombinator.jade.decompile

import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.BreakStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.util.Errors
import com.github.javaparser.ast.expr.Expression as JPExpression
import com.github.javaparser.ast.stmt.Statement as JPStatement

// import java.lang.invoke.LambdaMetafactory

/*
Nestings

Compare
Compile

CommonLibrary
Generators
 */

// TODO: ktlint: aligned forms
// TODO: ktlint: two-line if

/** TODO:doc. */
@Suppress(
  "detekt:MaxLineLength",
  "ktlint:standard:blank-line-before-declaration",
  "ktlint:standard:max-line-length",
  "ktlint:standard:parameter-list-wrapping",
  "ktlint:standard:statement-wrapping",
)
sealed class DecompiledInsn {
  /** TODO:doc. */
  open val usesNextInsn: Boolean = true

  /** TODO:doc.
   *
   * @property statement TODO:doc
   * @property usesNextInsn TODO:doc
   */
  data class Statement(val statement: JPStatement, override val usesNextInsn: Boolean = true) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property expression TODO:doc
   */
  data class Expression(val expression: JPExpression) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property insn TODO:doc
   */
  data class StackOperation(val insn: AbstractInsnNode) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property labelNode TODO:doc
   * @property condition TODO:doc
   */
  data class If(val labelNode: LabelNode, val condition: JPExpression) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property labelNode TODO:doc
   */
  data class Goto(val labelNode: LabelNode) : DecompiledInsn() { override val usesNextInsn = false }

  /** TODO:doc.
   *
   * @property labels TODO:doc
   * @property default TODO:doc
   */
  data class Switch(val labels: Map<Int, LabelNode>, val default: LabelNode) : DecompiledInsn() { override val usesNextInsn = false }

  /** TODO:doc.
   *
   * @property descriptor TODO:doc
   */
  data class New(val descriptor: ClassOrInterfaceType) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property expression TODO:doc
   */
  data class MonitorEnter(val expression: JPExpression) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property expression TODO:doc
   */
  data class MonitorExit(val expression: JPExpression) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property node TODO:doc
   */
  data class Label(val node: LabelNode) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property node TODO:doc
   */
  data class Frame(val node: FrameNode) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property node TODO:doc
   */
  data class LineNumber(val node: LineNumberNode) : DecompiledInsn()

  /** TODO:doc.
   *
   * @property insn TODO:doc
   */
  data class Unsupported(val insn: AbstractInsnNode) : DecompiledInsn()
}

// TODO: typo in Opcodes.java: visiTableSwitchInsn -> visitTableSwitchInsn
// TODO: typo in javaparser BlockComment: can has -> can have
// TODO: Research idea: distribution of iconst instructions in real code
// TODO: add hex form of literals in a comment
// TODO: literals: float vs double
// TODO: use `|` patterns
// TODO: UnaryExpr.Operator.BITWISE_COMPLEMENT: 0: iload_1; 1: iconst_m1; 2: ixor

/** Handles decompiling individual instructions. */
object DecompileInsn {
  /** TODO:doc.
   *
   * @param variable TODO:doc
   * @return TODO:doc
   */
  fun decompileVar(variable: Var): Expression {
// return expression
    // check if param var index 1 and if in static method, add parameters maybe
    if (variable is Var.Parameter && variable.local == 0) {
      return ThisExpr() // change to "this"
    }
    return NameExpr(variable.name)
  }
  
  /** TODO:doc.
   *
   * @param retVar TODO:doc
   * @param expression TODO:doc
   * @param ssa TODO:doc
   * @return TODO:doc
   */
  fun decompileExpression(retVar: Var?, expression: Expression, ssa: StaticSingleAssignment): Statement {
    if (retVar?.basicValue == null) {
      // basicValue should not be null otherwise
      return ExpressionStmt(expression)
    }

    val mainAssign = AssignExpr(decompileVar(retVar), expression, AssignExpr.Operator.ASSIGN)
    val phiVars = mutableListOf<Var>()
    val visitedVars = mutableListOf<Var>()
    val statements = NodeList<Statement>(ExpressionStmt(mainAssign))
    phiVars.add(retVar)
//    if (mainAssign.value.isThisExpr) {
//      // check if corresponds to "this"
//      thisVars.add(mainAssign.target as NameExpr)
//    }
    while (phiVars.isNotEmpty()) {
      val phiVar = phiVars.removeAt(0)
      val dependentPhis = ssa.reverseLookup(phiVar)

      for (dependentPhi in dependentPhis) {
        val phiAssign = AssignExpr(decompileVar(dependentPhi), decompileVar(phiVar), AssignExpr.Operator.ASSIGN)
        statements.add(ExpressionStmt(phiAssign))
        if (!visitedVars.contains(dependentPhi)) {
          phiVars.add(dependentPhi)
          visitedVars.add(dependentPhi)
        }
      }
    }
    return BlockStmt(statements)
  }

  /** TODO:doc.
   *
   * @param retVar TODO:doc
   * @param insn TODO:doc
   * @param ssa TODO:doc
   * @return TODO:doc
   */
  fun decompileInsn(retVar: Var?, insn: DecompiledInsn, ssa: StaticSingleAssignment): Statement =
    @Suppress("TOO_MANY_CONSECUTIVE_SPACES", "WRONG_WHITESPACE", "ktlint:standard:no-multi-spaces")
    when (insn) {
      is DecompiledInsn.Statement      -> insn.statement
      is DecompiledInsn.Expression     -> {
        decompileExpression(retVar, insn.expression, ssa)
      }
      is DecompiledInsn.StackOperation -> JavaParser.noop("Operand Stack Operation: $insn")
      is DecompiledInsn.If             -> IfStmt(insn.condition, BreakStmt(insn.labelNode.toString()), null)
      is DecompiledInsn.Goto           -> BreakStmt(insn.labelNode.toString()) // TODO: use instruction number?
      is DecompiledInsn.Switch         -> JavaParser.noop("Switch ${insn.labels} ${insn.default}")
      is DecompiledInsn.New            -> JavaParser.noop("new ${insn.descriptor}")
      is DecompiledInsn.MonitorEnter   -> JavaParser.noop("Monitor Enter: ${insn.expression}")
      is DecompiledInsn.MonitorExit    -> JavaParser.noop("Monitor Exit: ${insn.expression}")
      is DecompiledInsn.Label          -> JavaParser.noop("Label: ${insn.node.label}")
      is DecompiledInsn.Frame          -> JavaParser.noop("Frame: ${insn.node.local} ${insn.node.stack}")
      is DecompiledInsn.LineNumber     -> JavaParser.noop("Line number: ${insn.node.line}")
      is DecompiledInsn.Unsupported    -> JavaParser.noop("Unsupported: $insn")
    }

  /** TODO:doc.
   *
   * @param node TODO:doc
   * @param ssa TODO:doc
   * @return TODO:doc
   */
  @Suppress(
    "MORE_THAN_ONE_STATEMENT_PER_LINE",
    "TOO_MANY_CONSECUTIVE_SPACES",
    "WRONG_WHITESPACE",
    "detekt:MaxLineLength",
  )
  fun decompileInsn(node: AbstractInsnNode, ssa: StaticSingleAssignment, classNode: ClassNode, thisVars: Set<String>): Pair<Var?, DecompiledInsn> {
    val (retVar, argVars) = ssa.insnVars.getOrElse(node, { Pair(null, listOf()) })
    val argsArray: Array<Expression> = argVars.map(::decompileVar).toTypedArray()

    fun args(i: Int): Expression = argsArray[i]

    fun call(node: AbstractInsnNode): Triple<MethodInsnNode, List<Type>, NodeList<Type>> {
      val insn = node as MethodInsnNode
      val (argumentTypes, _) = Descriptor.methodDescriptor(insn.desc)
      val typeArguments = NodeList<Type>()
      return Triple(insn, argumentTypes, typeArguments)
    }

    fun instanceCall(node: AbstractInsnNode): DecompiledInsn {
      val (insn, argumentTypes, typeArguments) = call(node)
      return DecompiledInsn.Expression(
        MethodCallExpr(
          /*TODO: cast to insn.owner?*/ args(0),
          typeArguments,
          insn.name,
          NodeList(argumentTypes.indices.map { args(it + 1) }),
        ),
      )
    }

    fun superCall(node: AbstractInsnNode, classNode: ClassNode): DecompiledInsn {
      val (insn, argumentTypes, typeArguments) = call(node)

      // declare a constant if asm dont have
      // can use ==
      var insnName = insn.name
      if (insn.name == "<init>" && args(0).toString() in thisVars) {
        // check for <init> and nameExpr var refers to "this"?
        // refers to super call
        //TODO: need to further check target (super class or own constructor)
        if (insn.owner == classNode.name) { // checks if its this()
          return DecompiledInsn.Statement(
            ExplicitConstructorInvocationStmt(
              true,
              null,
              NodeList(argumentTypes.indices.map { args(it + 1) })
            )
          )
        } else {
          return DecompiledInsn.Statement(
            ExplicitConstructorInvocationStmt(
              false,
              null,
              NodeList(argumentTypes.indices.map { args(it + 1) })
            )
          )
        }
      } else {
        // TODO: for creation of new instance, not checked yet
        return DecompiledInsn.Expression(
          MethodCallExpr(
            /*TODO: cast to insn.owner?*/ args(0),
            typeArguments,
            insnName, //SuperExpr().toString()
            NodeList(argumentTypes.indices.map { args(it + 1) }),
          )
        )
      }
    }

    fun staticCall(node: AbstractInsnNode): DecompiledInsn {
      val (insn, argumentTypes, typeArguments) = call(node)
      return DecompiledInsn.Expression(
        MethodCallExpr(
          ClassName.classNameExpr(insn.owner),
          typeArguments,
          insn.name,
          NodeList(argumentTypes.indices.map(::args)),
        ),
      )
    }

    return Pair(
      retVar,
      @Suppress(
        "ktlint:standard:argument-list-wrapping",
        "ktlint:standard:max-line-length",
        "ktlint:standard:no-multi-spaces",
        "ktlint:standard:wrapping",
      )
      when (node.opcode) {
        // InsnNode
        Opcodes.NOP         -> DecompiledInsn.Statement(EmptyStmt())
        Opcodes.ACONST_NULL -> DecompiledInsn.Expression(NullLiteralExpr())
        Opcodes.ICONST_M1   -> DecompiledInsn.Expression(IntegerLiteralExpr("-1"))
        Opcodes.ICONST_0    -> DecompiledInsn.Expression(IntegerLiteralExpr("0"))
        Opcodes.ICONST_1    -> DecompiledInsn.Expression(IntegerLiteralExpr("1"))
        Opcodes.ICONST_2    -> DecompiledInsn.Expression(IntegerLiteralExpr("2"))
        Opcodes.ICONST_3    -> DecompiledInsn.Expression(IntegerLiteralExpr("3"))
        Opcodes.ICONST_4    -> DecompiledInsn.Expression(IntegerLiteralExpr("4"))
        Opcodes.ICONST_5    -> DecompiledInsn.Expression(IntegerLiteralExpr("5"))
        Opcodes.LCONST_0    -> DecompiledInsn.Expression(LongLiteralExpr("0L"))
        Opcodes.LCONST_1    -> DecompiledInsn.Expression(LongLiteralExpr("1L"))
        Opcodes.FCONST_0    -> DecompiledInsn.Expression(DoubleLiteralExpr("0.0"))
        Opcodes.FCONST_1    -> DecompiledInsn.Expression(DoubleLiteralExpr("1.0"))
        Opcodes.FCONST_2    -> DecompiledInsn.Expression(DoubleLiteralExpr("2.0"))
        Opcodes.DCONST_0    -> DecompiledInsn.Expression(DoubleLiteralExpr("0.0D"))
        Opcodes.DCONST_1    -> DecompiledInsn.Expression(DoubleLiteralExpr("1.0D"))
        // IntInsnNode
        Opcodes.BIPUSH -> DecompiledInsn.Expression(DecompileClass.decompileLiteral((node as IntInsnNode).operand)!!)
        Opcodes.SIPUSH -> DecompiledInsn.Expression(DecompileClass.decompileLiteral((node as IntInsnNode).operand)!!)
        // LdcInsnNode
        Opcodes.LDC -> DecompiledInsn.Expression(DecompileClass.decompileLiteral((node as LdcInsnNode).cst)!!)
        // VarInsnNode
        Opcodes.ILOAD -> DecompiledInsn.Expression(args(0))
        Opcodes.LLOAD -> DecompiledInsn.Expression(args(0))
        Opcodes.FLOAD -> DecompiledInsn.Expression(args(0))
        Opcodes.DLOAD -> DecompiledInsn.Expression(args(0))
        Opcodes.ALOAD -> DecompiledInsn.Expression(args(0))
        // InsnNode
        Opcodes.IALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.LALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.FALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.DALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.AALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.BALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.CALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.SALOAD -> DecompiledInsn.Expression(ArrayAccessExpr(args(0), args(1)))
        // VarInsnNode
        Opcodes.ISTORE -> DecompiledInsn.Expression(args(0))
        Opcodes.LSTORE -> DecompiledInsn.Expression(args(0))
        Opcodes.FSTORE -> DecompiledInsn.Expression(args(0))
        Opcodes.DSTORE -> DecompiledInsn.Expression(args(0))
        Opcodes.ASTORE -> DecompiledInsn.Expression(args(0))
        // InsnNode
        Opcodes.IASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.LASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.FASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.DASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.AASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.BASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.CASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.SASTORE -> DecompiledInsn.Expression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.POP     -> DecompiledInsn.StackOperation(node)
        Opcodes.POP2    -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP     -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP_X1  -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP_X2  -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP2    -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP2_X1 -> DecompiledInsn.StackOperation(node)
        Opcodes.DUP2_X2 -> DecompiledInsn.StackOperation(node)
        Opcodes.SWAP    -> DecompiledInsn.StackOperation(node)
        Opcodes.IADD    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.LADD    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.FADD    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.DADD    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.ISUB    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.LSUB    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.FSUB    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.DSUB    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.IMUL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.LMUL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.FMUL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.DMUL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.IDIV    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.LDIV    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.FDIV    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.DDIV    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.IREM    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.LREM    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.FREM    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.DREM    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.INEG    -> DecompiledInsn.Expression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.LNEG    -> DecompiledInsn.Expression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.FNEG    -> DecompiledInsn.Expression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.DNEG    -> DecompiledInsn.Expression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.ISHL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.LEFT_SHIFT))
        Opcodes.LSHL    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.LEFT_SHIFT))
        Opcodes.ISHR    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.SIGNED_RIGHT_SHIFT))
        Opcodes.LSHR    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.SIGNED_RIGHT_SHIFT))
        Opcodes.IUSHR   -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT))
        Opcodes.LUSHR   -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT))
        Opcodes.IAND    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_AND))
        Opcodes.LAND    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_AND))
        Opcodes.IOR     -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_OR))
        Opcodes.LOR     -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_OR))
        Opcodes.IXOR    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.XOR))
        Opcodes.LXOR    -> DecompiledInsn.Expression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.XOR))
        // IincInsnNode
        // TODO: double check that iinc works (because it is a strange instruction)
        Opcodes.IINC -> DecompiledInsn.Expression(BinaryExpr(args(0), IntegerLiteralExpr((node as IincInsnNode).incr.toString()), BinaryExpr.Operator.PLUS))
        // InsnNode
        Opcodes.I2L   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.I2F   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.I2D   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.L2I   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.L2F   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.L2D   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.F2I   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.F2L   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.F2D   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.D2I   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.D2L   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.D2F   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.I2B   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.byteType(), args(0)))
        Opcodes.I2C   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.charType(), args(0)))
        Opcodes.I2S   -> DecompiledInsn.Expression(CastExpr(PrimitiveType.shortType(), args(0)))
        Opcodes.LCMP  -> DecompiledInsn.Expression(MethodCallExpr(null, null, SimpleName("<lcmp>"), NodeList(args(0), args(1))))
        Opcodes.FCMPL -> DecompiledInsn.Expression(MethodCallExpr(null, null, SimpleName("<fcmpl>"), NodeList(args(0), args(1))))
        Opcodes.FCMPG -> DecompiledInsn.Expression(MethodCallExpr(null, null, SimpleName("<fcmpg>"), NodeList(args(0), args(1))))
        Opcodes.DCMPL -> DecompiledInsn.Expression(MethodCallExpr(null, null, SimpleName("<dcmpl>"), NodeList(args(0), args(1))))
        Opcodes.DCMPG -> DecompiledInsn.Expression(MethodCallExpr(null, null, SimpleName("<dcmpg>"), NodeList(args(0), args(1))))
        // JumpInsnNode
        Opcodes.IFEQ      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.EQUALS))
        Opcodes.IFNE      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.IFLT      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS))
        Opcodes.IFGE      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.GREATER_EQUALS))
        Opcodes.IFGT      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.GREATER))
        Opcodes.IFLE      -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS_EQUALS))
        Opcodes.IF_ICMPEQ -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.EQUALS))
        Opcodes.IF_ICMPNE -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.IF_ICMPLT -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.LESS))
        Opcodes.IF_ICMPGE -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.GREATER_EQUALS))
        Opcodes.IF_ICMPGT -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.GREATER))
        Opcodes.IF_ICMPLE -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.LESS_EQUALS))
        Opcodes.IF_ACMPEQ -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.EQUALS))
        Opcodes.IF_ACMPNE -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), args(1), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.GOTO      -> DecompiledInsn.Goto((node as JumpInsnNode).label)
        Opcodes.JSR       -> DecompiledInsn.Unsupported(node)
        // VarInsnNode
        Opcodes.RET -> DecompiledInsn.Unsupported(node)
        // TableSwitchInsnNode
        Opcodes.TABLESWITCH -> {
          val insn = node as TableSwitchInsnNode
          assert(insn.labels.size == insn.max - insn.min)
          DecompiledInsn.Switch(insn.labels.mapIndexed { i, l -> insn.min + i to l }.toMap(), insn.dflt)
        }
        // LookupSwitch
        Opcodes.LOOKUPSWITCH -> {
          val insn = node as LookupSwitchInsnNode
          assert(insn.labels.size == insn.keys.size)
          DecompiledInsn.Switch(insn.keys.zip(insn.labels).toMap(), insn.dflt)
        }
        // InsnNode
        Opcodes.IRETURN -> DecompiledInsn.Statement(ReturnStmt(args(0)), false)
        Opcodes.LRETURN -> DecompiledInsn.Statement(ReturnStmt(args(0)), false)
        Opcodes.FRETURN -> DecompiledInsn.Statement(ReturnStmt(args(0)), false)
        Opcodes.DRETURN -> DecompiledInsn.Statement(ReturnStmt(args(0)), false)
        Opcodes.ARETURN -> DecompiledInsn.Statement(ReturnStmt(args(0)), false)
        Opcodes.RETURN  -> DecompiledInsn.Statement(ReturnStmt(/*Nothing*/), false)
        // FieldInsnNode
        Opcodes.GETSTATIC -> (node as FieldInsnNode).let { DecompiledInsn.Expression(FieldAccessExpr(ClassName.classNameExpr(it.owner), /*TODO*/ NodeList(), SimpleName(it.name))) }
        Opcodes.PUTSTATIC -> (node as FieldInsnNode).let { DecompiledInsn.Expression(AssignExpr(FieldAccessExpr(ClassName.classNameExpr(it.owner), /*TODO*/ NodeList(), SimpleName(it.name)), args(0), AssignExpr.Operator.ASSIGN)) }
        Opcodes.GETFIELD  -> (node as FieldInsnNode).let { DecompiledInsn.Expression(FieldAccessExpr(args(0), /*TODO*/ NodeList(), SimpleName(it.name))) }
        Opcodes.PUTFIELD  -> (node as FieldInsnNode).let { DecompiledInsn.Expression(AssignExpr(FieldAccessExpr(args(0), /*TODO*/ NodeList(), SimpleName(it.name)), args(1), AssignExpr.Operator.ASSIGN)) }
        // MethodInsnNode
        Opcodes.INVOKEVIRTUAL   -> instanceCall(node)
        Opcodes.INVOKESPECIAL   -> superCall(node, classNode) // TODO: only for <init> (new, this, and super)?
        Opcodes.INVOKESTATIC    -> staticCall(node)
        Opcodes.INVOKEINTERFACE -> instanceCall(node)
        // InvokeDynamicInsnNode
        Opcodes.INVOKEDYNAMIC -> TODO() // TODO: lambda
        // TypeInsnNode
        Opcodes.NEW -> DecompiledInsn.New(ClassName.classNameType((node as TypeInsnNode).desc)) // TODO: pair with <init>
        // IntInsnNode
        Opcodes.NEWARRAY -> {
          val type = when ((node as IntInsnNode).operand) {
            Opcodes.T_BOOLEAN -> PrimitiveType.booleanType()
            Opcodes.T_CHAR    -> PrimitiveType.charType()
            Opcodes.T_FLOAT   -> PrimitiveType.floatType()
            Opcodes.T_DOUBLE  -> PrimitiveType.doubleType()
            Opcodes.T_BYTE    -> PrimitiveType.byteType()
            Opcodes.T_SHORT   -> PrimitiveType.shortType()
            Opcodes.T_INT     -> PrimitiveType.intType()
            Opcodes.T_LONG    -> PrimitiveType.longType()
            else              -> TODO()
          }
          DecompiledInsn.Expression(ArrayCreationExpr(type, NodeList(ArrayCreationLevel(args(0), NodeList())), null))
        }
        // TypeInsnNode
        Opcodes.ANEWARRAY -> {
          val type = ClassName.classNameType((node as TypeInsnNode).desc)
          DecompiledInsn.Expression(ArrayCreationExpr(type, NodeList(ArrayCreationLevel(args(0), NodeList())), null))
        }
        // InsnNode
        Opcodes.ARRAYLENGTH -> DecompiledInsn.Expression(FieldAccessExpr(args(0), NodeList(), SimpleName("length")))
        Opcodes.ATHROW      -> DecompiledInsn.Statement(ThrowStmt(args(0)), false)
        // TypeInsnNode
        Opcodes.CHECKCAST  -> DecompiledInsn.Expression(CastExpr(ClassName.classNameType((node as TypeInsnNode).desc), args(0))) // TODO: check if works
        Opcodes.INSTANCEOF -> DecompiledInsn.Expression(InstanceOfExpr(args(0), ClassName.classNameType((node as TypeInsnNode).desc)))
        // InsnNode
        Opcodes.MONITORENTER -> DecompiledInsn.MonitorEnter(args(0))
        Opcodes.MONITOREXIT  -> DecompiledInsn.MonitorExit(args(0))
        // MultiANewArrayInsnNode
        Opcodes.MULTIANEWARRAY -> {
          fun unwrap(type: Type): Pair<Type, Int> {
            // TODO: is this simpler if we use recursion instead of iteration? or maybe something like `.fold()`
            var t = type
            var levels = 0
            while (t is ArrayType) {
              t = t.componentType
              levels = levels + 1
            }
            return Pair(t, levels)
          }

          // TODO: use asm.Type functions
          val dims = (node as MultiANewArrayInsnNode).dims
          val (type, expectedDims) = unwrap(Descriptor.fieldDescriptor(node.desc))
          val dimArgs = argsArray.toList().subList(0, dims).map { ArrayCreationLevel(it, NodeList()) }
          val nonDimArgs = (dims until expectedDims).map { ArrayCreationLevel(null, NodeList()) }
          val levels = NodeList(dimArgs.plus(nonDimArgs))
          DecompiledInsn.Expression(ArrayCreationExpr(type, levels, null)) // TODO: replace null initializer
        }
        // JumpInsnNode
        Opcodes.IFNULL    -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), NullLiteralExpr(), BinaryExpr.Operator.EQUALS))
        Opcodes.IFNONNULL -> DecompiledInsn.If((node as JumpInsnNode).label, BinaryExpr(args(0), NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS))
        // Synthetic instructions
        else ->
          when (node) {
            is LabelNode      -> DecompiledInsn.Label(node)
            is FrameNode      -> DecompiledInsn.Frame(node)
            is LineNumberNode -> DecompiledInsn.LineNumber(node)
            else              -> Errors.unmatchedType(node)
          }
      },
    )
  }

  /*
  //case class DecodedLambda(interface: SootClass, interfaceMethod: SootMethod,
    implementationMethod: SootMethod, captures: java.util.List[soot.Value])
  // https://cr.openjdk.java.net/~briangoetz/lambda/lambda-translation.html
  fun decodeLambda(e: InvokeDynamicInsnNode): LambdaExpr = {
    // Arguments are the captured variables
    // Static (bootstrap?) arguments describe lambda

    fun decodeSimple(): LambdaExpr = {
      // Step 1: Find `interface`, which is the type of the closure returned by the lambda.
      assert(e.type.isInstanceOf[RefType])
      val interface = e.type.asInstanceOf[RefType].getSootClass

      // Step 2: Find the method in `interface` that the lambda corresponds to.
      // Unfortunately, this is not already computed so we find it manually.
      // This is complicated by the fact that it may be in a super-class or
      // super-interface of `interface`.
      assert(e.bsmArgs(0).isInstanceOf[ClassConstant])
      val bytecodeSignature = e.bsmArgs(0).asInstanceOf[ClassConstant].getValue
      val types = Util.v().jimpleTypesOfFieldOrMethodDescriptor(bytecodeSignature)
      val paramTypes = java.util.Arrays.asList[Type](types:_*).subList(0, types.length - 1)
      val returnType = types(types.length - 1)

      fun findMethod(klass: SootClass): SootMethod = {
        val m = klass.getMethodUnsafe(e.method.name, paramTypes, returnType)
        if (m != null) { return m }

        if (klass.hasSuperclass) {
          val m = findMethod(klass.superclass)
          if (m != null) { return m }
        }

        for (i <- klass.iterfaces.asScala) {
          val m = findMethod(i)
          if (m != null) { return m }
        }

        return null
      }

      val interfaceMethod = findMethod(interface)

      // Step 3: Find `implementationMethod`, which is the method implementing the lambda
      //   Because calling e.getMethodRef.resolve may throw missmatched `static` errors,
      //   we look for the method manually.
      assert(e.bsmArgs(1).isInstanceOf[soot.jimple.MethodHandle])
      val implementationMethodRef = e.getBootstrapArg(1).asInstanceOf[soot.jimple.MethodHandle].getMethodRef
      val implementationMethod = implementationMethodRef.declaringClass()
        .getMethodUnsafe(implementationMethodRef.getSubSignature)

      // Step 4: Find the `captures` which are values that should be saved and passed
      //   to `implementationMethod` before any other arguments
      val captures = e.getArgs

      return DecodedLambda(interface, interfaceMethod, implementationMethod, captures)
    }

    fun bootstrapArgIsInt(index: Int, i: Int): Boolean = {
      e.bsmArgs(index) match {
        case arg: Integer => arg == i
        case _ => false
      }
    }

    // Check that this dynamic invoke uses LambdaMetafactory
    assert(e.bsm.getOwner == "java.lang.invoke.LambdaMetafactory")
    val bootstrapMethod = e.bsm.getName
    if (bootstrapMethod == "metafactory") {
      assert(e.bsmArgs.length == 3)
      return decodeSimple()
    } else if (bootstrapMethod == "altMetafactory") {
      e.bsmArgs(3) match {
        case flags: Integer =>
          val bridges = (flags & LambdaMetafactory.FLAG_BRIDGES) != 0
          val markers = (flags & LambdaMetafactory.FLAG_MARKERS) != 0
          val isSimple =
            (bridges && !markers && bootstrapArgIsInt(4, 0)) ||
            (!bridges && markers && bootstrapArgIsInt(4, 0)) ||
            (bridges && markers && bootstrapArgIsInt(4, 0) && bootstrapArgIsInt(5, 0))
          if (isSimple) { decodeSimple() }
          else { throw new Exception("Unimplemented altMetafactory: e = " + e) }
        case _ => throw new Exception("Non-int flags passed to altMetafactory: e = " + e)
      }
    } else {
      throw new Exception("Soot.decodeLambda could not decode: e = " + e)
    }
  }
   */
}
