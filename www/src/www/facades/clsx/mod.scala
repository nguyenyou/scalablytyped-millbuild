package www.facades.clsx

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport}

object mod {
    @js.native
    @JSImport("clsx", JSImport.Namespace)
    object ^ extends js.Object

    // type ClassDictionary = js.Record[String, Any]

    def clsx(inputs: js.Any*): String = ^.asInstanceOf[js.Dynamic].applyDynamic("clsx")(inputs.asInstanceOf[Seq[js.Any]]*).asInstanceOf[String]

}