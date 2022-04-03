package org.ucombinator.jade.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.CallerData
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.pattern.color.ANSIConstants
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.ucombinator.jade.main.Jade
import java.util.jar.JarFile
import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter as OldHighlightingCompositeConverter
import org.slf4j.Logger as Slf4jLogger

object Log {
  operator fun invoke(func: () -> Unit) = KotlinLogging.logger(func)
  private val log = Log {}
  const val PREFIX = "org.ucombinator.jade." // TODO: autodetect
  fun getLog(name: String): LogbackLogger {
    val modifiedName = if (name.isEmpty()) Slf4jLogger.ROOT_LOGGER_NAME else name
    return LoggerFactory.getLogger(modifiedName) as LogbackLogger
  }

  // val name = getClass.getName
  //   .replace('$', '.')
  //   .replace("..", ".")
  //   .replaceAll(".$", "")
  //   ScalaLogger(LoggerFactory.getLogger(name))
  // }
  // fun childLog(name: String): ScalaLogger = {
  //   ScalaLogger(LoggerFactory.getLogger(log.underlying.getName + "." + name))
  // }

//   def getLog(name: String): LogbackLogger = {
//     val modifiedName =
//       if (name.isEmpty) { Slf4jLogger.ROOT_LOGGER_NAME }
//       else { name }
//     LoggerFactory.getLogger(modifiedName).asInstanceOf[LogbackLogger]
//   }

  fun listLoggers() {
    // See https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
    // Note: toURI is required in order to handle special characters
    val jar = java.io.File(Jade::class.java.protectionDomain.codeSource.location.toURI()).path
    this.log.debug("jar: $jar")

    for (entry in JarFile(jar).entries()) {
      if (entry.name.endsWith(".class")) {
        try {
          Class.forName(entry.name.replace("\\.class$", "").replace("/", "."))
        } catch (_: Throwable) {
          this.log.debug("skipping: ${entry.name}") // TODO: show exception in message
        }
      }
    }

    for (l in (LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME)as LogbackLogger).loggerContext.loggerList) {
      println(l.name)
    }
  }
}

class RelativeLoggerConverter : ClassicConverter() {
  lateinit var prefix: String

  // override fun start(): Unit {
  override fun start() {
    val x = getOptionList()
    assert(x.size == 1)
    prefix = x[0]
    super.start()
  }

  override fun convert(event: ILoggingEvent): String {
    val name = event.loggerName
    return if (name.startsWith(prefix)) name.removePrefix(prefix) else ".$name"
  }
}

class DynamicCallerConverter : ClassicConverter() {
  companion object {
    var depthStart = 0
    var depthEnd = 0
  }

  override fun convert(event: ILoggingEvent): String {
    var buf = StringBuilder()
    var cda = event.callerData
    if (cda != null && cda.size > depthStart) {
      val limit = if (depthEnd < cda.size) depthEnd else cda.size

      for (i in depthStart until limit) {
        buf.append("Caller+")
        buf.append(i)
        buf.append("\t at ")
        buf.append(cda[i])
        buf.append(CoreConstants.LINE_SEPARATOR)
      }
      return buf.toString()
    } else {
      return CallerData.CALLER_DATA_NA
    }
  }
}

class HighlightingCompositeConverter : OldHighlightingCompositeConverter() {
  protected override fun getForegroundColorCode(event: ILoggingEvent): String =
    when (event.level.toInt()) {
      Level.INFO_INT -> ANSIConstants.GREEN_FG
      Level.DEBUG_INT -> ANSIConstants.CYAN_FG
      Level.TRACE_INT -> ANSIConstants.MAGENTA_FG
      else -> super.getForegroundColorCode(event)
    }
}
