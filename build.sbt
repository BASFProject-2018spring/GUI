name := "GUI"

version := "0.1"

scalaVersion := "2.12.5"

mainClass in (Compile, run) := Some("nematode.App")
mainClass in (Compile, packageBin) := Some("nematode.App")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.11.0.201803080745-r"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.16.1"
