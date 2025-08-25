package org.scalablytyped.converter.internal

/* Borrowed from org/apache/commons/lang/StringEscapeUtils.java - Scala.js compatible version */
object EscapeStrings {
  def java(str: String): String = {
    val sb = new StringBuilder(str.length * 2)
    go(sb, str, escapeSingleQuote = false, escapeForwardSlash = false)
    sb.toString
  }

  def javaScript(str: String): String = {
    val sb = new StringBuilder(str.length * 2)
    go(sb, str, escapeSingleQuote = true, escapeForwardSlash = true)
    sb.toString
  }

  /**
    * @param out                StringBuilder to receive the escaped string
    * @param str                String to escape values in, may be null
    * @param escapeSingleQuote  escapes single quotes if <code>true</code>
    */
  private def go(out: StringBuilder, str: String, escapeSingleQuote: Boolean, escapeForwardSlash: Boolean): Unit = {
    var i = 0
    while (i < str.length) {
      val ch = str.charAt(i)

      // handle unicode
      if (ch > 0xfff) out.append("\\u").append(hex(ch))
      else if (ch > 0xff) out.append("\\u0").append(hex(ch))
      else if (ch > 0x7f) out.append("\\u00").append(hex(ch))
      else if (ch < 32) ch match {
        case '\b' =>
          out.append('\\')
          out.append('b')
        case '\n' =>
          out.append('\\')
          out.append('n')
        case '\t' =>
          out.append('\\')
          out.append('t')
        case '\f' =>
          out.append('\\')
          out.append('f')
        case '\r' =>
          out.append('\\')
          out.append('r')
        case _ =>
          if (ch > 0xf) out.append("\\u00").append(hex(ch))
          else out.append("\\u000").append(hex(ch))
      }
      else
        ch match {
          case '\'' =>
            if (escapeSingleQuote) out.append('\\')
            out.append('\'')
          case '"' =>
            out.append('\\')
            out.append('"')
          case '\\' =>
            out.append('\\')
            out.append('\\')
          case '/' =>
            if (escapeForwardSlash) out.append('\\')
            out.append('/')
          case _ =>
            out.append(ch)
        }

      i += 1
    }
  }

  /**
    * <p>Returns an upper case hexadecimal <code>String</code> for the given
    * character.</p>
    *
    * @param ch The character to convert.
    * @return An upper case hexadecimal <code>String</code>
    */
  def hex(ch: Char): String =
    ch.toInt.toHexString.toUpperCase
}
