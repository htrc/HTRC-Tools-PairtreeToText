package org.hathitrust.htrc.tools.pairtreetotext

import java.nio.file.{Files, Path}

import org.slf4j.{Logger, LoggerFactory}
import resource._

import scala.io.Codec

object Helper {
  @transient lazy val logger: Logger = LoggerFactory.getLogger(Main.appName)

  /**
    * Writes text to a file
    *
    * @param text  The text
    * @param path  The file path
    * @param codec The codec to use (implicit)
    */
  def writeTextFile(text: String, path: Path)(implicit codec: Codec): Unit =
    for (f <- managed(Files.newBufferedWriter(path, codec.charSet)))
      f.write(text)
}
