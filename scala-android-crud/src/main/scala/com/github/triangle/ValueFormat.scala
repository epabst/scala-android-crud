package com.github.triangle

import java.text.{ParsePosition, Format, NumberFormat}
import java.util.{Calendar, Date}

/**
 * A value format.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 */
trait ValueFormat[T] {
  /** May need to be overridden */
  def toString(value: T): String = value.toString

  def toValue(s: String): Option[T]
}

class BasicValueFormat[T <: AnyVal]()(implicit m: Manifest[T]) extends ValueFormat[T] {
  /** May need to be overridden */
  def toValue(s: String): Option[T] = {
    val erasure = m.asInstanceOf[ClassManifest[T]].erasure
    try {
      erasure match {
        case x: Class[_] if (x == classOf[String]) => Some(s.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Int]) => Some(s.toInt.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Long]) => Some(s.toLong.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Short]) => Some(s.toShort.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Byte]) => Some(s.toByte.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Double]) => Some(s.toDouble.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Float]) => Some(s.toFloat.asInstanceOf[T])
        case x: Class[_] if (x == classOf[Boolean]) => Some(s.toBoolean.asInstanceOf[T])
        case _ => None
      }
    } catch {
      case e: IllegalArgumentException => None
    }
  }
}

class TextValueFormat[T](format: Format) extends ValueFormat[T] {
  override def toString(value: T) = format.format(value)

  def toValue(string: String) = {
    val position = new ParsePosition(0)
    val result = format.parseObject(string, position)
    if (position.getIndex == 0) None else Some(result.asInstanceOf[T])
  }
}

class FlexibleValueFormat[T](formats: List[ValueFormat[T]]) extends ValueFormat[T] {
  override def toString(value: T) = formats.headOption.map(_.toString(value)).getOrElse(super.toString(value))

  def toValue(s: String): Option[T] = {
    for (format <- formats) {
      val value = format.toValue(s)
      if (value.isDefined) return value
    }
    None
  }
}

object ValueFormat {
  lazy val currencyFormat = NumberFormat.getCurrencyInstance
  lazy val currencyEditFormat = {
    val editFormat = NumberFormat.getNumberInstance
    editFormat.setMinimumFractionDigits(currencyFormat.getMinimumFractionDigits)
    editFormat.setMaximumFractionDigits(currencyFormat.getMaximumFractionDigits)
    editFormat
  }
  lazy val amountFormats = List(currencyEditFormat, currencyFormat, NumberFormat.getNumberInstance).map(new TextValueFormat[Number](_))

  lazy val dateFormats = List(new java.text.SimpleDateFormat("MM/dd/yyyy")).map(new TextValueFormat[Date](_))

  def toCalendarFormat(format: ValueFormat[Date]): ValueFormat[Calendar] = new ValueFormat[Calendar] {
    override def toString(value: Calendar) = format.toString(value.getTime)

    def toValue(s: String) = format.toValue(s).map { date =>
      val calendar = Calendar.getInstance
      calendar.setTime(date)
      calendar
    }
  }

  lazy val currencyValueFormat = new FlexibleValueFormat[Number](amountFormats)
  lazy val dateValueFormat = new FlexibleValueFormat[Date](dateFormats)
  lazy val calendarValueFormat = toCalendarFormat(dateValueFormat)
}
