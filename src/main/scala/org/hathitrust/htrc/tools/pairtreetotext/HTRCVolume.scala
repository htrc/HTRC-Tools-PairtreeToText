package org.hathitrust.htrc.tools.pairtreetotext

import java.io.File
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipFile
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.{XPathConstants, XPathFactory}

import org.hathitrust.htrc.textprocessing.runningheaders.{Page, PageStructureParser, PageWithStructure}
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper.PairtreeDocument
import org.hathitrust.htrc.tools.pairtreetotext.TextOptions._
import org.hathitrust.htrc.tools.scala.implicits.CollectionsImplicits._
import org.w3c.dom.{Document, Element, NodeList}
import resource._

import scala.io.Codec
import scala.util.matching.Regex

object HTRCVolume {
  protected val METS_XML_EXT = ".mets.xml"
  protected val VOL_ZIP_EXT = ".zip"

  protected val HyphenWordRegex: Regex = """(\S*\p{L})-\n(\p{L}\S*)\s?""".r
  protected val HyphenLeftRegex: Regex = """(\S*\p{L})-$""".r
  protected val HyphenRightRegex: Regex = """^(\p{L}\S*)\s?""".r
  protected val EndParagraphPunct = Set('.', '?', '!')

  /**
    * Dereference a Pairtree document and return the document's ZIP and METS XML file
    *
    * @param pairtreeDoc  The PairtreeDocument representing the volume
    * @param pairtreeRoot The folder representing the Pairtree root structure
    * @return A tuple containing the ZIP and METS XML files
    */
  protected def pairtreeDocToFiles(pairtreeDoc: PairtreeDocument,
                                   pairtreeRoot: String): (File, File) = {
    val cleanIdWithoutLibId = pairtreeDoc.getCleanIdWithoutLibId
    val volPath = new File(pairtreeRoot, pairtreeDoc.getDocumentRootPath)
    val zipFile = new File(volPath, cleanIdWithoutLibId + VOL_ZIP_EXT)
    val metsFile = new File(volPath, cleanIdWithoutLibId + METS_XML_EXT)

    (zipFile, metsFile)
  }

  /**
    * Loads a HTRC volume
    *
    * @param htid         The clean or unclean HT volume ID
    * @param pairtreeRoot The root of the pairtree folder structure;<br>
    *                     for example for a volume ID mdp.39015039688257, the corresponding
    *                     volume ZIP file is:<br>
    *                     [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId    True if `htid` represents a 'clean' ID, False otherwise
    *                     (assumed False if missing)
    * @param codec        The codec to use for decoding the text (implicit)
    * @return The HTRCVolume
    */
  def apply(htid: String, pairtreeRoot: String, isCleanId: Boolean = false)
           (implicit codec: Codec): HTRCVolume = {
    val pairtreeDoc =
      if (isCleanId) PairtreeHelper.getDocFromCleanId(htid)
      else PairtreeHelper.getDocFromUncleanId(htid)

    HTRCVolume(pairtreeDoc, pairtreeRoot)
  }

  /**
    * Loads a HTRC volume
    *
    * @param pairtreeDoc  The HTRC volume Pairtree reference
    * @param pairtreeRoot The folder representing the Pairtree root structure
    * @param codec        The codec to use for decoding the text (implicit)
    * @return The HTRCVolume
    */
  def apply(pairtreeDoc: PairtreeDocument, pairtreeRoot: String)
           (implicit codec: Codec): HTRCVolume = {
    val (zipFile, metsFile) = pairtreeDocToFiles(pairtreeDoc, pairtreeRoot)

    HTRCVolume(pairtreeDoc, metsFile, zipFile)
  }

  /**
    * Loads a HTRC volume
    *
    * @param volZipPath The path to the volume ZIP file
    * @param codec      The codec to use for decoding the text (implicit)
    * @return The HTRCVolume
    */
  def apply(volZipPath: String)(implicit codec: Codec): HTRCVolume = {
    val zipFile = new File(volZipPath)
    val pairtreeDoc = PairtreeHelper.parse(zipFile)
    val metsFile =
      new File(zipFile.getParentFile, pairtreeDoc.getCleanIdWithoutLibId + METS_XML_EXT)

    HTRCVolume(pairtreeDoc, metsFile, zipFile)
  }

