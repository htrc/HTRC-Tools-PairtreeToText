package org.hathitrust.htrc.tools.pairtreetotext

import org.slf4j.{Logger, LoggerFactory}

import java.nio.file.{Files, Path}
import scala.io.Codec
import scala.util.Using

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
    Using.resource(Files.newBufferedWriter(path, codec.charSet))(_.write(text))
}
