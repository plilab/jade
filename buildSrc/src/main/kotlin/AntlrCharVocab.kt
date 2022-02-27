package org.ucombinator.antlr

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.antlr.AntlrTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

open class AntlrCharVocab
    @Inject constructor(private val antlr: TaskProvider<AntlrTask>) : DefaultTask() {

    // init {
    //     antlr.get().dependsOn(this) // TODO: doesn't work for some reason
    // }

    @TaskAction fun run() {
        val dir = antlr.get().outputDirectory
        dir.mkdirs()
        val file = File(dir, "Character.tokens")

        file.bufferedWriter().use { w ->
            w.write("'\\0'=${0x0000}\n")
            w.write("'\\b'=${0x0008}\n")
            w.write("'\\t'=${0x0009}\n")
            w.write("'\\n'=${0x000a}\n")
            w.write("'\\f'=${0x000c}\n")
            w.write("'\\r'=${0x000d}\n")
            w.write("'\\\"'=${0x0022}\n")
            w.write("'\\''=${0x0027}\n")
            w.write("'\\\\'=${0x005c}\n")
            for (i in 0..127) {
                w.write("CHAR_x${"%02X".format(i)}=$i\n")
                w.write("CHAR_u${"%04X".format(i)}=$i\n")
                if (i >= 32 && i <= 126 && i.toChar() != '\'' && i.toChar() != '\\') {
                    w.write("'${i.toChar()}'=$i\n")
                }
            }
        }
    }
}
