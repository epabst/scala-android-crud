package com.github.triangle

import com.github.scala_android.crud.monitor.Logging
import collection.Map

/** A trait for {@link Field} for convenience such as when defining a List of heterogeneous Fields. */
trait CopyableField {
  /**
   * Copies this field from <code>from</code> to <code>to</code>.
   * @returns true if successfully set a value
   */
  def copy(from: AnyRef, to: AnyRef): Boolean
}

/**
 * A Field of a specific type which has any number of FieldAccesses to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import com.github.scala_android.crud._
 * import com.github.scala_android.crud.CursorFieldAccess._
 * import com.github.scala_android.crud.PersistedType._
 * import com.github.scala_android.crud.ViewFieldAccess._
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
final class Field[T](fieldAccessArgs: PartialFieldAccess[T]*) extends CopyableField with Logging {
  val fieldAccesses: List[PartialFieldAccess[T]] = fieldAccessArgs.toList
  /**
   * Finds a value out of <code>from</code> by using the FieldAccess that can handle it.
   * @returns Some(value) if successful, otherwise None (whether because no PartialFieldAccess applied or because the value was None)
   * @see #findOptionalValue to differentiate the two None cases
   */
  def findValue(from: AnyRef): Option[T] = findOptionalValue(from).getOrElse {
    debug("Unable to find value in " + from + " for field " + this)
    None
  }

  /**
   * Finds a value of out <code>from</code>.
   * @returns Some(Some(value)) if successful, Some(None) if a PartialFieldAccess applied but the value was None,
   * or None if no PartialFieldAccess applied.
   */
  def findOptionalValue(from: AnyRef): Option[Option[T]] = {
    for (fieldAccess <- fieldAccesses) {
      val value = fieldAccess.partialGet(from)
      if (value.isDefined) return value
    }
    None
  }

  /**
   * Sets a value in <code>to</code> by using all FieldAccesses that can handle it.
   * @return true if any were successful
   */
  def setValue(to: AnyRef, value: Option[T]): Boolean = {
    val result = fieldAccesses.foldLeft(false)((result, access) => access.partialSet(to, value) || result)
    if (!result) debug("Unable to set value of field " + this + " into " + to + " to " + value + ".")
    result
  }

  //inherited
  def copy(from: AnyRef, to: AnyRef): Boolean = {
    findOptionalValue(from) match {
      case Some(optionalValue) =>
        debug("Copying " + optionalValue + " from " + from + " to " + to + " for field " + this)
        setValue(to, optionalValue)
      case None =>
        false
    }
  }
}

/**
 * The base trait of all FieldAccesses.  This is based on PartialFunction.
 * @param T the value type that this FieldAccess consumes and provides.
 * @see #partialGet
 * @see #partialSet
 */
trait PartialFieldAccess[T] {
  /**
   * Tries to get the value from <code>readable</code>.
   * @param readable any kind of Object.  If it is not supported by this FieldAccess, this simply returns None.
   * @returns Some(Some(value)) if successful, Some(None) if a PartialFieldAccess applied but the value was None,
   * or None if no PartialFieldAccess applied.
   */
  def partialGet(readable: AnyRef): Option[Option[T]]

  /**
   * Finds a value out of <code>readable</code>.
   * @returns Some(value) if successful, otherwise None (whether this PartialFieldAccess didn't apply or because the value was None)
   * @see #partialGet(AnyRef) to differentiate the two None cases
   */
  def findValue(readable:AnyRef): Option[T] = partialGet(readable).getOrElse(None)

  /**
   * Tries to set the value in <code>writable</code>.
   * @param writable any kind of Object.  If it is not supported by this FieldAccess, this simply returns false.
   * @param value the value in an Option.  It will be None if the partialGet of the readable returned None.
   */
  def partialSet(writable: AnyRef, value: Option[T]): Boolean
}

/**
 * {@PartialFieldAccess} support for getting a value as an Option if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class FieldGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PartialFieldAccess[T] with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): Option[T]

  def partialGet(readable: AnyRef) = {
    debug("Seeing if " + readable + " is an instance of " + readableManifest.erasure + " to get value")
    if (readable == null) throw new IllegalArgumentException("'readable' may not be null")
    if (readableManifest.erasure.isInstance(readable))
      Some(get(readable.asInstanceOf[R]))
    else
      None
  }
}

trait NoSetter[T] extends PartialFieldAccess[T] {
  def partialSet(writable: AnyRef, value: Option[T]) = false
}

/**
 * {@PartialFieldAccess} support for setting a value if <code>writable</code> is of type W.
 * This is a trait so that it can be mixed with FieldGetter.
 * @param W the Writable type to put the value into
 */
