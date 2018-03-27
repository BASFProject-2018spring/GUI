package nematode.controllers

import java.nio.file.{Files, Paths}

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control
import javafx.scene.control.{cell, _}
import javafx.stage.{DirectoryChooser, FileChooser, Stage}
import nematode._
import org.apache.commons.io.FileUtils

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.sys.process.ProcessLogger
import scala.util.{Failure, Success, Try}

case object RunFX {
  def apply(body: => Unit): Future[Unit] = {
    Future.successful(Platform.runLater(() => {
      body
    }))
  }
}

class TableItem(@BeanProperty var image: String, @BeanProperty var inferenceCount: String, @BeanProperty var labelCount: String) {

}

class MainController {
  @FXML
  protected var openInputFolder: Button = _
  @FXML
  protected var openOutputFolder: Button = _
  @FXML
  protected var runInference: Button = _
  @FXML
  protected var browseInferenceFolder: Button = _
  @FXML
  protected var browseLabelFolder: Button = _
  @FXML
  protected var inferenceFolder: TextField = _
  @FXML
  protected var labelFolder: TextField = _
  @FXML
  protected var loadInferenceFolder: Button = _
  @FXML
  protected var loadLabelFolder: Button = _
  @FXML
  protected var updateBtn: Button = _
  @FXML
  protected var versionText: javafx.scene.control.Label = _
  @FXML
  protected var updateStatus: ListView[String] = _
  @FXML
  protected var threshold: TextField = _
  @FXML
  protected var countTable: TableView[TableItem] = _
  @FXML
  protected var calculate: Button = _
  @FXML
  protected var exportInferredCounts: Button = _
  @FXML
  protected var r2Val: javafx.scene.control.Label = _
  @FXML
  protected var inferenceLog: ListView[String] = _

  private var updating: Boolean = false
  private var inferring: Boolean = false

  private var stage: Stage = _

  private var labels: Labels = Labels(Map.empty)
  private var inferences: Inferences = Inferences(Map.empty)

  def setStage(stage: Stage): Unit = {
    this.stage = stage
  }

  def updateVersionString(): Unit = {
    versionText.setText(Version.versionString)
  }

  def addUpdateStatus(msg: String): Future[Any] = RunFX {
    updateStatus.getItems.add(0, msg)
  }

  protected val labelFolderChooser = new DirectoryChooser()
  protected val inferenceFolderChooser = new DirectoryChooser()
  protected val exportFileChooser = new FileChooser()

  protected def export(): Unit = {
    val file = exportFileChooser.showSaveDialog(stage)
    val buffer = collection.mutable.Buffer[String]()
    buffer.append("image,count")

    val thres = Try(threshold.getText.toFloat).getOrElse(0.7f)

    threshold.setText(thres.toString)

    inferences.toCounts(thres).counts.map(kv => s"${kv._1}, ${kv._2}").foreach(s => buffer.append(s))
    Files.write(file.toPath, buffer.asJava)
  }

  protected def inference(): Unit = {
    // this check is neither necessary nor correct,
    if (inferring) {
      RunFX {
        inferenceLog.getItems.add(0, "Already running")
      }
      return
    }

    inferring = true

    val bakFolder = Paths.get(Config.outputFolder + ".bak").toFile
    val targetFolder = Paths.get(Config.outputFolder).toFile
    if (bakFolder.exists()) {
      FileUtils.deleteQuietly(bakFolder)
    }
    if (targetFolder.exists()) {
      targetFolder.renameTo(bakFolder)
    }

    RunFX {
      inferenceLog.getItems.add(0, "Running")
    }

    runInference.setDisable(true)
    RunFX {
      runInference.setDisable(true)
    }

    val sh = Paths.get(Config.appFolder, "run.sh")
    import java.nio.file.attribute.PosixFilePermission
    val perms = Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
      PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
      PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE)
    Files.setPosixFilePermissions(sh, perms.asJava)
    val p = sys.process.Process(sh.toString)

    val logger = ProcessLogger(
      out => RunFX {
        inferenceLog.getItems.add(0, out)
      },
      err => RunFX {
        inferenceLog.getItems.add(0, s"Err: $err")
      }
    )

    val prun = p.run(logger)

