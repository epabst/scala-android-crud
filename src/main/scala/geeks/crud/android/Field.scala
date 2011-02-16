package geeks.crud.android

import android.view.View
import geeks.crud.{BasicValueFormat, ValueFormat}
import geeks.crud.util.Logging
import android.widget.SimpleCursorAdapter.ViewBinder
import android.widget.{DatePicker, SimpleCursorAdapter, TextView, EditText}
import java.util.{Calendar, Date, GregorianCalendar}
import PersistedType._

class AccessField[V](val accesses: PartialAccess[V]*) {
  def findValue(from: AnyRef): Option[V] = {
    for (access <- accesses) {
      val value = access.partialGet(from)
      if (value.isDefined) return value
    }
    None
  }

  /** do the partialSet on all accesses, and return true if any were successful. */
  def setValue(to: AnyRef, value: V): Boolean = {
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
 * @param V the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
trait PartialAccess[V] {
  def partialGet(readable: AnyRef): Option[V]

  def partialSet(writable: AnyRef, value: V): Boolean
}

/**
 * @param V the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class TypeGetter[R,V](implicit readableManifest: ClassManifest[R]) extends PartialAccess[V] {
  def get(readable: R): V

  final def partialGet(readable: AnyRef) = {
    if (readableManifest.erasure.isInstance(readable)) Some(get(readable.asInstanceOf[R])) else None
  }
}

/**
 * @param W the Writable type to put the value into
 */
trait TypeSetter[W,V] extends PartialAccess[V] {
  protected def writableManifest: ClassManifest[W]

  def set(writable: W, value: V)

  final override def partialSet(writable: AnyRef, value: V) = {
    if (writableManifest.erasure.isInstance(writable)) {
      set(writable.asInstanceOf[W], value)
      true
    } else false
  }
}

/**
 * @param V the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class TypeAccess[R,W,V](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends TypeGetter[R,V] with TypeSetter[W,V] {
  protected def writableManifest = _writableManifest
}

object Field {
  /** Defines read-only access for a field value for a type. */
  def readOnly[T,V](getter: T => V)
                   (implicit typeManifest: ClassManifest[T]): TypeGetter[T,V] = {
    new TypeGetter[T,V] {
      def get(readable: T) = getter(readable)

      def partialSet(writable: AnyRef, value: V) = false
    }
  }

  /** Defines write-only access for a field value for a type. */
  def writeOnly[T,V](setter: T => V => Unit)
                    (implicit typeManifest: ClassManifest[T]): TypeSetter[T,V] = {
    new TypeSetter[T,V] {
      protected def writableManifest = typeManifest

      def set(writable: T, value: V) = setter(writable)(value)

      def partialGet(readable: AnyRef) = None
    }
  }

  /** Defines a flow for a field value from one type to another type. */
  def flow[R,W,V](getter: R => V, setter: W => V => Unit)
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): TypeAccess[R,W,V] = {
    new TypeAccess[R,W,V] {
      def get(readable: R) = getter(readable)

      def set(writable: W, value: V) = setter(writable)(value)
    }
  }

  /** Defines access for a field value for a type. */
  def access[T,V](getter: T => V, setter: T => V => Unit)
                 (implicit typeManifest: ClassManifest[T]): TypeAccess[T,T,V] = flow[T,T,V](getter, setter)

  def apply[V](accesses: PartialAccess[V]*): AccessField[V] = new AccessField[V](accesses :_*)
}

