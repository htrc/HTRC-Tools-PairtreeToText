package org.hathitrust.htrc.tools.pairtreetotext

import java.io.File
import java.util.zip.ZipFile

import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper.PairtreeDocument
import resource._

import scala.io.Codec
import scala.util.Try
import scala.xml.{Elem, XML}

object PairtreeToText {
  protected val METS_XML_EXT = ".mets.xml"
  protected val VOL_ZIP_EXT = ".zip"

  /**
    * Retrieve the correct page sequence from METS
    *
    * @param metsXml The METS XML
    * @return The sequence of page file names
    */
  protected[pairtreetotext] def getPageSeq(metsXml: xml.Elem): Seq[String] = for {
    fileGrp <- metsXml \\ "fileGrp"
    if fileGrp.namespace == "http://www.loc.gov/METS/" && (fileGrp \ "@USE").text == "ocr"
    flocat <- fileGrp \\ "FLocat"
    if flocat.namespace == "http://www.loc.gov/METS/"
  } yield flocat.attribute("http://www.w3.org/1999/xlink", "href").get.text

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param doc The PairtreeDocument representing the volume
    * @param metsXml The METS XML document
    * @param volZip The volume ZIP structure
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The concatenated pages as text
    */
  protected def pairtreeToText(doc: PairtreeDocument, metsXml: Elem, volZip: ZipFile)
                              (implicit codec: Codec): String = {
    // extract the correct sequence of page filenames from the METS file
    val pageSeq = getPageSeq(metsXml)

    // attempt to map these page filenames to ZIP entries
    val pageZipEntries = pageSeq.map(f => s"${doc.getCleanIdWithoutLibId}/$f").map(volZip.getEntry)

    // check for inconsistencies between the METS page sequence and ZIP contents
    // and throw exception if any are found
    val missingPages = pageSeq.zip(pageZipEntries).withFilter(_._2 == null).map(_._1)
    if (missingPages.nonEmpty)
      throw MalformedVolumeException(s"[${doc.getUncleanId}] " +
        s"Missing page entries in volume ZIP file: " + missingPages.mkString(", "), null)

    // calculate the size of the text content for all the pages, and create a buffer
    val uncompressedTextSize = pageZipEntries.foldLeft(0)((acc, ze) => acc + ze.getSize.toInt)
    val volTextBuilder = new StringBuilder(uncompressedTextSize)

    // iterate over the page ZIP entries and extract the page text into the buffer
    for (pageZipEntry <- pageZipEntries) {
      for (pageStream <- managed(io.Source.fromInputStream(volZip.getInputStream(pageZipEntry)))) {
        volTextBuilder.append(pageStream.mkString)
      }
    }

    volTextBuilder.toString()
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param metsXmlFile The METS file describing the volume (and its page ordering)
    * @param volZipFile The volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(pairtreeDoc: PairtreeDocument, metsXmlFile: File, volZipFile: File)
                    (implicit codec: Codec): Try[String] = Try {
    // load the METS XML and ZIP files
    val metsXml = XML.loadFile(metsXmlFile)
    val volZip = new ZipFile(volZipFile, codec.charSet)

    // return the extracted text
    pairtreeToText(pairtreeDoc, metsXml, volZip)
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param volZipFile The volume ZIP file
    * @param metsFileExt The file extension of the associated METS XML file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(volZipFile: File, metsFileExt: String = METS_XML_EXT)
                    (implicit codec: Codec): Try[String] = Try {
    val pairtreeDoc = PairtreeHelper.parse(volZipFile)
    val metsXmlFile =
      new File(volZipFile.getParentFile, s"${pairtreeDoc.getCleanIdWithoutLibId}$metsFileExt")

    pairtreeToText(pairtreeDoc, metsXmlFile, volZipFile).get
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param htid The clean or unclean HT volume ID
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding
    *                         volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId True if `htid` represents a 'clean' ID, False otherwise
    *                  (assumed False if missing)
    * @param metsFileExt The file extension of the associated METS XML file
    * @param volFileExt The file extension of the volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(htid: String, pairtreeRootPath: File, isCleanId: Boolean = false,
                     metsFileExt: String = METS_XML_EXT, volFileExt: String = VOL_ZIP_EXT)
                    (implicit codec: Codec): Try[String] = Try {
    val pairtreeDoc =
      if (isCleanId) PairtreeHelper.getDocFromCleanId(htid)
      else PairtreeHelper.getDocFromUncleanId(htid)

    pairtreeToText(pairtreeDoc, pairtreeRootPath, metsFileExt, volFileExt).get
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding
    *                         volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param metsFileExt The file extension of the associated METS XML file
    * @param volFileExt The file extension of the volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(pairtreeDoc: PairtreeDocument, pairtreeRootPath: File,
                     metsFileExt: String = METS_XML_EXT, volFileExt: String = VOL_ZIP_EXT)
                    (implicit codec: Codec): Try[String] = Try {
    val volRootPath = new File(pairtreeRootPath, pairtreeDoc.getDocumentRootPath)
    val metsXmlFile = new File(volRootPath, s"${pairtreeDoc.getCleanIdWithoutLibId}$metsFileExt")
    val volZipFile = new File(volRootPath, s"${pairtreeDoc.getCleanIdWithoutLibId}$volFileExt")

    pairtreeToText(pairtreeDoc, metsXmlFile, volZipFile).get
  }
}