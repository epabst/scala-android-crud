package geeks.crud.android

import android.view.View
import geeks.crud.{BasicValueFormat, ValueFormat}
import geeks.crud.util.Logging
import android.widget.SimpleCursorAdapter.ViewBinder
import android.widget.{DatePicker, SimpleCursorAdapter, TextView, EditText}
import java.util.{Calendar, Date, GregorianCalendar}
import PersistedType._

class AccessField[T](val accesses: PartialAccess[T]*) {
  def findValue(from: AnyRef): Option[T] = {
    for (access <- accesses) {
      val value = access.partialGet(from)
      if (value.isDefined) return value
    }
    None
  }

  /** do the partialSet on all accesses, and return true if any were successful. */
  def setValue(to: AnyRef, value: T): Boolean = {
    accesses.foldLeft(false)((result, access) => access.partialSet(to, value) || result)
  }

  def copy(from: AnyRef, to: AnyRef): Boolean = {
    findValue(from).map(value => setValue(to, value)).getOrElse(false)
  }
}

/**
 * A Field that may be editable in a View and/or persisted.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 * @param R the type being read from such as Cursor
 * @param W the type being written to such as ContentValues
 */
trait Field[R,W] {
  def queryFieldNames: List[String]

  def readIntoView(readable: R, entryView: View)

  def writeFromView(entryView: View, writable: W)
}

/** The base trait of all displayed fields. If it doesn't extend ViewField it will not be displayed by default. */
trait ViewField[R,W] extends Field[R,W] {
  def viewResourceId: Int

  def readIntoFieldView(readable: R, fieldView: View)

  def writeFromFieldView(fieldView: View, writable: W)

  final def readIntoView(readable: R, entryView: View) {
    readIntoFieldView(readable, entryView.findViewById(viewResourceId))
  }

  final def writeFromView(entryView: View, writable: W) {
    writeFromFieldView(entryView.findViewById(viewResourceId), writable)
  }
}

/**
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
trait PartialAccess[T] {
  def partialGet(readable: AnyRef): Option[T]

  def partialSet(writable: AnyRef, value: T): Boolean
}

/**
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class TypeGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PartialAccess[T] {
  def get(readable: R): T

  final def partialGet(readable: AnyRef) = {
    if (readableManifest.erasure.isInstance(readable)) Some(get(readable.asInstanceOf[R])) else None
  }
}

/**
 * @param W the Writable type to put the value into
 */
trait TypeSetter[W,T] extends PartialAccess[T] {
  protected def writableManifest: ClassManifest[W]

  def set(writable: W, value: T)

  final override def partialSet(writable: AnyRef, value: T) = {
    if (writableManifest.erasure.isInstance(writable)) {
      set(writable.asInstanceOf[W], value)
      true
    } else false
  }
}

/**
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class TypeAccess[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends TypeGetter[R,T] with TypeSetter[W,T] {
  protected def writableManifest = _writableManifest
}

object Field {
  /** Defines read-only access for a field value for a Readable type. */
  def readOnly[R,T](getter: R => T)
                   (implicit typeManifest: ClassManifest[R]): TypeGetter[R,T] = {
    new TypeGetter[R,T] {
      def get(readable: R) = getter(readable)

      def partialSet(writable: AnyRef, value: T) = false
    }
  }

  /** Defines write-only access for a field value for a Writable type. */
  def writeOnly[W,T](setter: W => T => Unit)
                    (implicit typeManifest: ClassManifest[W]): TypeSetter[W,T] = {
    new TypeSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, value: T) = setter(writable)(value)

      def partialGet(readable: AnyRef) = None
    }
  }

  /** Defines a flow for a field value from a Readable type to a Writable type. */
  def flow[R,W,T](getter: R => T, setter: W => T => Unit)
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): TypeAccess[R,W,T] = {
    new TypeAccess[R,W,T] {
      def get(readable: R) = getter(readable)

      def set(writable: W, value: T) = setter(writable)(value)
    }
  }

  /** Defines access for a field value for a Mutable type. */
  def access[M,T](getter: M => T, setter: M => T => Unit)
                 (implicit typeManifest: ClassManifest[M]): TypeAccess[M,M,T] = flow[M,M,T](getter, setter)

  def apply[T](accesses: PartialAccess[T]*): AccessField[T] = new AccessField[T](accesses :_*)
}