  /**
    * Loads a HTRC volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param metsFile    The METS file describing the volume (and its page ordering)
    * @param zipFile     The volume ZIP file
    * @param codec       The codec to use for decoding the text (implicit)
    * @return The HTRCVolume
    */
  def apply(pairtreeDoc: PairtreeDocument, metsFile: File, zipFile: File)
           (implicit codec: Codec): HTRCVolume = {
    val metsXml = Helper.loadXml(metsFile)
    managed(new ZipFile(zipFile, codec.charSet)).acquireAndGet(
      HTRCVolume(pairtreeDoc, metsXml, _)
    )
  }

  /**
    * Loads a HTRC volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param metsXml     The METS XML document describing the volume
    * @param volZip      The associated volume ZIP file
    * @param codec       The codec to use for decoding the text (implicit)
    * @return The HTRCVolume
    */
  protected def apply(pairtreeDoc: PairtreeDocument, metsXml: Document, volZip: ZipFile)
                     (implicit codec: Codec): HTRCVolume = {
    // don't fail due to decoding errors
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    // extract the correct sequence of page filenames from the METS file and
    // attempt to map these page filenames to ZIP entries
    val pageZipEntries = getPageSeqMapping(metsXml).map {
      case (seq, f) => (seq, f, volZip.getEntry(s"${pairtreeDoc.getCleanIdWithoutLibId}/$f"))
    }

    // check for inconsistencies between the METS page sequence and ZIP contents
    // and throw exception if any are found
    val missingPages = pageZipEntries.withFilter(_._3 == null).map(_._2)
    if (missingPages.nonEmpty)
      throw MalformedVolumeException(s"[${pairtreeDoc.getUncleanId}] " +
        s"Missing page entries in volume ZIP file: " + missingPages.mkString(", "), null)

    val pages = pageZipEntries.map { case (seq, _, zipEntry) =>
      managed(volZip.getInputStream(zipEntry)).acquireAndGet(Page(_, seq))
    }.toIndexedSeq

    new HTRCVolume(pairtreeDoc, pages)
  }

  /**
    * Retrieve the correct page sequence from METS
    *
    * @param metsXml The METS XML
    * @return The sequence of page file names
    */
  protected def getPageSeqMapping(metsXml: Document): Seq[(String, String)] = {
    val xpath = XPathFactory.newInstance().newXPath()
    xpath.setNamespaceContext(new NamespaceContext {
      override def getPrefixes(namespaceURI: String) = null

      override def getPrefix(namespaceURI: String): String = null

      override def getNamespaceURI(prefix: String): String = prefix match {
        case "METS" => "http://www.loc.gov/METS/"
        case "xlink" => "http://www.w3.org/1999/xlink"
        case _ => null
      }
    })

    val metsOcrTxtFiles = xpath.evaluate(
      """//METS:fileGrp[@USE="ocr"]/METS:file[@MIMETYPE="text/plain"]/METS:FLocat""",
      metsXml, XPathConstants.NODESET
    ).asInstanceOf[NodeList]

    for {
      i <- 0 until metsOcrTxtFiles.getLength
      metsTxt = metsOcrTxtFiles.item(i).asInstanceOf[Element]
      pageFileName = metsTxt.getAttribute("xlink:href")
      pageSeq = metsTxt.getParentNode.asInstanceOf[Element].getAttribute("SEQ")
    } yield pageSeq -> pageFileName
  }

  private def joinLinesIntoPages(mutablePages: Seq[Seq[StringBuilder]],
                                 sep: String = "\n"): Seq[String] = {
    // for each page, join its non-empty lines using the given separator
    mutablePages.map(_.withFilter(_.nonEmpty).map(_.toString).mkString(sep))
  }

  private def extractParaLines(mutablePages: Seq[Seq[StringBuilder]],
                               sep: String = "\n"): Seq[String] = {
    // for each page, join non-empty lines into paragraphs,
    // where the end of the paragraph is decided when the last
    // character on a line is a period (or the following line is empty)
    mutablePages.map(_.withFilter(_.nonEmpty).map(_.toString())
      .groupWhen((l1, l2) => l1.nonEmpty && !EndParagraphPunct.contains(l1.last) && l2.nonEmpty)
      .map(_.mkString(" ")).mkString(sep)
    )
  }

