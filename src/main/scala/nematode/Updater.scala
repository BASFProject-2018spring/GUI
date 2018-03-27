package nematode

import java.net.URL
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Updater {
  // the working directory of the updater instance, usually in a temporary folder
  val workDir: Path = Paths.get(Config.tempFolder, UUID.randomUUID().toString)
  Files.createDirectories(workDir)

  // unzip the file into a temporary folder and returns the path to that folder
  private def unzip(file: String): String = {
    val tmp_folder = Paths.get(workDir.toString, UUID.randomUUID().toString)

    import java.io.{File, FileInputStream}

    import org.apache.commons.compress.archivers.ArchiveStreamFactory

    val inputFile = new File(file)

    val inputStream = new FileInputStream(inputFile)
    val archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream("zip", inputStream)

    var entry: ZipArchiveEntry = null

    Files.createDirectories(tmp_folder)

    while ( {
      entry = archiveInputStream.getNextEntry.asInstanceOf[ZipArchiveEntry]
      entry != null
    }) {
      val outFile = Paths.get(tmp_folder.toString, entry.getName).toFile
      if (entry.isDirectory || entry.getName.endsWith("/")) {
        if (!outFile.exists()) {
          outFile.mkdirs()
        }
      } else if (outFile.exists()) {

      } else {
        import java.io.FileOutputStream
        val out = new FileOutputStream(outFile)
        val buffer = new Array[Byte](1024)
        var length = 0
        while ( {
          length = archiveInputStream.read(buffer)
          length > 0
        }) {
          out.write(buffer, 0, length)
          out.flush()
        }
      }
    }
    tmp_folder.toString
  }

  // clone the release repository into the workDir
  def cloneReleaseRepo(): Future[Git] = {
    Future {
      import org.eclipse.jgit.api.Git
      Git.cloneRepository.setURI(Config.releaseRepo).setDirectory(workDir.toFile).call
    }
  }

  /**
    * Get latest versions from the release repository
    * @return ((AppVersion, AppLatestZipUrl), (ModelVersion, ModelLatestZipUrl), (GUIVersion, GUILatestJarUrl))
    */
  def getVersions: Future[((String, String), (String, String), (String, String))] = {
    Future {
      val app = Files.readAllLines(Paths.get(workDir.toString, "app.txt"))
      val model = Files.readAllLines(Paths.get(workDir.toString, "model.txt"))
      val gui = Files.readAllLines(Paths.get(workDir.toString, "gui.txt"))
      ((app.get(0), app.get(1)), (model.get(0), model.get(1)), (gui.get(0), gui.get(1)))
    }
  }

  /**
    * Update the app component by downloading the zip archive at urlStr. The function will report its status by
    * calling reportTo callable. The function is async and a Future is returned.
    * @param urlStr url of the zip archive
    * @param reportTo callable to report status
    * @return Future
    */
  def updateApp(urlStr: String, reportTo: Option[(String) => Any]): Future[Any] = Future {
    val zipFile = Paths.get(workDir.toString, "app.zip")
    reportTo.map(_.apply("Downloading new app"))
    FileUtils.copyURLToFile(new URL(urlStr), zipFile.toFile)
    reportTo.map(_.apply("New app downloaded"))

    reportTo.map(_.apply("Unzipping"))
    val tempFolder = unzip(zipFile.toString)
    reportTo.map(_.apply("Unzipped"))

    updateDirectory(tempFolder, Config.appFolder)
  }

  /**
    * Update the gui component by downloading the jar at urlStr. The function will report its status by
    * calling reportTo callable. The function is async and a Future is returned.
    * @param urlStr url of the gui jar
    * @param reportTo callable to report status
    * @return Future
    */
  def updateGUI(urlStr: String, reportTo: Option[(String) => Any]): Future[Any] = Future {
    reportTo.map(_.apply("Downloading new gui"))
    FileUtils.copyURLToFile(new URL(urlStr), Paths.get(Config.guiFolder, "app_new.jar").toFile)
    reportTo.map(_.apply("New gui downloaded"))
  }

  /**
    * Update the model component by downloading the zip archive at urlStr. The function will report its status by
    * calling reportTo callable. The function is async and a Future is returned.
    * @param urlStr url of the zip archive
    * @param reportTo callable to report status
    * @return Future
    */
  def updateModel(urlStr: String, reportTo: Option[(String) => Any]): Future[Any] =
    Future {
      val zipFile = Paths.get(workDir.toString, "model.zip")
      reportTo.map(_.apply("Downloading new model"))
      FileUtils.copyURLToFile(new URL(urlStr), zipFile.toFile)
      reportTo.map(_.apply("New model downloaded"))

      reportTo.map(_.apply("Unzipping"))
      val tempFolder = unzip(zipFile.toString)
      reportTo.map(_.apply("Unzipped"))

      updateDirectory(tempFolder, Config.modelFolder)
    }

  private def updateDirectory(sourceDir: String, targetDir: String): Unit = {
    val bakFolder = Paths.get(targetDir + ".bak").toFile
    val targetFolder = Paths.get(targetDir).toFile
    if (bakFolder.exists()) {
      FileUtils.deleteQuietly(bakFolder)
    }
    if (targetFolder.exists()) {
      targetFolder.renameTo(bakFolder)
    }
    FileUtils.copyDirectory(Paths.get(sourceDir).toFile, targetFolder)
  }
}
