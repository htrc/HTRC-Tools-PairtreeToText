package org.hathitrust.htrc.tools.pairtreetotext

case class MalformedVolumeException(msg: String, cause: Throwable) extends Exception(msg, cause)