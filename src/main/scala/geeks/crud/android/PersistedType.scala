package geeks.crud.android

import android.database.Cursor
import android.content.ContentValues


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
  implicit val stringType: PersistedType[String] = new DirectPersistedType[String](_.getString, _.put)
  implicit val longRefType: PersistedType[java.lang.Long] = new DirectPersistedType[java.lang.Long](_.getLong, _.put)
  implicit val intRefType: PersistedType[java.lang.Integer] = new DirectPersistedType[java.lang.Integer](_.getInt, _.put)
  implicit val shortRefType: PersistedType[java.lang.Short] = new DirectPersistedType[java.lang.Short](_.getShort, _.put)
  implicit val byteRefType: PersistedType[java.lang.Byte] = new DirectPersistedType[java.lang.Byte](getByte, _.put)
  implicit val doubleRefType: PersistedType[java.lang.Double] = new DirectPersistedType[java.lang.Double](_.getDouble, _.put)
  implicit val floatRefType: PersistedType[java.lang.Float] = new DirectPersistedType[java.lang.Float](_.getFloat, _.put)
  implicit val blobType: PersistedType[Array[Byte]] = new DirectPersistedType[Array[Byte]](_.getBlob, _.put)
  implicit val longType: PersistedType[Long] = new CastedPersistedType[Long,java.lang.Long]()
  implicit val intType: PersistedType[Int] = new CastedPersistedType[Int,java.lang.Integer]()
  implicit val shortType: PersistedType[Short] = new CastedPersistedType[Short,java.lang.Short]()
  implicit val byteType: PersistedType[Byte] = new CastedPersistedType[Byte,java.lang.Byte]()
  implicit val doubleType: PersistedType[Double] = new CastedPersistedType[Double,java.lang.Double]()
  implicit val floatType: PersistedType[Float] = new CastedPersistedType[Float,java.lang.Float]()

  private def getByte(cursor: Cursor)(index: Int): Byte = {
    cursor.getShort(index).asInstanceOf[Byte]
  }
}
