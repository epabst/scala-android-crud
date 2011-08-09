package com.github.triangle

import java.text.{ParsePosition, Format, NumberFormat}
import java.util.{Calendar, Date}
import scala.Enumeration

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

  def toValue(s: String): Option[T] = formats.view.flatMap(_.toValue(s)).headOption
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

  def enumFormat[T <: Enumeration#Value](enum: Enumeration): ValueFormat[T] = new ValueFormat[T] {
    override def toString(enumValue: T) = enumValue.toString

    def toValue(s: String) = enum.values.find(_.toString == s).map(_.asInstanceOf[T])
  }

  def basicFormat[T <: AnyVal](implicit manifest: Manifest[T]): ValueFormat[T] = {
    val converter: String => T = {
      manifest.erasure match {
        case x: Class[_] if (x == classOf[Int]) => _.toInt.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Long]) => _.toLong.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Short]) => _.toShort.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Byte]) => _.toByte.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Double]) => _.toDouble.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Float]) => _.toFloat.asInstanceOf[T]
        case x: Class[_] if (x == classOf[Boolean]) => _.toBoolean.asInstanceOf[T]
        case _ => throw new IllegalArgumentException("Unknown primitive type: " + classManifest.erasure)
      }
    }
    format(converter, _.toString)
  }

  def format[T](convertFromString: String => T, convertToString: T => String): ValueFormat[T] =
    formatOption[T](s => {
      try { Some(convertFromString(s)) }
      catch { case e: IllegalArgumentException => None }
    }, convertToString)

  def formatOption[T](convertFromString: String => Option[T], convertToString: T => String): ValueFormat[T] =
    new ValueFormat[T] {
      def toValue(s: String) = convertFromString(s)

      override def toString(value: T) = convertToString(value)
    }

  lazy val currencyValueFormat = new FlexibleValueFormat[Double](amountFormats)
  lazy val currencyDisplayValueFormat = new TextValueFormat[Double](currencyFormat, _.asInstanceOf[Number].doubleValue)
  lazy val dateValueFormat = new FlexibleValueFormat[Date](dateFormats)
  lazy val calendarValueFormat = toCalendarFormat(dateValueFormat)
}
