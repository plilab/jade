package org.ucombinator.jade.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.CallerData
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.pattern.color.ANSIConstants
import io.github.oshai.kotlinlogging.KLogger // TODO: consider other logger systems
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import org.ucombinator.jade.main.Main

import java.util.jar.JarFile

import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter as OldHighlightingCompositeConverter
import org.slf4j.Logger as Slf4jLogger

/** TODO:doc. */
object Log {
  private val log = Log {} // TODO: lazy?

  /** TODO:doc.
   *
   * @param func TODO:doc
   * @return TODO:doc
   */
  operator fun invoke(func: () -> Unit): KLogger = KotlinLogging.logger(func)

  /** TODO:doc.
   *
   * @param name TODO:doc
   * @return TODO:doc
   */
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

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun loggers(): List<LogbackLogger> {
    // See https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
    // Note: toURI is required in order to handle special characters
    val jar = java.io.File(Main::class.java.protectionDomain.codeSource.location.toURI()).path
    log.debug { "jar: $jar" }

    for (entry in JarFile(jar).entries()) {
      if (entry.name.endsWith(".class")) {
        try {
          // TODO: classLoader load class
          Class.forName(entry.name.replace("\\.class$", "").replace("/", "."))
        } catch (e: Throwable) {
          log.debug { "skipping: ${entry.name} ${e}" } // TODO: show exception in message
        }
      }
    }

    return (LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME) as LogbackLogger).loggerContext.loggerList
  }
}

/** TODO:doc. */
class RelativeLoggerConverter : ClassicConverter() {
  /** TODO:doc. */
  private val prefix: String by lazy {
    val options = this.optionList
    check(options != null) { "Options not set" }
    check(options.size == 1) { "Expected exactly one option but got: ${options}" }
    options[0]
  }

  override fun convert(event: ILoggingEvent): String {
    val name = event.loggerName
    return if (name.startsWith(prefix)) name.removePrefix(prefix) else ".$name"
  }
}

/** TODO:doc. */
class DynamicCallerConverter : ClassicConverter() {
  companion object {
    /** TODO:doc. */
    var depthStart = 0 // TODO: maybe make private

    /** TODO:doc. */
    var depthEnd = 0 // TODO: maybe make private
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

/** TODO:doc. */
class HighlightingCompositeConverter : OldHighlightingCompositeConverter() {
  protected override fun getForegroundColorCode(event: ILoggingEvent): String =
    when (event.level.toInt()) {
      Level.INFO_INT -> ANSIConstants.GREEN_FG
      Level.DEBUG_INT -> ANSIConstants.CYAN_FG
      Level.TRACE_INT -> ANSIConstants.MAGENTA_FG
      else -> super.getForegroundColorCode(event)
    }
}
