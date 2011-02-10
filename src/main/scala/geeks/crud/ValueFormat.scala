package geeks.crud

import java.util.Date
import java.text.{ParsePosition, Format, DateFormat, NumberFormat}

/**
 * A value format.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 */
trait ValueFormat[V] {
  /** May need to be overridden */
  def toString(value: V): String = value.toString

  def toValue(s: String): Option[V]
}

class BasicValueFormat[V](implicit m: Manifest[V]) extends ValueFormat[V] {
  /** May need to be overridden */
  def toValue(s: String): Option[V] = {
    val erasure = m.asInstanceOf[ClassManifest[V]].erasure
    try {
      erasure match {
        case x: Class[_] if (x == classOf[String]) => Some(s.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Int]) => Some(s.toInt.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Long]) => Some(s.toLong.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Short]) => Some(s.toShort.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Byte]) => Some(s.toByte.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Double]) => Some(s.toDouble.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Float]) => Some(s.toFloat.asInstanceOf[V])
        case x: Class[_] if (x == classOf[Boolean]) => Some(s.toBoolean.asInstanceOf[V])
        case _ => None
      }
    } catch {
      case e: IllegalArgumentException => None
    }
  }
}

class TextValueFormat[V](format: Format) extends ValueFormat[V] {
  def toValue(string: String) = {
    val position = new ParsePosition(0)
    val result = format.parseObject(string, position)
    if (position.getIndex == 0) None else Some(result.asInstanceOf[V])
  }
}