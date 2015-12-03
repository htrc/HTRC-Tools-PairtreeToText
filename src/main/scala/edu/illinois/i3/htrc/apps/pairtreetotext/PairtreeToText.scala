package edu.illinois.i3.htrc.apps.pairtreetotext

import java.io.File
import java.util.zip.ZipFile
import edu.illinois.i3.htrc.tools.PairtreeHelper
import edu.illinois.i3.htrc.tools.PairtreeHelper.PairtreeDocument
import resource._

import scala.io.Codec
import scala.xml.XML

trait PairtreeToText {
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param metsXmlFile The METS file describing the volume (and its page ordering)
    * @param volZipFile The volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(metsXmlFile: File, volZipFile: File)(implicit codec: Codec): (PairtreeDocument, String)

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param volZipFile The volume ZIP file
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(volZipFile: File): (PairtreeDocument, String)

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param htid The clean or unclean HT volume ID
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId True if `htid` represents a 'clean' ID, False otherwise (assumed False if missing)
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(htid: String, pairtreeRootPath: File, isCleanId: Boolean = false): (PairtreeDocument, String)

  /**
    * Retrieve the correct page sequence from METS
    *
    * @param metsXml The METS XML
    * @return The sequence of page file names
    */
  def getPageSeq(metsXml: xml.Elem): Seq[String]
}


object HTRCPairtreeToText extends PairtreeToText {
  /**
    * Retrieve the correct page sequence from METS
    *
    * @param metsXml The METS XML
    * @return The sequence of page file names
    */
  def getPageSeq(metsXml: xml.Elem): Seq[String] = for {
    fileGrp <- metsXml \\ "fileGrp"
    if fileGrp.namespace == "http://www.loc.gov/METS/" && (fileGrp \ "@USE").text == "ocr"
    flocat <- fileGrp \\ "FLocat"
    if flocat.namespace == "http://www.loc.gov/METS/"
  } yield flocat.attribute("http://www.w3.org/1999/xlink", "href").get.text

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param metsXmlFile The METS file describing the volume (and its page ordering)
    * @param volZipFile The volume ZIP file
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(metsXmlFile: File, volZipFile: File)(implicit codec: Codec): (PairtreeDocument, String) = {
    val metsXml = XML.loadFile(metsXmlFile)
    val pairtreeDoc = PairtreeHelper.parse(volZipFile)
    val volZip = new ZipFile(volZipFile, codec.charSet)
    val pageZipEntries = getPageSeq(metsXml).map(f => s"${pairtreeDoc.getCleanIdWithoutLibId}/$f").map(volZip.getEntry)
    val uncompressedTextSize = pageZipEntries.foldLeft(0)((acc, ze) => acc + ze.getSize.toInt)
    val volTextBuilder = new StringBuilder(uncompressedTextSize)
    for (pageZipEntry <- pageZipEntries) {
      for (pageStream <- managed(io.Source.fromInputStream(volZip.getInputStream(pageZipEntry)))) {
        volTextBuilder.append(pageStream.mkString)
      }
    }

    val volTxt = volTextBuilder.toString()

    pairtreeDoc -> volTxt
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param volZipFile The volume ZIP file
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(volZipFile: File): (PairtreeDocument, String) = {
    val pairtreeDoc = PairtreeHelper.parse(volZipFile)
    val metsXmlFile = new File(volZipFile.getParentFile, s"${pairtreeDoc.getCleanIdWithoutLibId}.mets.xml")
    pairtreeToText(metsXmlFile, volZipFile)
  }

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param htid The clean or unclean HT volume ID
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId True if `htid` represents a 'clean' ID, False otherwise (assumed False if missing)
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(htid: String, pairtreeRootPath: File, isCleanId: Boolean = false): (PairtreeDocument, String) = {
    val ppath = if (isCleanId) PairtreeHelper.getPathFromCleanId(htid) else PairtreeHelper.getPathFromUncleanId(htid)
    val volRootPath = new File(pairtreeRootPath, ppath)
    val id = volRootPath.getName
    val metsXmlFile = new File(volRootPath, s"$id.mets.xml")
    val volZipFile = new File(volRootPath, s"$id.zip")
    pairtreeToText(metsXmlFile, volZipFile)
  }

}