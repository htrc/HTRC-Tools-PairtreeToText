package org.hathitrust.htrc.tools.pairtreetotext

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

import org.w3c.dom.Document

object Utils {
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
}


