package jp.co.septeni_original.k8sop.util

import java.io.File
import java.nio.file.Paths

import com.nimbusds.jose.util.StandardCharset
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils

object FileReader extends LazyLogging {

  /**
    * key: file name
    * value: file body
    */
  def directoryToMap(dirName: String): Map[String, String] = {
    val directory = Paths.get(dirName).toFile
    require(directory.isDirectory, s"$directory isn't Directory.")
    logger.debug(s"directory found. ${directory.getAbsolutePath}")

    val files: List[File] = directory.listFiles().toList

    files.filter(_.isFile).map { f =>
      (f.getName, FileUtils.readFileToString(f, StandardCharset.UTF_8))
    }.toMap
  }

}
