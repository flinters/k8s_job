package jp.co.septeni_original.k8sop.util

import java.io.File

import com.nimbusds.jose.util.StandardCharset
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils

object FileReader extends LazyLogging {

  /**
    * key: file name
    * value: file body
    */
  def directoryToMap(cmdir: File): Map[String, String] = {
    logger.info(s"loading cmdir : ${cmdir.getAbsolutePath}")
    require(cmdir.isDirectory, s"$cmdir isn't Directory.")

    cmdir
      .listFiles()
      .toList
      .filter(_.isFile)
      .map { f =>
        (f.getName, FileUtils.readFileToString(f, StandardCharset.UTF_8))
      }
      .toMap
  }

}