    Future {
      scala.concurrent.blocking(println(prun.exitValue()))
    }
      .onComplete(_ =>
        RunFX {
          println(prun.isAlive())
          println("Completed")
          inferenceLog.getItems.add(0, "Completed")
          runInference.setDisable(false)
          inferring = false
        }
      )
  }


  protected def update(): Unit = {
    if (updating) {
      updateStatus.getItems.add(0, "Already running")
      return
    }

    updating = true

    RunFX {
      updateBtn.setDisable(true)
    }
    updateBtn.setDisable(true)

    updateStatus.getItems.add(0, "Initializing")
    val updater = new Updater()
    updateStatus.getItems.add(0, s"Working dir ${updater.workDir.toString}")

    updateStatus.getItems.add(0, "Cloning release repository")

    val a = for {
      _ <- updater.cloneReleaseRepo()
      _ <- addUpdateStatus("Release repository cloned")
      versions <- updater.getVersions
      _ <- addUpdateStatus(s"app:${versions._1._1} model:${versions._2._1} gui:${versions._3._1}")
      _ <- versions._1._1 match {
        case v if v == Version.app => addUpdateStatus("app already up-to-date.")
        case _ => updater.updateApp(versions._1._2, Some(addUpdateStatus)).map(_ => addUpdateStatus("app updated."))
      }
      _ <- versions._2._1 match {
        case v if v == Version.model => addUpdateStatus("model already up-to-date.")
        case _ => updater.updateModel(versions._2._2, Some(addUpdateStatus)).map(_ => addUpdateStatus("model updated."))
      }
      _ <- versions._3._1 match {
        case Version.gui => addUpdateStatus("gui already up-to-date.")
        case _ => updater.updateGUI(versions._3._2, Some(addUpdateStatus)).map(_ => addUpdateStatus("gui updated, please restart."))
      }
    } yield {
    }

    a.onComplete({
      case Success(_) => RunFX {
        updateStatus.getItems.add(0, "Update successful.")

        updating = false
        RunFX {
          updateBtn.setDisable(false)
          updateVersionString()
        }
      }
      case Failure(f) => RunFX {
        updateStatus.getItems.add(0, s"Failure: ${f.toString}")

        updating = false
        RunFX {
          updateBtn.setDisable(false)
          updateVersionString()
        }
      }
    })

  }

  protected def updateCounts(): Unit = {
    RunFX {
      val thres = Try(threshold.getText.toFloat).getOrElse(0.7f)

      threshold.setText(thres.toString)

      val counts = inferences.toCounts(thres).merge(labels.toCounts)

      val tableItems = counts.keys.toList.sorted.map(
        k => new TableItem(k, counts(k)._1.map(_.toString).getOrElse("N/A"), counts(k)._2.map(_.toString).getOrElse("N/A"))
      )
      val ol = FXCollections.observableArrayList(tableItems.asJava)

      countTable.setItems(ol)

      val r2 = Labels.getR2(counts)
      val r2Str = r2.map(_.toString).getOrElse("N/A")
      r2Val.setText(r2Str)
    }
  }

  def initialize(): Unit = {
    openInputFolder.setOnAction(_ => Utils.showFolder(Config.inputFolder))
    openOutputFolder.setOnAction(_ => Utils.showFolder(Config.outputFolder))
    runInference.setOnAction(_ => inference())

    inferenceFolder.setText(Paths.get(Config.outputFolder, "boxes").toString)
    inferenceFolderChooser.setInitialDirectory(Paths.get(Config.outputFolder, "boxes").toFile)

    browseLabelFolder.setOnAction((_) => labelFolder.setText(Option(labelFolderChooser.showDialog(stage)).map(_.toString).getOrElse(labelFolder.getText)))
    browseInferenceFolder.setOnAction((_) => inferenceFolder.setText(Option(inferenceFolderChooser.showDialog(stage)).map(_.toString).getOrElse(inferenceFolder.getText)))

    updateBtn.setOnAction(_ => update())

    versionText.setAlignment(Pos.CENTER_RIGHT)

    loadLabelFolder.setOnAction(_ => {
      labels = Labels.loadLabels(labelFolder.getText)
      updateCounts()
    })

    loadInferenceFolder.setOnAction(_ => {
      inferences = Labels.loadInferences(inferenceFolder.getText)
      updateCounts()
    })

    calculate.setOnAction(_ => updateCounts())

    val imageColumn = new control.TableColumn[TableItem, String]("Image ID")
    val inferenceCountColumn = new control.TableColumn[TableItem, String]("Inferred Count")
    val labelCountColumn = new control.TableColumn[TableItem, String]("Labeled Count")
    imageColumn.setCellValueFactory(new cell.PropertyValueFactory[TableItem, String]("image"))
    inferenceCountColumn.setCellValueFactory(new cell.PropertyValueFactory[TableItem, String]("inferenceCount"))
    labelCountColumn.setCellValueFactory(new cell.PropertyValueFactory[TableItem, String]("labelCount"))
    countTable.getColumns.add(imageColumn)
    countTable.getColumns.add(inferenceCountColumn)
    countTable.getColumns.add(labelCountColumn)

    updateVersionString()

    exportInferredCounts.setOnAction(_ => export())
  }
}
