package org.hathitrust.htrc.tools.pairtreetotext

import com.gilt.gfc.time.Timer
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.hathitrust.htrc.data.id2Volume
import org.hathitrust.htrc.data.ops.TextOptions.{TextOptions, _}
import org.hathitrust.htrc.tools.pairtreetotext.Helper._
import org.hathitrust.htrc.tools.spark.errorhandling.ErrorAccumulator
import org.hathitrust.htrc.tools.spark.errorhandling.RddExtensions._

import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.io.{Codec, Source, StdIn}
import scala.util.Using

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
  val appName: String = "pairtree-to-text"

  def main(args: Array[String]): Unit = {
    // parse and extract command line arguments
    val conf = new Conf(args.toIndexedSeq)
    val numPartitions = conf.numPartitions.toOption
    val pairtreeRootPath = conf.pairtreeRootPath().toString
    val outputPath = conf.outputPath().toString
    val codec = conf.codec().name
    val bodyOnly = conf.bodyOnly()
    val dehyphenateAtEol = conf.dehyphenateAtEol()
    val paraLines = conf.paraLines()
    val writePages = conf.writePages()
    val htids = conf.htids.toOption match {
      case Some(file) => Using.resource(Source.fromFile(file))(_.getLines().toSeq)
      case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toSeq
    }

    conf.outputPath().mkdirs()

    // set up logging destination
    conf.sparkLog.toOption match {
      case Some(logFile) => System.setProperty("spark.logFile", logFile)
      case None =>
    }
    System.setProperty("logLevel", conf.logLevel().toUpperCase)

    // set up Spark context
    val sparkConf = new SparkConf()
    sparkConf.setAppName(appName)
    sparkConf.setIfMissing("spark.master", "local[*]")

    val spark = SparkSession.builder()
      .config(sparkConf)
      .getOrCreate()

    val sc = spark.sparkContext

    val textOptions = mutable.HashSet.empty[TextOptions]
    if (dehyphenateAtEol) {
      textOptions ++= Seq(TrimLines, RemoveEmptyLines, DehyphenateAtEol)
    }
    if (paraLines) {
      textOptions ++= Seq(TrimLines, RemoveEmptyLines, ParaLines)
    }

    logger.info("Running...")
    val t0 = Timer.nanoClock()

    // ensure output path exists
    conf.outputPath().mkdirs()

    val volErrAcc = new ErrorAccumulator[String, String](identity)(sc)

    val ids = numPartitions match {
      case Some(n) => sc.parallelize(htids, n) // split input into n partitions
      case None => sc.parallelize(htids) // use default number of partitions
    }

    val vols = ids.tryMap(id2Volume(_, pairtreeRootPath)(Codec(codec)).get)(volErrAcc)

    val volTexts = vols.map(v => v.volumeId -> {
      if (bodyOnly)
        v.structuredPages.map(_.body(textOptions.toSeq: _*))
      else v.pages.map(_.text(textOptions.toSeq: _*))
    })

    if (writePages) {
      volTexts.foreach {
        case (doc, pages) =>
          val outPath = Paths.get(outputPath, doc.cleanId)
          Files.createDirectories(outPath)
          pages.zipWithIndex.foreach {
            case (text, i) =>
              val pagePath = Paths.get(outPath.toString, f"${i + 1}%08d.txt")
              Helper.writeTextFile(text, pagePath)(Codec(codec))
          }

      }
    } else {
      volTexts.foreach {
        case (doc, pages) =>
          val text = pages.mkString(System.lineSeparator())
          val outPath = Paths.get(outputPath, doc.cleanId + ".txt")
          Helper.writeTextFile(text, outPath)(Codec(codec))
      }
    }

    if (volErrAcc.nonEmpty)
      volErrAcc.saveErrors(new Path(outputPath, "errors.txt"), _.toString)

    val t1 = Timer.nanoClock()
    val elapsed = t1 - t0

    logger.info(f"All done in ${Timer.pretty(elapsed)}")
  }
}
