package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.Type
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("BACKTICKS_PROHIBITED")
object DescriptorTest {
  // TODO: ktlint closing paren on same line

  enum class Kind { FIELD, METHOD }

  @Suppress("ktlint:standard:max-line-length", "ktlint:standard:argument-list-wrapping", "MaxLineLength")
  @JvmStatic fun tests() = listOf<Triple<Kind, String, String?>>(
    // Triple(descriptor kind, descriptor, expected result or null for invalid)
    Triple(Kind.FIELD, "", null),
    Triple(Kind.METHOD, "", null),
    Triple(Kind.FIELD, "L.;", null),
    Triple(Kind.FIELD, "L;;", null),
    Triple(Kind.FIELD, "L[;", null),
    Triple(Kind.FIELD, "L/;", null),
    Triple(Kind.FIELD, "L<;", null),
    Triple(Kind.FIELD, "L>;", null),
    Triple(Kind.FIELD, "L:;", null),
    Triple(Kind.FIELD, "Z", "boolean"),
    Triple(Kind.FIELD, "C", "char"),
    Triple(Kind.FIELD, "B", "byte"),
    Triple(Kind.FIELD, "S", "short"),
    Triple(Kind.FIELD, "I", "int"),
    Triple(Kind.FIELD, "J", "long"),
    Triple(Kind.FIELD, "F", "float"),
    Triple(Kind.FIELD, "D", "double"),
    Triple(Kind.FIELD, "Ljava/lang/Object;", "java.lang.Object"),
    Triple(Kind.FIELD, "[[Z", "boolean[][]"),
    Triple(Kind.FIELD, "[[Ljava/lang/Object;", "java.lang.Object[][]"),
    Triple(Kind.METHOD, "(ZJ[[Ljava/lang/Object;)[[Ljava/lang/Object;", "boolean,long,java.lang.Object[][];java.lang.Object[][]"),
    Triple(Kind.METHOD, "()V", ";void"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/langtools/tools/javap/classfile/6888367/T6888367.java
    Triple(Kind.FIELD, "Z", "boolean"),
    Triple(Kind.FIELD, "B", "byte"),
    Triple(Kind.FIELD, "C", "char"),
    Triple(Kind.FIELD, "D", "double"),
    Triple(Kind.FIELD, "F", "float"),
    Triple(Kind.FIELD, "I", "int"),
    Triple(Kind.FIELD, "J", "long"),
    Triple(Kind.FIELD, "S", "short"),
    Triple(Kind.FIELD, "LClss;", "Clss"),
    Triple(Kind.FIELD, "LIntf;", "Intf"),
    Triple(Kind.FIELD, "[I", "int[]"),
    Triple(Kind.FIELD, "[LClss;", "Clss[]"),
    Triple(Kind.FIELD, "LGenClss;", "GenClss"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()LClss;", ";Clss"),
    Triple(Kind.METHOD, "()[I", ";int[]"),
    Triple(Kind.METHOD, "()[LClss;", ";Clss[]"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "()Ljava/lang/Object;", ";java.lang.Object"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "()LGenClss;", ";GenClss"),
    Triple(Kind.METHOD, "(I)V", "int;void"),
    Triple(Kind.METHOD, "(LClss;)V", "Clss;void"),
    Triple(Kind.METHOD, "([I)V", "int[];void"),
    Triple(Kind.METHOD, "([LClss;)V", "Clss[];void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;)V", "java.lang.Object;void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "(LGenClss;)V", "GenClss;void"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "()V", ";void"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/access_test/pkg1/MethodTypeDescriptorAccessTest.java
    Triple(Kind.METHOD, "(Lpkg2/PublicClass;)Lpkg2/PublicClass;", "pkg2.PublicClass;pkg2.PublicClass"),
    Triple(Kind.METHOD, "()Lpkg2/NonPublicClass;", ";pkg2.NonPublicClass"),
    Triple(Kind.METHOD, "(Lpkg2/NonPublicClass;)I", "pkg2.NonPublicClass;int"),
    Triple(Kind.METHOD, "(Lpkg2/NonPublicClass;)I", "pkg2.NonPublicClass;int"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/boottest/java.base/java/lang/constant/ConstantUtilsTest.java
    Triple(Kind.FIELD, ".", null),
    Triple(Kind.FIELD, ";", null),
    Triple(Kind.FIELD, "[", null),
    Triple(Kind.FIELD, "/", null),
    Triple(Kind.FIELD, "<", null),
    Triple(Kind.FIELD, ">", null),
    Triple(Kind.FIELD, "(V)V", null),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/ClassDescTest.java
    Triple(Kind.FIELD, "Ljava/lang/String;", "java.lang.String"),
    Triple(Kind.FIELD, "II", null),
    Triple(Kind.FIELD, "I;", null),
    Triple(Kind.FIELD, "Q", null),
    Triple(Kind.FIELD, "L", null),
    Triple(Kind.FIELD, "", null),
    Triple(Kind.FIELD, "java.lang.String", null),
    Triple(Kind.FIELD, "[]", null),
    Triple(Kind.FIELD, "Ljava/lang/String", null),
    // Triple(Kind.FIELD, "Ljava.lang.String;", null), // invalid descriptor but valid signature
    Triple(Kind.FIELD, "java/lang/String", null),
    Triple(Kind.FIELD, "I;", null),
    Triple(Kind.FIELD, "[]", null),
    Triple(Kind.FIELD, "Ljava/lang/String", null),
    // Triple(Kind.FIELD, "Ljava.lang.String;", null), // invalid descriptor but valid signature
    Triple(Kind.FIELD, "java/lang/String", null),
    Triple(Kind.FIELD, "I;", null),
    Triple(Kind.FIELD, "[]", null),
    // Triple(Kind.FIELD, "[Ljava/lang/String;", null), // invalid "internal name" but valid descriptor
    // Triple(Kind.FIELD, "Ljava.lang.String;", null), // invalid descriptor but valid signature
    Triple(Kind.FIELD, "java.lang.String", null),
    Triple(Kind.FIELD, "Ljava/lang/String;", "java.lang.String"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/DynamicCallSiteDescTest.java
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/MethodHandleDescTest.java
    Triple(Kind.METHOD, "()Z", ";boolean"),
    Triple(Kind.METHOD, "()Z", ";boolean"),
    Triple(Kind.METHOD, "()Z", ";boolean"),
    Triple(Kind.METHOD, "()Z", ";boolean"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;)V", "java.lang.Object;void"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;I)I", "java.lang.Object,int;int"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;)I", "java.lang.Object;int"),
    Triple(Kind.METHOD, "(I)I", "int;int"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;I)I", "java.lang.Object,int;int"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(I)V", "int;void"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;)V", "java.lang.Object;void"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;I)I", "java.lang.Object,int;int"),
    Triple(Kind.METHOD, "(Ljava/lang/Object;II)V", "java.lang.Object,int,int;void"),
    Triple(Kind.METHOD, "()V", ";void"),
    Triple(Kind.METHOD, "(II)V", "int,int;void"),
    Triple(Kind.METHOD, "()I", ";int"),
    Triple(Kind.METHOD, "()I", ";int"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/methodTypeDesc/ResolveConstantDesc.java
    Triple(Kind.METHOD, "()Ljdk/internal/misc/VM;", ";jdk.internal.misc.VM"),
    Triple(Kind.METHOD, "()Lsun/misc/Unsafe;", ";sun.misc.Unsafe"),
    Triple(Kind.METHOD, "()Ljdk/internal/access/SharedSecrets;", ";jdk.internal.access.SharedSecrets"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/MethodTypeDescTest.java
    Triple(Kind.METHOD, "()II", null),
    Triple(Kind.METHOD, "()I;", null),
    Triple(Kind.METHOD, "(I;)", null),
    Triple(Kind.METHOD, "(I)", null),
    Triple(Kind.METHOD, "()L", null),
    Triple(Kind.METHOD, "(V)V", null),
    Triple(Kind.METHOD, "(java.lang.String)V", null),
    Triple(Kind.METHOD, "()[]", null),
    Triple(Kind.METHOD, "(Ljava/lang/String)V", null),
    // Triple(Kind.METHOD, "(Ljava.lang.String;)V", null), // invalid descriptor but valid signature
    Triple(Kind.METHOD, "(java/lang/String)V", null),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/NameValidationTest.java
    Triple(Kind.METHOD, "()Z", ";boolean"),

    // From https://github.com/openjdk/jdk/blob/jdk-23%2B23/test/jdk/java/lang/constant/SymbolicDescTest.java
    Triple(Kind.FIELD, "Ljava/lang/String;", "java.lang.String"),
    Triple(Kind.FIELD, "Ljava/util/List;", "java.util.List"),
    Triple(Kind.FIELD, "I", "int"),
    Triple(Kind.FIELD, "J", "long"),
    Triple(Kind.FIELD, "S", "short"),
    Triple(Kind.FIELD, "B", "byte"),
    Triple(Kind.FIELD, "C", "char"),
    Triple(Kind.FIELD, "F", "float"),
    Triple(Kind.FIELD, "D", "double"),
    Triple(Kind.FIELD, "Z", "boolean"),
    Triple(Kind.FIELD, "V", "void"),
  )

  fun <T : Type> resultsToString(r: List<T>): String = r.joinToString(",") { it.asString() }

  fun parseDescriptor(kind: Kind, descriptor: String): String = when (kind) {
    Kind.FIELD -> Descriptor.fieldDescriptor(descriptor).asString()
    Kind.METHOD -> {
      val s = Descriptor.methodDescriptor(descriptor)
      listOf(
        resultsToString(s.parameterTypes),
        s.returnType.asString(),
      ).joinToString(";")
    }
  }

  @ParameterizedTest @MethodSource("tests") fun `test descriptor`(test: Triple<Kind, String, String?>) =
    SignatureTest.test("descriptor", test, ::parseDescriptor)
}
