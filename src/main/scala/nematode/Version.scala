package nematode

import java.io.InputStream
import java.nio.file.{Files, Path, Paths}

import scala.util.Try

/** *
  * Functions for getting version strings of the components
  */
object Version {
  private def readFirstLine(path: Path): String = {
    Try(Files.readAllLines(path).get(0)).getOrElse("UNK")
  }

  import java.io.{BufferedReader, InputStreamReader}

  private def getStringFromInputStream(is: InputStream): String = {

    val sb = new StringBuilder
    var line: String = ""
    val br = new BufferedReader(new InputStreamReader(is))
    while (true) {
      line = br.readLine()
      if (line == null) {
        br.close()
        return sb.toString()
      }
      else {
        sb.append(line)
      }
    }
    null
  }

  val gui: String = getStringFromInputStream(getClass.getResourceAsStream("/VERSION"))

  def model: String = {
    readFirstLine(Paths.get(Config.modelFolder, "VERSION"))
  }

  def app: String = {
    readFirstLine(Paths.get(Config.appFolder, "VERSION"))
  }

  def versionString: String = s"app:$app model:$model gui:$gui"
}
