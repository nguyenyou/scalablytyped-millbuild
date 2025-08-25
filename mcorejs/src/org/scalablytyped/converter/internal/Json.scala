package org.scalablytyped.converter.internal

import io.circe.{Decoder, Encoder, Error, Json => CirceJson}
import io.circe.parser._
import io.circe.syntax._

/**
 * Scala.js compatible JSON utilities.
 * This version removes JVM-specific file I/O operations and focuses on 
 * string-based JSON parsing and encoding that works in browser environments.
 */
object Json {

  def force[T: Decoder](jsonString: String): T =
    apply[T](jsonString) match {
      case Left(error) => sys.error(s"Error while parsing JSON: $error")
      case Right(t)    => t
    }

  def apply[T: Decoder](jsonString: String): Either[Error, T] = {
    val str = jsonString match {
      case withBom if withBom.startsWith(BOM) => withBom.replace(BOM, "")
      case ok                                 => ok
    }
    parse(str).flatMap(_.as[T])
  }

  def encode[T: Encoder](value: T): String =
    value.asJson.noSpaces

  def encodePretty[T: Encoder](value: T): String =
    value.asJson.spaces2

  def parseJson(jsonString: String): Either[Error, CirceJson] = {
    val str = jsonString match {
      case withBom if withBom.startsWith(BOM) => withBom.replace(BOM, "")
      case ok                                 => ok
    }
    parse(str)
  }

  private val BOM = "\uFEFF"
}
