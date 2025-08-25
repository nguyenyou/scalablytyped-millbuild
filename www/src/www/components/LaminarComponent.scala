package www.components

import com.raquo.laminar.api.L.*

trait LaminarComponent extends UIComponent {

  def render(): HtmlElement

  def apply(): HtmlElement = {
    modifiers(render())
  }

}
