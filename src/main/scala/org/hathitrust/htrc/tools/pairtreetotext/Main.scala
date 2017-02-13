package org.hathitrust.htrc.tools.pairtreetotext

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.hadoop.fs.Path
import org.apache.spark.{SparkConf, SparkContext}
import org.hathitrust.htrc.tools.pairtreetotext.Helper.logger
import org.hathitrust.htrc.tools.pairtreetotext.TextOptions._
import org.hathitrust.htrc.tools.spark.errorhandling.ErrorAccumulator
import org.hathitrust.htrc.tools.spark.errorhandling.RddExtensions._
import org.rogach.scallop.{ScallopConf, ScallopOption, singleArgConverter}

import scala.collection.mutable
import scala.io.{Codec, Source, StdIn}

/**
  * PairtreeToText
  *
  * This application extracts full text from a HT volume stored in Pairtree by concatenating the
  * pages in the correct order, as specified in the associated METS XML metadata file.
  * It optionally performs additional post-processing to identify running headers,
  * fix end-of-line hyphenation, and reformat the text.
  *
  * @author Boris Capitanu
  */

object Main {
  val appName = "pairtree-to-text"

  def main(args: Array[String]): Unit = {
    // parse and extract command line arguments
    val conf = new Conf(args)
    val numPartitions = conf.numPartitions.toOption
    val pairtreeRootPath = conf.pairtreeRootPath().toString
    val outputPath = conf.outputPath().toString
    val codec = conf.codec().name
    val bodyOnly = conf.bodyOnly()
    val fixHyphenation = conf.fixHyphenation()
    val paraLines = conf.paraLines()
    val isCleanId = conf.isCleanId()
    val writePages = conf.writePages()
    val htids = conf.htids.toOption match {
      case Some(file) => Source.fromFile(file).getLines().toSeq
      case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toSeq
    }

    conf.sparkLog.toOption match {
      case Some(logFile) => System.setProperty("spark.logFile", logFile)
      case None =>
    }

    val textOptions = mutable.HashSet.empty[TextOptions]
    if (bodyOnly) {
      textOptions += BodyOnly
    }
    if (fixHyphenation) {
      textOptions ++= Seq(TrimLines, RemoveEmptyLines, FixHyphenation)
    }
    if (paraLines) {
      textOptions ++= Seq(TrimLines, RemoveEmptyLines, ParaLines)
    }

    val sparkConf = new SparkConf()
    sparkConf.setIfMissing("spark.master", "local[*]")
    sparkConf.setIfMissing("spark.app.name", appName)

    val sc = new SparkContext(sparkConf)

    logger.info("Running...")

    // ensure output path exists
    conf.outputPath().mkdirs()

    val volErrAcc = new ErrorAccumulator[String, String](identity)(sc)

    val ids = numPartitions match {
      case Some(n) => sc.parallelize(htids, n) // split input into n partitions
      case None => sc.parallelize(htids) // use default number of partitions
    }

    val vols = ids.tryMap(HTRCVolume(_, pairtreeRootPath, isCleanId)(Codec(codec)))(volErrAcc)

    if (writePages) {
      val volTexts = vols.map(v => v.pairtreeDoc -> v.getTextPages(textOptions.toSeq: _*))

      volTexts.foreach {
        case (doc, pages) =>
          val outPath = Paths.get(outputPath, doc.getCleanId)
          Files.createDirectories(outPath)
          pages.zipWithIndex.foreach {
            case ((text, i)) =>
              val pagePath = Paths.get(outPath.toString, f"${i + 1}%08d.txt")
              Helper.writeTextFile(text, pagePath)(Codec(codec))
          }

      }
    } else {
      val volTexts = vols.map(v => v.pairtreeDoc -> v.getText(textOptions.toSeq: _*))

      volTexts.foreach {
        case (doc, text) =>
          val outPath = Paths.get(outputPath, doc.getCleanId + ".txt")
          Helper.writeTextFile(text, outPath)(Codec(codec))
      }
    }

    if (volErrAcc.nonEmpty)
      volErrAcc.saveErrors(new Path(outputPath, "errors.txt"), _.toString)

    logger.info("All done.")
  }

  /**
    * Command line argument configuration
    *
    * @param arguments The cmd line args
    */
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    private val (appTitle, appVersion, appVendor) = {
      val p = getClass.getPackage
      val nameOpt = Option(p).flatMap(p => Option(p.getImplementationTitle))
      val versionOpt = Option(p).flatMap(p => Option(p.getImplementationVersion))
      val vendorOpt = Option(p).flatMap(p => Option(p.getImplementationVendor))
      (nameOpt, versionOpt, vendorOpt)
    }

    version(appTitle.flatMap(
      name => appVersion.flatMap(
        version => appVendor.map(
          vendor => s"$name $version\n$vendor"))).getOrElse(appName))

    implicit private val codecConverter = singleArgConverter[Codec](Codec(_))

    val sparkLog: ScallopOption[String] = opt[String]("spark-log",
      descr = "Where to write logging output from Spark to",
      argName = "FILE",
      noshort = true
    )

    val numPartitions: ScallopOption[Int] = opt[Int]("num-partitions",
      descr = "The number of partitions to split the input set of HT IDs into, " +
        "for increased parallelism",
      required = false,
      argName = "N",
      validate = 0 <
    )

    val pairtreeRootPath: ScallopOption[File] = opt[File]("pairtree",
      descr = "The path to the pairtree root hierarchy to process",
      required = true,
      argName = "DIR"
    )

    val outputPath: ScallopOption[File] = opt[File]("output",
      descr = "The folder where the output will be written to",
      required = true,
      argName = "DIR"
    )

    val codec: ScallopOption[Codec] = opt[Codec]("codec",
      descr = "The codec to use for reading the volume",
      required = true,
      argName = "CODEC",
      default = Some(Codec.UTF8)
    )

    val bodyOnly: ScallopOption[Boolean] = opt[Boolean]("body-only",
      descr = "Remove running headers/footers from the pages before concatenation",
      default = Some(false)
    )

    val fixHyphenation: ScallopOption[Boolean] = opt[Boolean]("fix-hyphenation",
      short = 'h',
      descr = "Remove hyphenation for words occurring at the end of a line",
      default = Some(false)
    )

    val paraLines: ScallopOption[Boolean] = opt[Boolean]("para-lines",
      short = 'l',
      descr = "Join lines such that each paragraph is on a single line",
      default = Some(false)
    )

    val isCleanId: ScallopOption[Boolean] = opt[Boolean]("clean-ids",
      noshort = true,
      descr = "Specifies whether the IDs are 'clean' or not",
      default = Some(false)
    )

    val writePages: ScallopOption[Boolean] = opt[Boolean]("write-pages",
      descr = "Writes each page as a separate text file",
      default = Some(false)
    )

    val htids: ScallopOption[File] = trailArg[File]("htids",
      descr = "The file containing the list of HT IDs to process (if omitted, will read from stdin)",
      required = false
    )

    mainOptions = Seq(pairtreeRootPath, outputPath, bodyOnly, fixHyphenation, paraLines)
    validateFileExists(pairtreeRootPath)
    validateFileExists(htids)
    verify()
  }

}