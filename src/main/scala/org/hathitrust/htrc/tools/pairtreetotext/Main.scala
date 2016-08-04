package org.hathitrust.htrc.tools.pairtreetotext

import java.io.{File, PrintWriter}

import org.hathitrust.htrc.tools.pairtreetotext.PairtreeToText.pairtreeToText
import org.rogach.scallop.ScallopConf
import resource._

import scala.io.{Codec, Source, StdIn}
import scala.util.{Failure, Success}

/**
  * PairtreeToText
  *
  * This application extracts full text from a HT volume stored in Pairtree by concatenating the pages in the correct
  * order, as specified in the associated METS XML metadata file. A number of helpful API methods are made available
  * so this app can also be used as a library (whose methods can be invoked from external code)
  *
  * @author Boris Capitanu
  */

object Main extends App {
  implicit val codec = Codec.UTF8

  // parse and extract command line arguments
  val conf = new Conf(args)
  val pairtreeRootPath = conf.pairtreeRootPath()
  val outputPath = conf.outputPath()
  val isCleanId = conf.isCleanId()
  val htids = conf.htids.toOption match {
    case Some(file) => Source.fromFile(file).getLines()
    case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null)
  }

  // ensure output path exists
  outputPath.mkdirs()

  // process all the supplied HT IDs in parallel (remove ".par" below if you want sequential processing)
  for (htid <- htids.toList.par) {
    pairtreeToText(htid, pairtreeRootPath, isCleanId) match {
      case Success((pairtreeDoc, volTxt)) =>
        val volTxtFile = new File(outputPath, s"${pairtreeDoc.getCleanId}.txt")
        for (writer <- managed(new PrintWriter(volTxtFile, codec.name)))
          writer.write(volTxt)

      case Failure(e) => Console.err.println(s"Error [$htid]: ${e.getMessage}")
    }
  }
}

/**
  * Command line argument configuration
  *
  * @param arguments The cmd line args
  */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val (appTitle, appVersion, appVendor) = {
    val p = getClass.getPackage
    val nameOpt = Option(p).flatMap(p => Option(p.getImplementationTitle))
    val versionOpt = Option(p).flatMap(p => Option(p.getImplementationVersion))
    val vendorOpt = Option(p).flatMap(p => Option(p.getImplementationVendor))
    (nameOpt, versionOpt, vendorOpt)
  }

  version(appTitle.flatMap(
    name => appVersion.flatMap(
      version => appVendor.map(
        vendor => s"$name $version\n$vendor"))).getOrElse("pairtree-to-text"))

  val pairtreeRootPath = opt[File]("pairtree",
    descr = "The path to the pairtree root hierarchy to process",
    required = true,
    argName = "DIR"
  )

  val outputPath = opt[File]("output",
    descr = "The output folder where the text files will be written to",
    required = true,
    argName = "DIR"
  )

  val isCleanId = opt[Boolean]("clean-ids",
    descr = "Specifies whether the IDs are 'clean' or not",
    default = Some(false)
  )

  val htids = trailArg[File]("htids",
    descr = "The file containing the list of HT IDs to process (if omitted, will read from stdin)",
    required = false
  )

  validateFileExists(pairtreeRootPath)
  validateFileExists(htids)
  verify()
}