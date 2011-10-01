package com.github.scala.android.crud.persistence

import android.database.Cursor
import android.content.ContentValues
import java.util.{GregorianCalendar, Calendar, Date}
import scala.Enumeration
import android.os.Bundle

trait PersistedType[T] {
  def valueManifest: Manifest[T]

  def sqliteType: String

  def getValue(cursor: Cursor, cursorIndex: Int): Option[T]

  def putValue(contentValues: ContentValues, name: String, value: T)

  def getValue(bundle: Bundle, name: String): Option[T]

  def putValue(bundle: Bundle, name: String, value: T)
}

/**
 * An android PersistedType based on the {@link Cursor} and {@link ContentValues} api's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:30 AM
 */

private class ConvertedPersistedType[T,P](toValue: P => Option[T], toPersisted: T => P)
                                          (implicit persistedType: PersistedType[P], implicit val valueManifest: Manifest[T])
        extends PersistedType[T] {
  def sqliteType = persistedType.sqliteType

  def putValue(contentValues: ContentValues, name: String, value: T) {
    persistedType.putValue(contentValues, name, toPersisted(value))
  }

  def getValue(cursor: Cursor, cursorIndex: Int): Option[T] = persistedType.getValue(cursor, cursorIndex).flatMap(toValue)

  def putValue(bundle: Bundle, name: String, value: T) {
    persistedType.putValue(bundle, name, toPersisted(value))
  }

  def getValue(bundle: Bundle, name: String): Option[T] = persistedType.getValue(bundle, name).flatMap(toValue)
}

private class DirectPersistedType[T <: AnyRef](val sqliteType: String,
                                               cursorGetter: Cursor => Int => Option[T], contentValuesPutter: ContentValues => (String, T) => Unit,
                                               bundleGetter: Bundle => String => Option[T], bundlePutter: Bundle => (String, T) => Unit)
                                      (implicit val valueManifest: Manifest[T]) extends PersistedType[T] {
  def putValue(contentValues: ContentValues, name: String, value: T) {
    contentValuesPutter(contentValues)(name, value)
  }

  def getValue(cursor: Cursor, cursorIndex: Int): Option[T] = if (cursor.isNull(cursorIndex)) None else cursorGetter(cursor)(cursorIndex)

  def putValue(bundle: Bundle, name: String, value: T) {
    bundlePutter(bundle)(name, value)
  }

  def getValue(bundle: Bundle, name: String): Option[T] = if (bundle.containsKey(name)) bundleGetter(bundle)(name) else None
}

private class CastedPersistedType[T <: AnyVal, R <: AnyRef](implicit refType: PersistedType[R],
                                                            implicit val valueManifest: Manifest[T])
        extends PersistedType[T] {
  def sqliteType = refType.sqliteType

  def putValue(contentValues: ContentValues, name: String, value: T) {
    refType.putValue(contentValues, name, value.asInstanceOf[R])
  }

  def getValue(cursor: Cursor, cursorIndex: Int) = refType.getValue(cursor, cursorIndex).asInstanceOf[Option[T]]

  def putValue(bundle: Bundle, name: String, value: T) {
    refType.putValue(bundle, name, value.asInstanceOf[R])
  }

  def getValue(bundle: Bundle, name: String) = refType.getValue(bundle, name).asInstanceOf[Option[T]]
}

