package com.olvind.logging

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * Scala.js compatibility layer for mlogging.
 * Replaces JVM-specific APIs with JavaScript-compatible equivalents.
 */

/**
 * Replacement for java.time.Instant that works in Scala.js
 */
case class JsInstant(millis: Double) {
  override def toString: String = {
    val date = new js.Date(millis)
    date.toISOString()
  }
}

object JsInstant {
  def now: JsInstant = JsInstant(js.Date.now())
}

/**
 * Replacement for java.io.Appendable that works in Scala.js
 */
trait JsAppendable {
  def append(s: String): Unit
}

/**
 * Replacement for java.io.Writer that works in Scala.js
 */
trait JsWriter extends JsAppendable {
  def flush(): Unit
}

/**
 * Replacement for java.io.StringWriter that works in Scala.js
 */
class JsStringWriter extends JsWriter {
  private val buffer = new StringBuilder()
  
  def append(s: String): Unit = {
    buffer.append(s)
    ()
  }
  
  def flush(): Unit = ()
  
  override def toString: String = buffer.toString()
}

/**
 * Console output that works in Scala.js
 */
object JsConsole extends JsAppendable {
  def append(s: String): Unit = {
    js.Dynamic.global.console.log(s)
    ()
  }
}

/**
 * Utility functions for Scala.js compatibility
 */
object JsUtils {
  /**
   * Extract filename from a path (replacement for java.io.File.getName)
   */
  def getFileName(path: String): String = {
    val lastSlash = math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'))
    if (lastSlash >= 0) path.substring(lastSlash + 1) else path
  }
  
  /**
   * Format throwable for logging (replacement for printStackTrace)
   */
  def formatThrowable(th: Throwable): String = {
    val message = Option(th.getMessage).getOrElse("")
    val className = th.getClass.getName
    if (message.nonEmpty) s"$className: $message" else className
  }
}
