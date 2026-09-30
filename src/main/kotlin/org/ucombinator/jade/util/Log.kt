package org.ucombinator.jade.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.CallerData
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.pattern.color.ANSIConstants
import io.github.oshai.kotlinlogging.KLogger // TODO: consider other logger systems
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter as OldHighlightingCompositeConverter
import org.slf4j.Logger as Slf4jLogger

/** Logging helper. */
object Log {
  /** Names of loggers created through this object, including command-line overrides. */
  private val names = ConcurrentHashMap.newKeySet<String>()

  /**
   * Creates a logger whose name is inferred from the call site.
   *
   * Passing the lambda reference lets kotlin-logging derive the enclosing class name.
   *
   * @param func a lambda at the call site used to infer the logger name.
   * @return the logger for the call site's enclosing class.
   */
  operator fun invoke(func: () -> Unit): KLogger = KotlinLogging.logger(func).also { names += it.name }

  /**
   * Creates a named child logger below the logger inferred from the call site.
   *
   * @param name the child component name, such as `cfg` or `ssa.frames`.
   * @param func a lambda at the call site used to infer the parent logger name.
   * @return the child logger.
   */
  operator fun invoke(name: String, func: () -> Unit): KLogger =
    KotlinLogging.logger("${KotlinLogging.logger(func).name}.${name}").also {
      names += it.name
    }

  /**
   * Gets a Logback logger by name.
   *
   * An empty name refers to Logback's root logger. Other names are passed through unchanged.
   * Callers are responsible for qualifying relative names first.
   *
   * @param name the Logback logger name, or empty for the root logger
   * @return the configured Logback logger
   */
  fun getLog(name: String): LogbackLogger {
    val modifiedName = if (name.isEmpty()) Slf4jLogger.ROOT_LOGGER_NAME else name
    names += modifiedName
    return LoggerFactory.getLogger(modifiedName) as LogbackLogger
  }

  private const val FILE_APPENDER_NAME = "FILE"
  private const val FILE_PATTERN = "%-5level %logger{org.ucombinator.jade.}: %message%n%caller"
  private val RUN_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS")

  /** Creates and attaches a uniquely named file appender to the root logger. */
  fun enableFileAppender() {
    val root = getLog("")
    if (root.getAppender(FILE_APPENDER_NAME) != null) return

    val context = root.loggerContext
    val encoder = PatternLayoutEncoder().apply {
      this.context = context
      pattern = FILE_PATTERN
      start()
    }
    val appender = FileAppender<ILoggingEvent>().apply {
      this.context = context
      name = FILE_APPENDER_NAME
      file = "logs/${RUN_TIMESTAMP_FORMAT.format(LocalDateTime.now())}.log"
      // append = false
      this.encoder = encoder
      start()
    }
    root.addAppender(appender)
  }

  /**
   * Returns known loggers, optionally including every logger in the Logback context.
   *
   * By defauly, logger names are registered when Jade creates a logger or applies a command-line
   * override. Includes initialized loggers from dependencies and their hierarchical parent loggers
   * if `all` is true.
   *
   * @param all whether to include all loggers currently known to Logback.
   * @return known loggers sorted by name.
   */
  fun loggers(all: Boolean = false): List<LogbackLogger> {
    // TODO: This logic only loads loggers from static initialized classes;
    // This should check for other loggers that may not have been initialized.
    if (all) {
      return (LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME) as LogbackLogger)
        .loggerContext
        .loggerList
        .sortedBy { it.name }
    }

    return names.plus(Slf4jLogger.ROOT_LOGGER_NAME).sorted().map {
      LoggerFactory.getLogger(it) as LogbackLogger
    }
  }
}

/** Formats logger names relative to a configured package prefix. */
class RelativeLoggerConverter : ClassicConverter() {
  /** The package prefix supplied as the single conversion-word option. */
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

/** Adds selected caller stack frames to each log message. */
class DynamicCallerConverter : ClassicConverter() {
  companion object {
    private var depthStart = 0
    private var configuredDepthEnd = 0

    /** Exclusive callerData index at which to stop. */
    val depthEnd: Int get() = configuredDepthEnd

    /**
     * Sets the callerData index at which to stop.
     *
     * @param value the target depth end.
     */
    fun setDepthEnd(value: Int) {
      configuredDepthEnd = value
    }
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

/** Applies Jade's console colors to INFO, DEBUG, and TRACE messages. */
class HighlightingCompositeConverter : OldHighlightingCompositeConverter() {
  protected override fun getForegroundColorCode(event: ILoggingEvent): String =
    when (event.level.toInt()) {
      Level.INFO_INT -> ANSIConstants.GREEN_FG
      Level.DEBUG_INT -> ANSIConstants.CYAN_FG
      Level.TRACE_INT -> ANSIConstants.MAGENTA_FG
      else -> super.getForegroundColorCode(event)
    }
}
