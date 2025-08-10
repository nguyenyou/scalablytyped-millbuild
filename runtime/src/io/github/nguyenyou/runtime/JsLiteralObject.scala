package io.github.nguyenyou.runtime

import scala.scalajs.js

trait JsLiteralObject extends js.Object

object JsLiteralObject {
  def apply(): JsLiteralObject = js.Dynamic.literal().asInstanceOf[JsLiteralObject]

   /**
   * Mutate the current object and set an arbitrary member on it.
   *
   * This is after all allowed in Javascript, and in ScalablyTyped it forms the basis of the
   *  mutable builder encoding.
   */
  @inline
  def set[Self <: js.Any](x: Self, key: String, value: Any): Self = {
    x.asInstanceOf[js.Dynamic].updateDynamic(key)(value.asInstanceOf[js.Any])
    x
  }
}