object PersistedType {
  private class RichBundle(bundle: Bundle) {
    implicit def getJLong(key: String): java.lang.Long = bundle.getLong(key)
    implicit def putJLong(key: String, value: java.lang.Long) { bundle.putLong(key, value.longValue) }
    implicit def getJInt(key: String): java.lang.Integer = bundle.getInt(key)
    implicit def putJInt(key: String, value: java.lang.Integer) { bundle.putInt(key, value.intValue) }
    implicit def getJShort(key: String): java.lang.Short = bundle.getShort(key)
    implicit def putJShort(key: String, value: java.lang.Short) { bundle.putShort(key, value.shortValue) }
    implicit def getJByte(key: String): java.lang.Byte = bundle.getByte(key)
    implicit def putJByte(key: String, value: java.lang.Byte) { bundle.putByte(key, value.byteValue) }
    implicit def getJDouble(key: String): java.lang.Double = bundle.getDouble(key)
    implicit def putJDouble(key: String, value: java.lang.Double) { bundle.putDouble(key, value.doubleValue) }
    implicit def getJFloat(key: String): java.lang.Float = bundle.getFloat(key)
    implicit def putJFloat(key: String, value: java.lang.Float) { bundle.putFloat(key, value.floatValue) }
  }
  private implicit def toRichBundle(bundle: Bundle): RichBundle = new RichBundle(bundle)
  private class RichCursor(cursor: Cursor) {
    def getByte(index: Int): Byte = cursor.getShort(index).asInstanceOf[Byte]
    def getJLong(index: Int): java.lang.Long = cursor.getLong(index).asInstanceOf[java.lang.Long]
    def getJInt(index: Int): java.lang.Integer = cursor.getInt(index).asInstanceOf[java.lang.Integer]
    def getJShort(index: Int): java.lang.Short = cursor.getShort(index).asInstanceOf[java.lang.Short]
    def getJByte(index: Int): java.lang.Byte = cursor.getShort(index).asInstanceOf[java.lang.Byte]
    def getJDouble(index: Int): java.lang.Double = cursor.getDouble(index).asInstanceOf[java.lang.Double]
    def getJFloat(index: Int): java.lang.Float = cursor.getFloat(index).asInstanceOf[java.lang.Float]
  }
  private implicit def toRichCursor(cursor: Cursor): RichCursor = new RichCursor(cursor)
  implicit lazy val stringType: PersistedType[String] = directPersistedType[String]("TEXT", _.getString, _.put, _.getString, _.putString)
  implicit lazy val longRefType: PersistedType[java.lang.Long] = directPersistedType[java.lang.Long]("INTEGER", _.getJLong, _.put, _.getJLong, _.putJLong)
  implicit lazy val intRefType: PersistedType[java.lang.Integer] = directPersistedType[java.lang.Integer]("INTEGER", _.getJInt, _.put, _.getJInt, _.putJInt)
  implicit lazy val shortRefType: PersistedType[java.lang.Short] = directPersistedType[java.lang.Short]("INTEGER", _.getJShort, _.put, _.getJShort, _.putJShort)
  implicit lazy val byteRefType: PersistedType[java.lang.Byte] = directPersistedType[java.lang.Byte]("INTEGER", _.getJByte, _.put, _.getJByte, _.putJByte)
  implicit lazy val doubleRefType: PersistedType[java.lang.Double] = directPersistedType[java.lang.Double]("REAL", _.getJDouble, _.put, _.getJDouble, _.putJDouble)
  implicit lazy val floatRefType: PersistedType[java.lang.Float] = directPersistedType[java.lang.Float]("REAL", _.getJFloat, _.put, _.getJFloat, _.putJFloat)
  implicit lazy val blobType: PersistedType[Array[Byte]] = directPersistedType[Array[Byte]]("BLOB", _.getBlob, _.put, _.getByteArray, _.putByteArray)
  implicit lazy val longType: PersistedType[Long] = castedPersistedType[Long,java.lang.Long]
  implicit lazy val intType: PersistedType[Int] = castedPersistedType[Int,java.lang.Integer]
  implicit lazy val shortType: PersistedType[Short] = castedPersistedType[Short,java.lang.Short]
  implicit lazy val byteType: PersistedType[Byte] = castedPersistedType[Byte,java.lang.Byte]
  implicit lazy val doubleType: PersistedType[Double] = castedPersistedType[Double,java.lang.Double]
  implicit lazy val floatType: PersistedType[Float] = castedPersistedType[Float,java.lang.Float]

  implicit lazy val calendarLongType: PersistedType[Calendar] = convertedPersistedType[Calendar,Long](_.getTimeInMillis, persisted => {
    val calendar = new GregorianCalendar
    calendar.setTimeInMillis(persisted)
    Some(calendar)
  })

  implicit lazy val dateLongType: PersistedType[Date] = convertedPersistedType[Date,Long](_.getTime, persisted => {
    Some(new Date(persisted))
  })

  def enumStringType[E <: Ordered[_]](enumeration: Enumeration)(implicit m: Manifest[E]): PersistedType[E] =
    convertedPersistedType[E,String](_.toString, persisted => enumeration.valueOf(persisted).asInstanceOf[Option[E]])

  /**
   * @param P the persisted type
   * @param T the value type
   */
  def convertedPersistedType[T,P](toPersisted: T => P, toValue: P => Option[T])(
          implicit persistedType: PersistedType[P], valueManifest: Manifest[T]): PersistedType[T] = {
    new ConvertedPersistedType[T,P](toValue, toPersisted)
  }

  /**
   * @param P the persisted type
   * @param T the value type
   */
  def castedPersistedType[T,P](implicit persistedType: PersistedType[P], valueManifest: Manifest[T]): PersistedType[T] = {
    convertedPersistedType[T,P](v => v.asInstanceOf[P], p => Option(p.asInstanceOf[T]))
  }

  //doesn't require an Option.
  private def directPersistedType[T <: AnyRef](sqliteType: String,
                                               cursorGetter: Cursor => Int => T, contentValuesPutter: ContentValues => (String, T) => Unit,
                                               bundleGetter: Bundle => String => T, bundlePutter: Bundle => (String, T) => Unit)
                                              (implicit valueManifest: Manifest[T]) =
    new DirectPersistedType(sqliteType, c => index => Some(cursorGetter(c)(index)), contentValuesPutter, b => k => Some(bundleGetter(b)(k)), bundlePutter)
}
