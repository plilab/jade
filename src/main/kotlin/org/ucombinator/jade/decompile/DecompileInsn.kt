package org.ucombinator.jade.decompile

// import java.lang.invoke.LambdaMetafactory

import com.github.javaparser.ast.ArrayCreationLevel
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.ucombinator.jade.analysis.StaticSingleAssignment
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.javaparser.JavaParser

/*
Nestings

Compare
Compile

CommonLibrary
Generators
 */

/*
  scalafmt: {
    maxColumn = 300,
    align.preset = some,
    align.multiline = true,
    align.tokens.add = [{
      code = "extends", owner = "Defn.(Class|Trait|Object)",
    }],
  }
 */

// TODO: ktlint: alligned forms
// TODO: ktlint: two-line if
sealed class DecompiledInsn(val usesNextInsn: Boolean = true)
data class DecompiledStatement(val statement: Statement, val usesNextInsnArg: Boolean = true) :
  DecompiledInsn(usesNextInsnArg)
data class DecompiledExpression(val expression: Expression) :
  DecompiledInsn()
data class DecompiledStackOperation(val insn: AbstractInsnNode) :
  DecompiledInsn()
data class DecompiledIf(val labelNode: LabelNode, val condition: Expression) :
  DecompiledInsn()
data class DecompiledGoto(val labelNode: LabelNode) :
  DecompiledInsn(false)
data class DecompiledSwitch(val labels: Map<Int, LabelNode>, val default: LabelNode) :
  DecompiledInsn(false)
data class DecompiledNew(val descriptor: ClassOrInterfaceType) :
  DecompiledInsn()
data class DecompiledMonitorEnter(val expression: Expression) :
  DecompiledInsn()
data class DecompiledMonitorExit(val expression: Expression) :
  DecompiledInsn()
data class DecompiledLabel(val node: LabelNode) :
  DecompiledInsn()
data class DecompiledFrame(val node: FrameNode) :
  DecompiledInsn()
data class DecompiledLineNumber(val node: LineNumberNode) :
  DecompiledInsn()
data class DecompiledUnsupported(val insn: AbstractInsnNode) :
  DecompiledInsn()

// TODO: typo in Opcodes.java: visiTableSwitchInsn -> visitTableSwitchInsn
// TODO: typo in javaparser BlockComment: can has -> can have
// TODO: Research idea: distribution of iconst instructions in real code
// TODO: add hex form of literals in a comment
// TODO: literals: float vs double
// TODO: use `|` patterns
// TODO: UnaryExpr.Operator.BITWISE_COMPLEMENT: 0: iload_1; 1: iconst_m1; 2: ixor

@Suppress("LONG_LINE")
object DecompileInsn {
  fun decompileVar(variable: Var): NameExpr = NameExpr(variable.name)

  fun decompileExpression(retVar: Var?, expression: Expression) =
    if (retVar === null) {
      ExpressionStmt(expression)
    } else {
      ExpressionStmt(AssignExpr(decompileVar(retVar), expression, AssignExpr.Operator.ASSIGN))
    }

  @Suppress("TOO_MANY_CONSECUTIVE_SPACES", "WRONG_WHITESPACE", "MaxLineLength")
  fun decompileInsn(retVar: Var?, insn: DecompiledInsn): Statement =
    when (insn) {
      /* ktlint-disable no-multi-spaces */
      is DecompiledStatement      -> insn.statement
      is DecompiledExpression     -> decompileExpression(retVar, insn.expression)
      is DecompiledStackOperation -> JavaParser.noop("Operand Stack Operation: $insn")
      is DecompiledIf             -> IfStmt(insn.condition, BreakStmt(insn.labelNode.toString()), null)
      is DecompiledGoto           -> BreakStmt(insn.labelNode.toString()) // TODO: use instruction number?
      is DecompiledSwitch         -> JavaParser.noop("Switch ${insn.labels} ${insn.default}")
      is DecompiledNew            -> JavaParser.noop("new ${insn.descriptor}")
      is DecompiledMonitorEnter   -> JavaParser.noop("Monitor Enter: ${insn.expression}")
      is DecompiledMonitorExit    -> JavaParser.noop("Monitor Exit: ${insn.expression}")
      is DecompiledLabel          -> JavaParser.noop("Label: ${insn.node.label}")
      is DecompiledFrame          -> JavaParser.noop("Frame: ${insn.node.local} ${insn.node.stack}")
      is DecompiledLineNumber     -> JavaParser.noop("Line number: ${insn.node.line}")
      is DecompiledUnsupported    -> JavaParser.noop("Unsupported: $insn")
      /* ktlint-enable no-multi-spaces */
    }

