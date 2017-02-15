package org.hathitrust.htrc.tools.pairtreetotext

object TextOptions extends Enumeration {
  type TextOptions = Value
  val BodyOnly, TrimLines, RemoveEmptyLines, FixHyphenation, ParaLines = Value
}