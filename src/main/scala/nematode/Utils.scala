package nematode

import nematode.implicits._

object Utils {
  private val OS = System.getProperty("os.name").toLowerCase match {
    case u if u.contains("win") => Windows
    case u if u.contains("mac") => Mac
    case u if u.contains("nix") | u.contains("nux") => Ubuntu
  }

  /**
    * Execute a command
    * @param cmd command to be executed
    * @return a process instance in which the command is run
    */
  def exec(cmd: String): Process = {
    Runtime.getRuntime.exec(cmd)
  }

  /**
    * Show the folder using native file explorer
    * @param path path of the folder
    */
  def showFolder(path: String): Unit = {
    exec(OS.openFolderCmd.format(OS.resolveHome(path)))
  }

  /**
    * Convert ~/xxx paths to absolute paths
    * @param path path beginning with '~'
    * @return resolved path
    */
  def resolveHome(path: String): String = OS.resolveHome(path)
}

trait OS {
  val openFolderCmd: Pattern

  def resolveHome(path: String): String =
    if (path.startsWith("~"))
      System.getProperty("user.home") + path.substring(1)
    else path
}

object Windows extends OS {

  override val openFolderCmd: Pattern = "explorer {0}"
}

object Mac extends OS {
  override val openFolderCmd: Pattern = "open {0}"
}

object Ubuntu extends OS {
  override val openFolderCmd: Pattern = "nautilus {0}"
}