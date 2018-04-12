package nematode

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.JavaConverters._

import scala.languageFeature.implicitConversions

object Labels {
  private def readLines(file: Path): List[String] = {
    Files.readAllLines(file).asScala.toList
  }

  private def getFiles(folder: String, endsWiths: String*): Map[String, Path] = {
    val files = scala.collection.mutable.Map[String, Path]()
    Files.walkFileTree(Paths.get(folder), new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val fname = file.getFileName.toString
        for (endsWith <- endsWiths) {
          if (fname.endsWith(endsWith)) {
            println(fname, fname.substring(0, fname.length - endsWith.length), endsWith)
            files.put(fname.substring(0, fname.length - endsWith.length), file)
          }
        }
        FileVisitResult.CONTINUE
      }
    })
    files.toMap
  }

  def loadLabels(folder: String): Labels = {
    Labels(getFiles(folder, ".txt").mapValues(p => readLines(p).drop(1)).mapValues(
      _.map(
        line => {
          val parts = line.split(" ").map(_.trim)
          val bbox = BoundingBox(parts(0).toFloat, parts(1).toFloat, parts(2).toFloat, parts(3).toFloat)
          val label = parts(4).toLowerCase == "interested"
          Label(boundingBox = bbox, label = label)
        }
      )
    ))
  }

  def loadInferences(folder: String): Inferences = {
    Inferences(getFiles(folder, ".bmp.txt", ".jpg.txt", ".png.txt", ".jpeg.txt").mapValues(p => readLines(p).drop(1)).mapValues(
      _.map(
        line => {
          val parts = line.split(",").map(_.trim)
          val bbox = BoundingBox(parts(1).toFloat, parts(2).toFloat, parts(3).toFloat, parts(4).toFloat)
          val raw_confidence = parts(5).toFloat
          val confidence = if (parts(0).toLowerCase == "interested") {
            raw_confidence
          } else {
            -raw_confidence
          }
          Inference(boundingBox = bbox, confidence = confidence)
        }
      )
    ))
  }

  def loadCorrectedCounts(filePath: String): Counts = {
    // TODO implement load counts
    val file = Paths.get(filePath).toFile
    if (!file.exists()) {
      return Counts(Map.empty)
    }
    val map = Files.readAllLines(file.toPath).asScala.drop(1).map(s => s.split(',')).map(a => (a(0), a(1).toInt)).toMap
    Counts(map)
  }

  private def getR2(values: Iterable[(Int, Int)]): Option[Double] = {
    if (values.isEmpty) {
      return None
    }
    val ss_res = values.map(v => math.pow(v._2 - v._1, 2)).sum
    val tv = values.map(_._2)
    val mean = tv.sum.toDouble / tv.size.toDouble
    val ss_tot = tv.map(v => v - mean).map(math.pow(_, 2)).sum
    Some(1 - ss_res / ss_tot)
  }

  def getR2(mergedCounts: Map[String, (Option[Int], Option[Int], Option[Int])]): (Option[Double], Option[Double]) = {
    val values1 = mergedCounts.filter(kv => kv._2._1.isDefined && kv._2._2.isDefined).mapValues(v => (v._1.get, v._2.get)).values
    val values2 = mergedCounts.filter(kv => kv._2._1.isDefined && kv._2._3.isDefined).mapValues(v => (v._3.get, v._2.get)).values
    (getR2(values1), getR2(values2))
  }
}

/**
  * @param counts A map of image ids to count of interested nematodes
  */
case class Counts(counts: Map[String, Int]) {
  /**
    * Merge the counts in this instance with the counts in the 'right' parameter. The resulting map will contain the
    * union of two merges. The values become tuples of Options. ._1 stores the count in the left (if defined) and
    * ._2 stores the count in the right (if defined).
    *
    * @param right1 the counts to be merged with
    * @param right2 the counts to merged with
    * @return a map of image ids to tuples of Options
    */
  def merge(right1: Counts, right2: Counts): Map[String, (Option[Int], Option[Int], Option[Int])] = {
    val mmap = scala.collection.mutable.Map[String, (Option[Int], Option[Int], Option[Int])]()
    counts.map(kv => mmap.put(kv._1, (Some(kv._2), None, None)))
    right1.counts.map(kv => {
      if (mmap.contains(kv._1)) {
        mmap.put(kv._1, (mmap(kv._1)._1, Some(kv._2), None))
      } else {
        mmap.put(kv._1, (None, Some(kv._2), None))
      }
    })
    right2.counts.map(kv => {
      if (mmap.contains(kv._1)) {
        mmap.put(kv._1, (mmap(kv._1)._1, mmap(kv._1)._2, Some(kv._2)))
      } else {
        mmap.put(kv._1, (None, None, Some(kv._2)))
      }
    })
    mmap.toMap
  }
}

/**
  * @param labels A map of image ids to lists of labels
  */
case class Labels(labels: Map[String, List[Label]]) {
  def toCounts: Counts = {
    Counts(labels.mapValues(l => l.count(_.label == true)))
  }
}

/**
  * @param inferences a map from image ids to lists of inferences
  */
case class Inferences(inferences: Map[String, List[Inference]]) {
  def toCounts(threshold: Float): Counts = {
    Counts(inferences.mapValues(l => l.count(_.confidence >= threshold)))
  }
}

/**
  * Represents a labeled bounding box
  *
  * @param boundingBox the bounding box
  * @param label       whether the bounding box is labeled as interested
  */
case class Label(boundingBox: BoundingBox, label: Boolean)

/**
  * Represents an inference result of a bounding box
  *
  * @param boundingBox the bounding box
  * @param confidence  level of confidence that the nematode inscribed by the bounding box is an interested nematode
  */
case class Inference(boundingBox: BoundingBox, confidence: Float)

/**
  * Represents a BoundingBox
  *
  * @param x1 x coordinate of top-left corner
  * @param y1 y coordinate of top-left corner
  * @param x2 x coordinate of bottom-right corner
  * @param y2 y coordinate of bottom-right corner
  */
case class BoundingBox(x1: Float, y1: Float, x2: Float, y2: Float)