package org.michaeldadams.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jsoup.Jsoup
import java.io.File
import java.net.URL

fun <A> List<A>.pairs(): List<Pair<A, A>> = (0 until this.size step 2).map { Pair(this[it], this[it + 1]) }

// TODO: Generate Flags.txt (see `sbt flagsTable`)
// Code for generating `Flags.txt` and `Flags.kt`
class GenerateClassfileFlagsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks.register("generateClassfileFlags") {
      // TODO: description = "Displays the project version."
      // group = "Help"
      doLast {
        val flagsTxt = "src/main/kotlin/org/ucombinator/jade/classfile/Flags.txt" // TODO: configurable?
        val code = code(File(project.projectDir, flagsTxt).readText(Charsets.UTF_8))
        project.generateSrc("Flags.kt", code)
      }
    }
  }

  companion object {
    val generatorName = "org.michaeldadams.gradle.GenerateClassfileFlags" // TODO: use reflection to get class name
    const val MIN_JAVA_VERSION = 9
    fun javaSpec(spec: String, version: Int, chapter: Int): String { // TODO: task
      if (version < MIN_JAVA_VERSION) throw Exception("version must be at least 9 but is $version.")
      val url = "https://docs.oracle.com/javase/specs/$spec/se$version/html/$spec-$chapter.html"
      return URL(url).readText() // TODO: depricated
    }

    fun table(html: String): String {
      val builder = StringBuilder()
      val document = Jsoup.parse(html)

      builder.append("""
          |# Do not edit this file by hand.  It is generated by ${generatorName}.
          |
          |# Kind      Name             Value  Keyword      Description
          |
        """.trimMargin()
      )

      val tables = listOf<Pair<String, String>>(
        "Class" to "Class access and property modifiers",
        "Field" to "Field access and property flags",
        "Method" to "Method access and property flags",
        "NestedClass" to "Nested class access and property flags",
      )
      for ((kind, tableSummary) in tables) {
        val (table) = document.select("table[summary=\"$tableSummary\"]")
        for (row in table.select("tbody > tr").toList()) {
          val (accName, value, description) = row.select("td").toList()
          val keywordOption = "(Declared|Marked|Marked or implicitly) <code class=\"literal\">(.*)</code>"
            .toRegex()
            .find(description.childNodes().joinToString())
          val keyword = if (keywordOption == null) "-" else keywordOption.groupValues[2] // TODO: 2 -> 1?
          builder.append("$kind%-11s ${accName.text()}%-16s ${value.text()} $keyword%-12s ${description.text()}\n")
        }
        builder.append("\n")
      }

      val lists = listOf<Pair<String, String>>(
        "Parameter" to "access_flags",
        "Module" to "module_flags",
        "Requires" to "requires_flags",
        "Exports" to "exports_flags",
        "Opens" to "opens_flags",
      )
      for ((kind, codeLiteral) in lists) {
        val (list) = document.select(
          "dd:has(div[class=variablelist] dl) > p:matchesOwn(The value of the) " +
            "> code[class=literal]:matchesOwn(^$codeLiteral$$)"
        )
        val rows = list.parent()!!.nextElementSibling()!!.child(0).children().pairs()
        for ((row, description) in rows) {
          val regexMatch = """(0x[0-9]*) \(([A-Z_]*)\)""".toRegex().find(row.text())!!
          val value = regexMatch.groupValues[1]
          val accName = regexMatch.groupValues[2]
          val keyword = if (accName == "ACC_TRANSITIVE") "transitive" else "-"
          builder.append("$kind%-11s $accName%-16s $value $keyword%-12s ${description.text()}\n")
        }
        builder.append("\n")
      }

      return builder.toString().replace("\n\n$", "\n")
    }

    private data class FlagInfo(
      val kind: String,
      val accName: String,
      val value: Int,
      val keyword: String?,
      val description: String
    )

    private const val HEX_BASE = 16
    private const val NUM_COLUMNS = 5
    fun code(table: String): String {
      val flagInfos = table
        .lines()
        .filter { !it.matches("\\s*#.*".toRegex()) }
        .filter { !it.matches("\\s*".toRegex()) }
        .map {
          val (kind, accName, value, keyword, description) = it.split(" +".toRegex(), NUM_COLUMNS)
          val k = if (keyword == "-") null else keyword
          val intValue = value.substring(2).toInt(HEX_BASE)
          FlagInfo(kind, accName, intValue, k, description)
        }

      val flagInfoMap = flagInfos.groupBy { it.accName }
      val flagExtensions = flagInfoMap.mapValues { it.value.map { it.kind } }
      val uniqueFlagInfos = flagInfoMap.toList().map { it.second.first() }.sortedBy { it.accName }.sortedBy { it.value }
      val flagInfoGroups = mutableMapOf<String, List<FlagInfo>>()
      for (m in flagInfos) {
        flagInfoGroups.put(m.kind, flagInfoGroups.getOrDefault(m.kind, listOf()).plus(m))
      }

      // TODO: use stringBuilder() { ... } idiom
      val builder = StringBuilder()

      builder.append("""
          |// Do not edit this file by hand.  It is generated by ${generatorName}.
          |
          |package org.ucombinator.jade.classfile
          |
          |import com.github.javaparser.ast.Modifier
          |import com.github.javaparser.ast.NodeList
          |import javax.annotation.processing.Generated
          |
          |@Generated("${generatorName}")
          |sealed interface Flag {
          |  fun value(): Int
          |  fun valueAsString(): String = "0x${'$'}{"%04x".format(value())}"
          |  fun keyword(): Modifier.Keyword?
          |  fun modifier(): Modifier? = keyword()?.let(::Modifier)
          |
          |
        """.trimMargin()
      )

      for (kind in flagInfos.map { it.kind }.distinct()) {
        builder.append("  @Generated(\"${generatorName}\") sealed interface ${kind} : Flag\n")
      }


      for (flagsInfo in uniqueFlagInfos) {
        val keyword = if (flagsInfo.keyword == null) null else "Modifier.Keyword.${flagsInfo.keyword.uppercase()}"
        val extensions = flagExtensions.getValue(flagsInfo.accName).joinToString(", ")

        builder.append("""
            |
            |  object ${flagsInfo.accName} : Flag, $extensions {
            |    override fun value() = 0x${"%04x".format(flagsInfo.value)}
            |    override fun keyword() = $keyword
            |  }
            |
          """.trimMargin()
        )
      }

      builder.append("""
          |
          |  companion object {
          |    fun toModifiers(flags: List<Flag>): NodeList<Modifier> = NodeList(flags.mapNotNull(Flag::modifier))
          |
          |    private fun <T> fromInt(mapping: List<Pair<Int, T>>): (Int) -> List<T> = { int ->
          |      val maskedInt = int and 0xffff // Ignore ASM specific flags, which occur above bit 16
          |      val result = mapping.filter { (it.first and maskedInt) != 0 }
          |      val intResult = result.fold(0) { x, y -> x or y.first }
          |      assert(maskedInt == intResult) { "flag parsing error: want 0x${'$'}{"%x".format(int)}, got 0x${'$'}{"%x".format(intResult)}" }
          |      result.map(Pair<Int, T>::second)
          |    }
          |
        """.trimMargin()
      )

      for ((kind, flagInfosForKind) in flagInfoGroups) {
        assert(flagInfosForKind.map { it.value } == flagInfosForKind.map { it.value }.distinct())
        builder.append("\n    private val ${kind}FlagMapping = listOf<Pair<Int, Flag.${kind}>>(\n")
        for (flagInfo in flagInfosForKind.sortedBy { it.value }) {
          builder.append(
            "      /*0x${"%04x".format(flagInfo.value)}*/ Flag.${flagInfo.accName}.value() " +
              "to Flag.${flagInfo.accName}, // ${flagInfo.description}\n"
          )
        }
        builder.append("    )\n")
      }

      builder.append("\n")

      for ((kind, _) in flagInfoGroups) {
        val name = "${kind.substring(0, 1).lowercase()}${kind.substring(1)}Flags"
        builder.append("    val $name: (Int) -> List<Flag.${kind}> = fromInt(${kind}FlagMapping)\n")
      }

      builder.append("  }\n")
      builder.append("}\n")

      return builder.toString()
    }
  }
}
