package nematode

import java.io.FileInputStream
import java.nio.file.Paths
import java.util.Properties

/**
  * Contains configs provided in ~/nematodes.cfg
  */
object Config {
  private val config: Properties = new Properties()
  config.load(new FileInputStream(Utils.resolveHome("~/nematodes.cfg")))

  val dataFolder: String = Utils.resolveHome(config.getProperty("data_folder"))

  val inputFolder: String = Paths.get(dataFolder, "input").toString

  val outputFolder: String = Paths.get(dataFolder, "output").toString

  val appFolder: String = Utils.resolveHome(config.getProperty("app_folder"))

  val modelFolder: String = Utils.resolveHome(config.getProperty("model_folder"))

  val guiFolder: String = Utils.resolveHome(config.getProperty("gui_folder"))

  val tempFolder: String = Utils.resolveHome(config.getProperty("tmp_folder"))

  val releaseRepo: String = config.getProperty("release_repo")
}