  private def fixHyphenationInPlace(lines: Seq[StringBuilder]): Unit = {
    lines.map(sb => {
      val s = sb.toString()
      val left = HyphenLeftRegex.findFirstMatchIn(s)
      val right = HyphenRightRegex.findFirstMatchIn(s)
      (sb, left, right)
    })
      .sliding(2)
      .collect {
        case Seq((sb1, left1, right1), (sb2, left2, right2))
          if left1.nonEmpty && right2.nonEmpty =>
          (sb1, left1.get.group(1), sb2, right2.get.group(1))
      }
      .foreach {
        case (sb1, group1, sb2, group2) =>
          if (sb1.endsWith(group1 + "-") && sb2.startsWith(group2)) {
            val l1 = group1.length
            val l2 = group2.length
            sb1.delete(sb1.length - l1 - 1, sb1.length)
            sb1.append(group1).append(group2)
            sb2.delete(0, Math.min(l2 + 1, sb2.length))
          }
      }
  }

  private def trimInPlace(lines: Seq[StringBuilder]): Unit = {
    lines.foreach(sb => {
      val trimmed = sb.toString().trim
      sb.clear()
      sb.append(trimmed)
    })
  }
}

/**
  * An object representing an HTRC volume
  *
  * @param pairtreeDoc The pairtree reference object describing the document
  * @param pages       The sequence of pages comprising this document
  */
class HTRCVolume(val pairtreeDoc: PairtreeDocument,
                 val pages: Seq[Page]) {

  import HTRCVolume._

  /**
    * Lazily evaluated variable containing the pages of the volume
    * on which the header/footer detection algorithm has been ran.
    */
  lazy val structuredPages: Seq[Page with PageWithStructure] =
    PageStructureParser.parsePageStructure(pages)

  /**
    * Returns the cleaned volume ID of this volume.
    *
    * @return The cleaned volume ID of this volume
    */
  def cleanId: String = pairtreeDoc.getCleanId

  /**
    * Retrieves the page text content of a volume, each page separately.
    *
    * @param textOptions Optional text pre-processing actions to apply
    * @return The (processed) text for each page of the volume
    */
  def getTextPages(textOptions: TextOptions*): Seq[String] = {
    val mutablePages =
      if (textOptions contains BodyOnly)
        structuredPages.map(_.getBody.map(l => new StringBuilder(l.text)))
      else
        pages.map(_.lines.map(l => new StringBuilder(l.text)))

    var lines = mutablePages.flatten

    if (textOptions contains TrimLines)
      trimInPlace(lines)

    if (textOptions contains RemoveEmptyLines)
      lines = lines.filter(_.nonEmpty)

    if (textOptions contains FixHyphenation) {
      fixHyphenationInPlace(lines)
    }

    val textPages =
      if (textOptions contains ParaLines) {
        extractParaLines(mutablePages)
      } else
        joinLinesIntoPages(mutablePages)

    textPages
  }

  /**
    * Retrieves the textual content of a volume
    *
    * @param textOptions Optional text pre-processing actions to apply
    * @return The (processed) textual content of a volume
    */
  def getText(textOptions: TextOptions*): String = {
    var lines =
      if (textOptions contains BodyOnly)
        structuredPages.flatMap(_.getBody.map(_.text))
      else
        pages.flatMap(_.lines.map(_.text))

    if (textOptions contains TrimLines)
      lines = lines.map(_.trim)

    if (textOptions contains RemoveEmptyLines)
      lines = lines.filter(_.nonEmpty)

    if (textOptions contains FixHyphenation) {
      val text = HyphenWordRegex.replaceAllIn(lines.mkString("\n"), "$1$2\n")
      lines = text.split("\n")
    }

    val text =
      if (textOptions contains ParaLines)
        lines.groupWhen((l1, l2) => l1.nonEmpty && !EndParagraphPunct.contains(l1.last) && l2.nonEmpty)
          .map(_.mkString(" ")).mkString("\n")
      else
        lines.mkString("\n")

    text
  }

  /**
    * Returns the identified header for each page of the volume.
    *
    * @return The identified header for each page of the volume
    */
  def getHeaders: Seq[String] = structuredPages.map(_.getHeaderText())

  /**
    * Returns the identified footer for each page of the volume.
    *
    * @return The identified footer for each page of the volume
    */
  def getFooters: Seq[String] = structuredPages.map(_.getFooterText())

  /**
    * Returns the volume ID.
    *
    * @return The volume ID
    */
  def id: String = pairtreeDoc.getUncleanId

  override def toString: String = s"HTRCVolume($id)"
}

