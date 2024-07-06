package org.ucombinator.jade.util

import java.io.PrintWriter
import java.io.StringWriter

/** TODO:doc. */
object Exceptions {
  /** TODO:doc.
   *
   * @param exception TODO:doc
   * @return TODO:doc
   */
  fun causes(exception: Throwable?): List<Throwable> =
    if (exception == null) emptyList() else listOf(exception) + causes(exception.cause)

  /** TODO:doc.
   *
   * @param exception TODO:doc
   * @return TODO:doc
   */
  fun name(exception: Throwable): String =
    causes(exception).joinToString(":") { it::class.qualifiedName ?: "<anonymous>" }

  /** TODO:doc.
   *
   * @param exception TODO:doc
   * @return TODO:doc
   */
  fun stackTrace(exception: Throwable): String {
    // TODO: buildString idiom
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    exception.printStackTrace(printWriter)
    printWriter.flush()
    return stringWriter.toString()
  }

  /** TODO:doc.
   *
   * @param exception TODO:doc
   * @param classes TODO:doc
   * @return TODO:doc
   */
  fun skip(exception: Throwable?, vararg classes: kotlin.reflect.KClass<*>): Throwable? =
    if (exception == null || exception::class in classes) exception else skip(exception.cause, *classes)

  /** TODO:doc.
   *
   * @param exception TODO:doc
   * @param classes TODO:doc
   * @return TODO:doc
   */
  fun isClasses(exception: Throwable?, vararg classes: kotlin.reflect.KClass<*>): Boolean =
    classes.toList() == causes(exception).map { it::class }
}