  @Suppress("MORE_THAN_ONE_STATEMENT_PER_LINE", "TOO_MANY_CONSECUTIVE_SPACES", "WRONG_WHITESPACE", "MaxLineLength")
  fun decompileInsn(node: AbstractInsnNode, ssa: StaticSingleAssignment): Pair<Var?, DecompiledInsn> {
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
      return DecompiledExpression(
        MethodCallExpr(
          /*TODO: cast to insn.owner?*/ args(0),
          typeArguments,
          insn.name,
          NodeList(argumentTypes.indices.map { args(it + 1) })
        )
      ) // TODO: better way for this
    }
    fun staticCall(node: AbstractInsnNode): DecompiledInsn {
      val (insn, argumentTypes, typeArguments) = call(node)
      return DecompiledExpression(
        MethodCallExpr(
          ClassName.classNameExpr(insn.owner),
          typeArguments,
          insn.name,
          NodeList(argumentTypes.indices.map(::args))
        )
      )
    }
    return Pair(
      retVar,
      when (node.opcode) {
        /* ktlint-disable no-multi-spaces */
        // InsnNode
        Opcodes.NOP         -> DecompiledStatement(EmptyStmt())
        Opcodes.ACONST_NULL -> DecompiledExpression(NullLiteralExpr())
        Opcodes.ICONST_M1   -> DecompiledExpression(IntegerLiteralExpr("-1"))
        Opcodes.ICONST_0    -> DecompiledExpression(IntegerLiteralExpr("0"))
        Opcodes.ICONST_1    -> DecompiledExpression(IntegerLiteralExpr("1"))
        Opcodes.ICONST_2    -> DecompiledExpression(IntegerLiteralExpr("2"))
        Opcodes.ICONST_3    -> DecompiledExpression(IntegerLiteralExpr("3"))
        Opcodes.ICONST_4    -> DecompiledExpression(IntegerLiteralExpr("4"))
        Opcodes.ICONST_5    -> DecompiledExpression(IntegerLiteralExpr("5"))
        Opcodes.LCONST_0    -> DecompiledExpression(LongLiteralExpr("0L"))
        Opcodes.LCONST_1    -> DecompiledExpression(LongLiteralExpr("1L"))
        Opcodes.FCONST_0    -> DecompiledExpression(DoubleLiteralExpr("0.0"))
        Opcodes.FCONST_1    -> DecompiledExpression(DoubleLiteralExpr("1.0"))
        Opcodes.FCONST_2    -> DecompiledExpression(DoubleLiteralExpr("2.0"))
        Opcodes.DCONST_0    -> DecompiledExpression(DoubleLiteralExpr("0.0D"))
        Opcodes.DCONST_1    -> DecompiledExpression(DoubleLiteralExpr("1.0D"))
        // IntInsnNode
        Opcodes.BIPUSH -> DecompiledStackOperation(node)
        Opcodes.SIPUSH -> DecompiledStackOperation(node)
        // LdcInsnNode
        Opcodes.LDC -> DecompiledExpression(DecompileClass.decompileLiteral((node as LdcInsnNode).cst)!!)
        // VarInsnNode
        Opcodes.ILOAD -> DecompiledExpression(args(0))
        Opcodes.LLOAD -> DecompiledExpression(args(0))
        Opcodes.FLOAD -> DecompiledExpression(args(0))
        Opcodes.DLOAD -> DecompiledExpression(args(0))
        Opcodes.ALOAD -> DecompiledExpression(args(0))
        // InsnNode
        Opcodes.IALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.LALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.FALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.DALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.AALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.BALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.CALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        Opcodes.SALOAD -> DecompiledExpression(ArrayAccessExpr(args(0), args(1)))
        // VarInsnNode
        Opcodes.ISTORE -> DecompiledExpression(args(0))
        Opcodes.LSTORE -> DecompiledExpression(args(0))
        Opcodes.FSTORE -> DecompiledExpression(args(0))
        Opcodes.DSTORE -> DecompiledExpression(args(0))
        Opcodes.ASTORE -> DecompiledExpression(args(0))
        // InsnNode
        Opcodes.IASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.LASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.FASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.DASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.AASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.BASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.CASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.SASTORE -> DecompiledExpression(AssignExpr(ArrayAccessExpr(args(0), args(1)), args(2), AssignExpr.Operator.ASSIGN))
        Opcodes.POP     -> DecompiledStackOperation(node)
        Opcodes.POP2    -> DecompiledStackOperation(node)
        Opcodes.DUP     -> DecompiledStackOperation(node)
        Opcodes.DUP_X1  -> DecompiledStackOperation(node)
        Opcodes.DUP_X2  -> DecompiledStackOperation(node)
        Opcodes.DUP2    -> DecompiledStackOperation(node)
        Opcodes.DUP2_X1 -> DecompiledStackOperation(node)
        Opcodes.DUP2_X2 -> DecompiledStackOperation(node)
        Opcodes.SWAP    -> DecompiledStackOperation(node)
        Opcodes.IADD    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.LADD    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.FADD    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.DADD    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.PLUS))
        Opcodes.ISUB    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.LSUB    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.FSUB    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.DSUB    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MINUS))
        Opcodes.IMUL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.LMUL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.FMUL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.DMUL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.MULTIPLY))
        Opcodes.IDIV    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.LDIV    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.FDIV    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.DDIV    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.DIVIDE))
        Opcodes.IREM    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.LREM    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.FREM    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.DREM    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.REMAINDER))
        Opcodes.INEG    -> DecompiledExpression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.LNEG    -> DecompiledExpression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.FNEG    -> DecompiledExpression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.DNEG    -> DecompiledExpression(UnaryExpr(args(0), UnaryExpr.Operator.MINUS))
        Opcodes.ISHL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.LEFT_SHIFT))
        Opcodes.LSHL    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.LEFT_SHIFT))
        Opcodes.ISHR    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.SIGNED_RIGHT_SHIFT))
        Opcodes.LSHR    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.SIGNED_RIGHT_SHIFT))
        Opcodes.IUSHR   -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT))
        Opcodes.LUSHR   -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT))
        Opcodes.IAND    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_AND))
        Opcodes.LAND    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_AND))
        Opcodes.IOR     -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_OR))
        Opcodes.LOR     -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.BINARY_OR))
        Opcodes.IXOR    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.XOR))
        Opcodes.LXOR    -> DecompiledExpression(BinaryExpr(args(0), args(1), BinaryExpr.Operator.XOR))
        // IincInsnNode
        // TODO: double check that iinc works (because it is a strange instruction)
        Opcodes.IINC -> DecompiledExpression(BinaryExpr(args(0), IntegerLiteralExpr((node as IincInsnNode).incr.toString()), BinaryExpr.Operator.PLUS))
        // InsnNode
        Opcodes.I2L   -> DecompiledExpression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.I2F   -> DecompiledExpression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.I2D   -> DecompiledExpression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.L2I   -> DecompiledExpression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.L2F   -> DecompiledExpression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.L2D   -> DecompiledExpression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.F2I   -> DecompiledExpression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.F2L   -> DecompiledExpression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.F2D   -> DecompiledExpression(CastExpr(PrimitiveType.doubleType(), args(0)))
        Opcodes.D2I   -> DecompiledExpression(CastExpr(PrimitiveType.intType(), args(0)))
        Opcodes.D2L   -> DecompiledExpression(CastExpr(PrimitiveType.longType(), args(0)))
        Opcodes.D2F   -> DecompiledExpression(CastExpr(PrimitiveType.floatType(), args(0)))
        Opcodes.I2B   -> DecompiledExpression(CastExpr(PrimitiveType.byteType(), args(0)))
        Opcodes.I2C   -> DecompiledExpression(CastExpr(PrimitiveType.charType(), args(0)))
        Opcodes.I2S   -> DecompiledExpression(CastExpr(PrimitiveType.shortType(), args(0)))
        Opcodes.LCMP  -> DecompiledExpression(MethodCallExpr(null, null, SimpleName("<lcmp>"), NodeList(args(0), args(1))))
        Opcodes.FCMPL -> DecompiledExpression(MethodCallExpr(null, null, SimpleName("<fcmpl>"), NodeList(args(0), args(1))))
        Opcodes.FCMPG -> DecompiledExpression(MethodCallExpr(null, null, SimpleName("<fcmpg>"), NodeList(args(0), args(1))))
        Opcodes.DCMPL -> DecompiledExpression(MethodCallExpr(null, null, SimpleName("<dcmpl>"), NodeList(args(0), args(1))))
        Opcodes.DCMPG -> DecompiledExpression(MethodCallExpr(null, null, SimpleName("<dcmpg>"), NodeList(args(0), args(1))))
        // JumpInsnNode
        Opcodes.IFEQ      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.EQUALS))
        Opcodes.IFNE      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.IFLT      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS))
        Opcodes.IFGE      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.GREATER_EQUALS))
        Opcodes.IFGT      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.GREATER))
        Opcodes.IFLE      -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS_EQUALS))
        Opcodes.IF_ICMPEQ -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.EQUALS))
        Opcodes.IF_ICMPNE -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.IF_ICMPLT -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.LESS))
        Opcodes.IF_ICMPGE -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.GREATER_EQUALS))
        Opcodes.IF_ICMPGT -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.GREATER))
        Opcodes.IF_ICMPLE -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.LESS_EQUALS))
        Opcodes.IF_ACMPEQ -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.EQUALS))
        Opcodes.IF_ACMPNE -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), args(0), BinaryExpr.Operator.NOT_EQUALS))
        Opcodes.GOTO      -> DecompiledGoto((node as JumpInsnNode).label)
        Opcodes.JSR       -> DecompiledUnsupported(node)
        // VarInsnNode
        Opcodes.RET -> DecompiledUnsupported(node)
        // TableSwitchInsnNode
        Opcodes.TABLESWITCH -> {
          val insn = node as TableSwitchInsnNode
          assert(insn.labels.size == insn.max - insn.min)
          DecompiledSwitch((insn.labels.mapIndexed { i, l -> insn.min + i to l }).toMap(), insn.dflt)
        }
        // LookupSwitch
        Opcodes.LOOKUPSWITCH -> {
          val insn = node as LookupSwitchInsnNode
          assert(insn.labels.size == insn.keys.size)
          DecompiledSwitch(insn.keys.zip(insn.labels).toMap(), insn.dflt)
        }
        // InsnNode
        Opcodes.IRETURN -> DecompiledStatement(ReturnStmt(args(0)), false)
        Opcodes.LRETURN -> DecompiledStatement(ReturnStmt(args(0)), false)
        Opcodes.FRETURN -> DecompiledStatement(ReturnStmt(args(0)), false)
        Opcodes.DRETURN -> DecompiledStatement(ReturnStmt(args(0)), false)
        Opcodes.ARETURN -> DecompiledStatement(ReturnStmt(args(0)), false)
        Opcodes.RETURN  -> DecompiledStatement(ReturnStmt(/*Nothing*/), false)
        // FieldInsnNode
        Opcodes.GETSTATIC -> { val insn = node as FieldInsnNode; DecompiledExpression(FieldAccessExpr(ClassName.classNameExpr(insn.owner), /*TODO*/ NodeList(), SimpleName(insn.name))) }
        Opcodes.PUTSTATIC -> { val insn = node as FieldInsnNode; DecompiledExpression(AssignExpr(FieldAccessExpr(ClassName.classNameExpr(insn.owner), /*TODO*/ NodeList(), SimpleName(insn.name)), args(0), AssignExpr.Operator.ASSIGN)) }
        Opcodes.GETFIELD  -> { val insn = node as FieldInsnNode; DecompiledExpression(FieldAccessExpr(args(0), /*TODO*/ NodeList(), SimpleName(insn.name))) }
        Opcodes.PUTFIELD  -> { val insn = node as FieldInsnNode; DecompiledExpression(AssignExpr(FieldAccessExpr(args(0), /*TODO*/ NodeList(), SimpleName(insn.name)), args(1), AssignExpr.Operator.ASSIGN)) }
        // MethodInsnNode
        Opcodes.INVOKEVIRTUAL   -> instanceCall(node)
        Opcodes.INVOKESPECIAL   -> instanceCall(node) // TODO: only for <init> (new, this, and super)?
        Opcodes.INVOKESTATIC    -> staticCall(node)
        Opcodes.INVOKEINTERFACE -> instanceCall(node)
        // InvokeDynamicInsnNode
        Opcodes.INVOKEDYNAMIC -> TODO() // TODO: lambda
        // TypeInsnNode
        Opcodes.NEW -> DecompiledNew(ClassName.classNameType((node as TypeInsnNode).desc)!!) // TODO: pair with <init>
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
          DecompiledExpression(ArrayCreationExpr(type, NodeList(ArrayCreationLevel(args(0), NodeList())), null))
        }
        // TypeInsnNode
        Opcodes.ANEWARRAY -> {
          val type = ClassName.classNameType((node as TypeInsnNode).desc)
          DecompiledExpression(ArrayCreationExpr(type, NodeList(ArrayCreationLevel(args(0), NodeList())), null))
        }
        // InsnNode
        Opcodes.ARRAYLENGTH -> DecompiledExpression(FieldAccessExpr(args(0), NodeList(), SimpleName("length")))
        Opcodes.ATHROW      -> DecompiledStatement(ThrowStmt(args(0)), false)
        // TypeInsnNode
        Opcodes.CHECKCAST  -> DecompiledExpression(CastExpr(ClassName.classNameType((node as TypeInsnNode).desc), args(0))) // TODO: check if works
        Opcodes.INSTANCEOF -> DecompiledExpression(InstanceOfExpr(args(0), ClassName.classNameType((node as TypeInsnNode).desc)))
        // InsnNode
        Opcodes.MONITORENTER -> DecompiledMonitorEnter(args(0))
        Opcodes.MONITOREXIT  -> DecompiledMonitorExit(args(0))
        // MultiANewArrayInsnNode
        Opcodes.MULTIANEWARRAY -> {
          // TODO: use asm.Type functions
          val dims = (node as MultiANewArrayInsnNode).dims
          fun unwrap(type: Type): Pair<Type, Int> {
            var t = type
            var levels = 0
            while (t is ArrayType) {
              t = t.componentType
              levels = levels + 1
            }
            return Pair(t, levels)
          }
          val (type, expectedDims) = unwrap(Descriptor.fieldDescriptor(node.desc))
          val dimArgs =
            argsArray.toList().subList(0, dims).map { ArrayCreationLevel(it, NodeList()) }
          val nonDimArgs =
            (dims until expectedDims).map { ArrayCreationLevel(null, NodeList()) }
          val levels = NodeList(dimArgs.plus(nonDimArgs))
          DecompiledExpression(ArrayCreationExpr(type, levels, /*TODO: initializer*/ null))
        }
        // JumpInsnNode
        Opcodes.IFNULL    -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), NullLiteralExpr(), BinaryExpr.Operator.EQUALS))
        Opcodes.IFNONNULL -> DecompiledIf((node as JumpInsnNode).label, BinaryExpr(args(0), NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS))
        // Synthetic instructions
        else ->
          when (node) {
            is LabelNode      -> DecompiledLabel(node)
            is FrameNode      -> DecompiledFrame(node)
            is LineNumberNode -> DecompiledLineNumber(node)
            else              -> throw Exception("unknown instruction type: $node")
          }
        /* ktlint-enable no-multi-spaces */
      }
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
