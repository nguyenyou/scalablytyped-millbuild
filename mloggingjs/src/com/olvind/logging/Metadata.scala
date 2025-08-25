package com.olvind.logging

import sourcecode.{Enclosing, File, Line}

final class Metadata(
    val instant:   JsInstant,
    val logLevel:  LogLevel,
    val line:      Line,
    val file:      File,
    val enclosing: Enclosing,
)
