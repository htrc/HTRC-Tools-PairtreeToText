package org.hathitrust.htrc.tools.pairtreetotext

import java.io.{File, FileNotFoundException}
import java.nio.file.{Files, Path}
import javax.xml.parsers.DocumentBuilderFactory

import org.slf4j.{Logger, LoggerFactory}
import org.w3c.dom.Document
import resource._

import scala.io.Codec

object Helper {
  @transient lazy val logger: Logger = LoggerFactory.getLogger(Main.appName)

  /**
    * Loads a file as an XML document
    *
    * @param f The file
    * @return The XML document
    */
  def loadXml(f: File): Document = {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    val documentBuilder = factory.newDocumentBuilder()
    documentBuilder.parse(f)
  }

  /**
    * Checks if the given files exist and throw FileNotFoundException if any are missing
    *
    * @param files The files to check
    */
  def checkFilesExist(files: File*): Unit =
    files.filterNot(_.exists()).foreach(f =>
      throw new FileNotFoundException(f.toString + " (No such file or directory)")
    )

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
