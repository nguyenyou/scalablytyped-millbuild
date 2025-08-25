package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*

case class App() {

  val dts =
    s"""|declare namespace clsx {
        |    type ClassValue = ClassArray | ClassDictionary | string | number | bigint | null | boolean | undefined;
        |    type ClassDictionary = Record<string, any>;
        |    type ClassArray = ClassValue[];
        |    function clsx(...inputs: ClassValue[]): string;
        |}
        |declare function clsx(...inputs: clsx.ClassValue[]): string;
        |export = clsx;
        """.stripMargin
  def apply() = {
    div(
      cls("w-screen h-screen"),
      div(
        pre(dts)
      ),
      div(
        "ast"
      )
    )
  }
}
