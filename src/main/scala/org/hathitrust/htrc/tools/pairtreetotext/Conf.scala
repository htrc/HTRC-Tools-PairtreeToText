package org.hathitrust.htrc.tools.pairtreetotext

import org.rogach.scallop.{ScallopConf, ScallopOption, ScallopOptionGroup, ValueConverter, singleArgConverter}

import java.io.File
import scala.io.Codec

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
        vendor => s"$name $version\n$vendor"))).getOrElse(Main.appName))

  implicit private val codecConverter: ValueConverter[Codec] = singleArgConverter[Codec](Codec(_))

  val appOptions: ScallopOptionGroup = group("Main Options:")

  val sparkLog: ScallopOption[String] = opt[String]("spark-log",
    descr = "Where to write logging output from Spark to",
    argName = "FILE",
    noshort = true
  )

  val logLevel: ScallopOption[String] = opt[String]("log-level",
    descr = "The application log level; one of INFO, DEBUG, OFF",
    argName = "LEVEL",
    default = Some("INFO"),
    validate = level => Set("INFO", "DEBUG", "OFF").contains(level.toUpperCase)
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
    argName = "DIR",
    group = appOptions
  )

  val outputPath: ScallopOption[File] = opt[File]("output",
    descr = "The folder where the output will be written to",
    required = true,
    argName = "DIR",
    group = appOptions
  )

  val codec: ScallopOption[Codec] = opt[Codec]("codec",
    descr = "The codec to use for reading the volume",
    required = true,
    argName = "CODEC",
    default = Some(Codec.UTF8)
  )

  val bodyOnly: ScallopOption[Boolean] = opt[Boolean]("body-only",
    descr = "Remove running headers/footers from the pages before concatenation",
    default = Some(false),
    group = appOptions
  )

  val dehyphenateAtEol: ScallopOption[Boolean] = opt[Boolean]("dehyphenate-at-eol",
    short = 'h',
    descr = "Remove hyphenation for words occurring at the end of a line",
    default = Some(false),
    group = appOptions
  )

  val paraLines: ScallopOption[Boolean] = opt[Boolean]("para-lines",
    short = 'l',
    descr = "Join lines such that each paragraph is on a single line",
    default = Some(false),
    group = appOptions
  )

  val writePages: ScallopOption[Boolean] = opt[Boolean]("write-pages",
    descr = "Writes each page as a separate text file",
    default = Some(false)
  )

  val htids: ScallopOption[File] = trailArg[File]("htids",
    descr = "The file containing the list of HT IDs to process (if omitted, will read from stdin)",
    required = false
  )

  validateFileExists(pairtreeRootPath)
  validateFileExists(htids)
  verify()
}