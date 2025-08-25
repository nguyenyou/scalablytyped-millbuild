package com.olvind

import com.olvind.logging.Logger.{AppendableLogger, Stored, StoringLogger, WriterLogger}
import fansi.Str

package object logging {
  type Ctx = Map[Str, Str]

  private[logging] val emptyContext: Ctx = Map.empty

  def console: Logger[Unit] =
    appendable(JsConsole).void

  def appendable[A <: JsAppendable](
      appendable: A,
      pattern:    Pattern = Pattern.default,
      ctx:        Ctx = emptyContext,
  ): Logger[A] =
    new AppendableLogger(appendable, pattern, ctx)

  def writer[W <: JsWriter](
      writer:  W,
      pattern: Pattern = Pattern.default,
      ctx:     Ctx     = emptyContext,
  ): Logger[W] =
    new WriterLogger(new AppendableLogger(writer, pattern, ctx))

  def stringWriter(pattern: Pattern = Pattern.default, ctx: Ctx = emptyContext): Logger[JsStringWriter] =
    writer(new JsStringWriter(), pattern, ctx)

  def storing(ctx: Ctx = emptyContext): Logger[Array[Stored]] =
    new StoringLogger(new Logger.Store, ctx)
}
