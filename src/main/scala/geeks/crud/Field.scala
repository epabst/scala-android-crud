package geeks.crud.android

import geeks.crud.util.Logging

/** A trait for {@link Field} for convenience such as when defining a List of heterogeneous Fields. */
trait CopyableField {
  def copy(from: AnyRef, to: AnyRef): Boolean
}

/**
 * A Field of a specific type which has any number of Accesses to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import geeks.crud.android._
 * import geeks.crud.android.CursorField._
 * import geeks.crud.android.PersistedType._
 * import geeks.crud.android.ViewAccess._
 * import android.widget.{TextView, ListView}
 *
 * val fields = List(
 *   new Field[String](persisted("name"), viewId[TextView,String](R.id.name)),
 *   new Field[Double](persisted("score"), viewId[TextView,Double](R.id.score))
 * )
 * </pre>
 * <p>
 * Usage of implicits make this syntax concise for the simple cases, but allow for very complex situations as well
 * by providing custom implementations for the implicits.
 */
final class Field[T](val accesses: PartialAccess[T]*) extends CopyableField with Logging {
  /**
   * Finds a value out of <code>from</code> by using the Access that can handle it.
   * @returns Some(value) if successful, otherwise None
   */
  def findValue(from: AnyRef): Option[T] = {
    for (access <- accesses) {
      val value = access.partialGet(from)
      if (value.isDefined) return value
    }
    None
  }

  /**
   * Sets a value in <code>to</code> by using all Accesses that can handle it.
   * @return true if any were successful
   */
  def copy(from: AnyRef, to: AnyRef): Boolean = {
    findValue(from).map(value => {
      debug("Copying " + value + " from " + from + " to " + to + " for field " + this)
      setValue(to, value)
    }).getOrElse(false)
  }
}

/**
 * The base trait of all Accesses.  This is based on PartialFunction.
 * @param T the value type that this Access consumes and provides.
 * @see #partialGet
 * @see #partialSet
 */
trait PartialAccess[T] {
  /**
   * Tries to get the value from <code>readable</code>.
   * @param readable any kind of Object.  If it is not supported by this Access, this simply returns None.
   * @returns Some(value) if successful, otherwise None
   */
  def partialGet(readable: AnyRef): Option[T]

  /**
   * Tries to set the value in <code>writable</code>.
   * @param writable any kind of Object.  If it is not supported by this Access, this simply returns false.
   */
  def partialSet(writable: AnyRef, value: T): Boolean
}

/**
 * {@PartialAccess} support for getting a value if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class TypeGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PartialAccess[T] {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): T

  final def partialGet(readable: AnyRef) = {
    if (readableManifest.erasure.isInstance(readable)) Some(get(readable.asInstanceOf[R])) else None
  }
}

/**
 * {@PartialAccess} support for setting a value if <code>writable</code> is of type W.
 * This is a trait so that it can be mixed with TypeGetter.
 * @param W the Writable type to put the value into
 */
trait TypeSetter[W,T] extends PartialAccess[T] {
  protected def writableManifest: ClassManifest[W]

  /** An abstract method that must be implemented by subtypes. */
  def set(writable: W, value: T)

  final override def partialSet(writable: AnyRef, value: T) = {
    if (writableManifest.erasure.isInstance(writable)) {
      set(writable.asInstanceOf[W], value)
      true
    } else false
  }
}

/**
 * {@PartialAccess} support for getting and setting a value if <code>readable</code> and <code>writable</code>
 * are of the types R and W respectively.
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class TypeAccess[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends TypeGetter[R,T] with TypeSetter[W,T] {
  protected def writableManifest = _writableManifest
}

/**
 * Factory methods for basic Accesses.  This should be imported as Field._.
 */
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

  /**
   * Defines a flow for a field value from a Readable type to a Writable type.
   * The value never actually is taken directly from the Readable and set in the Writable.
   * It is copied to and from other objects.
   * @param R the Readable type to get the value out of
   * @param W the Writable type to put the value into
   * @param T the value type
   */
  def flow[R,W,T](getter: R => T, setter: W => T => Unit)
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): TypeAccess[R,W,T] = {
    new TypeAccess[R,W,T] {
      def get(readable: R) = getter(readable)

      def set(writable: W, value: T) = setter(writable)(value)
    }
  }

  /**
   *  Defines access for a field value using a setter and getter.
   * @param M any mutable type
   * @param T the value type
   */
  def access[M,T](getter: M => T, setter: M => T => Unit)
                 (implicit typeManifest: ClassManifest[M]): TypeAccess[M,M,T] = flow[M,M,T](getter, setter)

  /**
   * Allow creating a Field without using "new".
   */
  def apply[T](accesses: PartialAccess[T]*): Field[T] = new Field[T](accesses :_*)
}

