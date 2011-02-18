package geeks.crud.android

import android.database.Cursor
import android.content.ContentValues
import java.util.{GregorianCalendar, Calendar}

trait PersistedType[T] {
  def valueManifest: Manifest[T]

  def getValue(cursor: Cursor, cursorIndex: Int): T

  def putValue(contentValues: ContentValues, name: String, value: T)
}

/**
 * An android PersistedType based on the {@link Cursor} and {@link ContentValues} api's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:30 AM
 */

private class ConvertedPersistedType[T,P](toValue: P => T, toPersisted: T => P)
                                          (implicit persistedType: PersistedType[P], implicit val valueManifest: Manifest[T])
        extends PersistedType[T] {
  def putValue(contentValues: ContentValues, name: String, value: T) {
    persistedType.putValue(contentValues, name, toPersisted(value))
  }

  def getValue(cursor: Cursor, cursorIndex: Int): T = toValue(persistedType.getValue(cursor, cursorIndex))
}

private class DirectPersistedType[T <: AnyRef](cursorGetter: Cursor => Int => T, contentValuesPutter: ContentValues => (String, T) => Unit)
                                      (implicit val valueManifest: Manifest[T]) extends PersistedType[T] {
  def getValue(cursor: Cursor, cursorIndex: Int): T = cursorGetter(cursor)(cursorIndex)
  def putValue(contentValues: ContentValues, name: String, value: T) {
    contentValuesPutter(contentValues)(name, value)
  }
}

private class CastedPersistedType[T <: AnyVal, R <: AnyRef](implicit refType: PersistedType[R],
                                                               implicit val valueManifest: Manifest[T])
        extends PersistedType[T] {
  def putValue(contentValues: ContentValues, name: String, value: T) {
    refType.putValue(contentValues, name, value.asInstanceOf[R])
  }

  def getValue(cursor: Cursor, cursorIndex: Int) = refType.getValue(cursor, cursorIndex).asInstanceOf[T]
}

object PersistedType {
  implicit lazy val stringType: PersistedType[String] = new DirectPersistedType[String](_.getString, _.put)
  implicit lazy val longRefType: PersistedType[java.lang.Long] = new DirectPersistedType[java.lang.Long](_.getLong, _.put)
  implicit lazy val intRefType: PersistedType[java.lang.Integer] = new DirectPersistedType[java.lang.Integer](_.getInt, _.put)
  implicit lazy val shortRefType: PersistedType[java.lang.Short] = new DirectPersistedType[java.lang.Short](_.getShort, _.put)
  implicit lazy val byteRefType: PersistedType[java.lang.Byte] = new DirectPersistedType[java.lang.Byte](getByte, _.put)
  implicit lazy val doubleRefType: PersistedType[java.lang.Double] = new DirectPersistedType[java.lang.Double](_.getDouble, _.put)
  implicit lazy val floatRefType: PersistedType[java.lang.Float] = new DirectPersistedType[java.lang.Float](_.getFloat, _.put)
  implicit lazy val blobType: PersistedType[Array[Byte]] = new DirectPersistedType[Array[Byte]](_.getBlob, _.put)
  implicit lazy val longType: PersistedType[Long] = castedPersistedType[Long,java.lang.Long]
  implicit lazy val intType: PersistedType[Int] = castedPersistedType[Int,java.lang.Integer]
  implicit lazy val shortType: PersistedType[Short] = castedPersistedType[Short,java.lang.Short]
  implicit lazy val byteType: PersistedType[Byte] = castedPersistedType[Byte,java.lang.Byte]
  implicit lazy val doubleType: PersistedType[Double] = castedPersistedType[Double,java.lang.Double]
  implicit lazy val floatType: PersistedType[Float] = castedPersistedType[Float,java.lang.Float]

  implicit lazy val calendarLongType: PersistedType[Calendar] = convertedPersistedType[Calendar,Long](_.getTimeInMillis, persisted => {
    val calendar = new GregorianCalendar
    calendar.setTimeInMillis(persisted)
    calendar
  })

  private def getByte(cursor: Cursor)(index: Int): Byte = {
    cursor.getShort(index).asInstanceOf[Byte]
  }

  /**
   * @param P the persisted type
   * @param T the value type
   */
  def convertedPersistedType[T,P](toPersisted: T => P, toValue: P => T)(
          implicit persistedType: PersistedType[P], valueManifest: Manifest[T]): PersistedType[T] = {
    new ConvertedPersistedType[T,P](toValue, toPersisted)
  }

  /**
   * @param P the persisted type
   * @param T the value type
   */
  def castedPersistedType[T,P](implicit persistedType: PersistedType[P], valueManifest: Manifest[T]): PersistedType[T] = {
    convertedPersistedType[T,P](_.asInstanceOf[P], _.asInstanceOf[T])
  }
}
