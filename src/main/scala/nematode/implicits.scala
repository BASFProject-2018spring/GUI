package nematode

import java.text.MessageFormat

import scala.language.implicitConversions

/**
  * Created by Hongxiang Qiu on 3/25/18.
  */
object implicits {

  implicit def toPattern(str: String): Pattern = Pattern(str)

}

case class Pattern(value: String) extends AnyVal {
  def format(args: AnyRef*): String = {
    MessageFormat.format(value, args: _*)
  }
}
