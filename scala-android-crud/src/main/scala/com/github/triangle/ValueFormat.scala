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

  private val converter: String => Option[T] = {
    val classManifest = m.asInstanceOf[ClassManifest[T]]
    classManifest.erasure match {
      case x: Class[_] if (x == classOf[Int]) => convert(_.toInt)
      case x: Class[_] if (x == classOf[Long]) => convert(_.toLong)
      case x: Class[_] if (x == classOf[Short]) => convert(_.toShort)
      case x: Class[_] if (x == classOf[Byte]) => convert(_.toByte)
      case x: Class[_] if (x == classOf[Double]) => convert(_.toDouble)
      case x: Class[_] if (x == classOf[Float]) => convert(_.toFloat)
      case x: Class[_] if (x == classOf[Boolean]) => convert(_.toBoolean)
      case _ => throw new IllegalArgumentException("Unknown primitive type: " + classManifest.erasure)
    }
  }

  private def convert(f: String => AnyVal)(s: String): Option[T] = {
    try {
      Some(f(s).asInstanceOf[T])
    } catch {
      case e: IllegalArgumentException => None
    }
  }

  def toValue(s: String): Option[T] = converter(s)
}

class TextValueFormat[T](format: Format, obj2Value: (Object) => T = {(v: Object) => v.asInstanceOf[T]}) extends ValueFormat[T] {
  override def toString(value: T) = format.format(value)

  def toValue(string: String) = {
    val position = new ParsePosition(0)
    val result = format.parseObject(string, position)
    if (position.getIndex == 0) None else Some(obj2Value(result))
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
  private lazy val currencyFormat = NumberFormat.getCurrencyInstance
  private lazy val currencyEditFormat = {
    val editFormat = NumberFormat.getNumberInstance
    editFormat.setMinimumFractionDigits(currencyFormat.getMinimumFractionDigits)
    editFormat.setMaximumFractionDigits(currencyFormat.getMaximumFractionDigits)
    editFormat
  }
  lazy val amountFormats = List(currencyEditFormat, currencyFormat, NumberFormat.getNumberInstance).map(new TextValueFormat[Double](_, _.asInstanceOf[Number].doubleValue))

  lazy val dateFormats = List(new java.text.SimpleDateFormat("MM/dd/yyyy"), new java.text.SimpleDateFormat("dd MMM yyyy")).map(new TextValueFormat[Date](_))

  def toCalendarFormat(format: ValueFormat[Date]): ValueFormat[Calendar] = new ValueFormat[Calendar] {
    override def toString(value: Calendar) = format.toString(value.getTime)

    def toValue(s: String) = format.toValue(s).map { date =>
      val calendar = Calendar.getInstance
      calendar.setTime(date)
      calendar
    }
  }

  def enumFormat[T](enum: Enumeration): ValueFormat[T] = new ValueFormat[T] {
    override def toString(enumValue: T) = enumValue.toString

    def toValue(s: String) = enum.valueOf(s).map(_.asInstanceOf[T])
  }

  lazy val currencyValueFormat = new FlexibleValueFormat[Double](amountFormats)
  lazy val currencyDisplayValueFormat = new TextValueFormat[Double](currencyFormat, _.asInstanceOf[Number].doubleValue)
  lazy val dateValueFormat = new FlexibleValueFormat[Date](dateFormats)
  lazy val calendarValueFormat = toCalendarFormat(dateValueFormat)
}
