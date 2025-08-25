package www.components

import scala.scalajs.LinkingInfo.developmentMode

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.{IntAsStringCodec, StringAsIsCodec}

trait Locator(
  using
  n: sourcecode.FileName,
  f: sourcecode.File,
  l: sourcecode.Line
) {

  def modifiers(el: HtmlElement): HtmlElement = {
    if (developmentMode) {
      el.amend(
        Locator.scalaFileName := n.value,
        Locator.scalaSourcePath := f.value,
        Locator.scalaLineNumber := l.value,
        dataAttr("scala") := s"${n.value}:${l.value}"
      )
    }
    el
  }

  def componentName: String = {
    if (developmentMode) {
      f.value
    } else {
      n.value
    }

  }

}

object Locator {

  private lazy val scalaSourcePath =
    htmlProp("__scalasourcepath", StringAsIsCodec)

  private lazy val scalaFileName =
    htmlProp("__scalafilename", StringAsIsCodec)

  private lazy val scalaLineNumber =
    htmlProp("__scalasourceline", IntAsStringCodec)

}