trait FieldSetter[W,T] extends PartialFieldAccess[T] with Logging {
  protected def writableManifest: ClassManifest[W]

  /** An abstract method that must be implemented by subtypes. */
  def set(writable: W, value: Option[T])

  override def partialSet(writable: AnyRef, value: Option[T]) = {
    debug("Seeing if " + writable + " is an instance of " + writableManifest.erasure + " to set value " + value)
    if (writable == null) throw new IllegalArgumentException("'writable' may not be null")
    if (writableManifest.erasure.isInstance(writable)) {
      set(writable.asInstanceOf[W], value)
      true
    } else false
  }
}

/**
 * {@PartialFieldAccess} support for getting and setting a value if <code>readable</code> and <code>writable</code>
 * are of the types R and W respectively.
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class FieldAccess[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends FieldGetter[R,T] with FieldSetter[W,T] {
  protected def writableManifest = _writableManifest
}

trait FieldAccessVariations[T] extends PartialFieldAccess[T] {
  def fieldAccesses: List[PartialFieldAccess[T]]

  /**
   * Gets a value out of <code>readable</code> by using the first FieldAccess that can handle it.
   * @returns Some(value) if successful, otherwise None
   */
  def partialGet(readable: AnyRef): Option[Option[T]] = {
    for (fieldAccess <- fieldAccesses) {
      val value = fieldAccess.partialGet(readable)
      if (value.isDefined) return value
    }
    None
  }

  /**
   * Sets a value in <code>writable</code> by using all FieldAccesses that can handle it.
   * @return true if any were successful
   */
  def partialSet(writable: AnyRef, value: Option[T]) = {
    fieldAccesses.foldLeft(false)((result, access) => access.partialSet(writable, value) || result)
  }
}

/**
 * Factory methods for basic FieldAccesses.  This should be imported as Field._.
 */
object Field {
  //This is here so that getters can be written more simply by not having to explicitly wrap the result in a "Some".
  implicit def toSome[T](value: T): Option[T] = Some(value)

  /** Defines read-only fieldAccess for a field value for a Readable type. */
  def readOnly[R,T](getter: R => Option[T])
                   (implicit typeManifest: ClassManifest[R]): FieldGetter[R,T] = {
    new FieldGetter[R,T] with NoSetter[T] {
      def get(readable: R) = getter(readable)
    }
  }

  /** Defines write-only fieldAccess for a field value for a Writable type. */
  def writeOnly[W,T](setter: W => T => Unit, clearer: W => Unit = {_: W => })
                    (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] = {
    new FieldSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter(writable)(value)
          case None => clearer(writable)
        }
      }

      def partialGet(readable: AnyRef) = None
    }
  }

  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PartialFieldAccess[T] = new PartialFieldAccess[T] with NoSetter[T] {
    def partialGet(readable: AnyRef) = readable match {
      case Unit => Some(Some(value))
      case _ => None
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
  def flow[R,W,T](getter: R => Option[T], setter: W => T => Unit, clearer: W => Unit = {_: W => })
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): FieldAccess[R,W,T] = {
    new FieldAccess[R,W,T] {
      def get(readable: R) = getter(readable)

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter(writable)(value)
          case None => clearer(writable)
        }
      }
    }
  }

  /**
   *  Defines fieldAccess for a field value using a setter and getter.
   * @param M any mutable type
   * @param T the value type
   */
  def fieldAccess[M,T](getter: M => Option[T], setter: M => T => Unit, clearer: M => Unit = {_: M => })
                 (implicit typeManifest: ClassManifest[M]): FieldAccess[M,M,T] = flow[M,M,T](getter, setter, clearer)

  def mapAccess[T](name: String): FieldAccess[Map[String,_ <: T],collection.mutable.Map[String,_ >: T],T] =
    flow(_.get(name), m => v => m.put(name, v), _.remove(name))

  def variations[T](fieldAccessArgs: PartialFieldAccess[T]*): PartialFieldAccess[T] = new FieldAccessVariations[T] {
    val fieldAccesses: List[PartialFieldAccess[T]] = fieldAccessArgs.toList
  }

  /**
   * Allow creating a Field without using "new".
   */
  def apply[T](fieldAccesses: PartialFieldAccess[T]*): Field[T] = new Field[T](fieldAccesses :_*)
}
