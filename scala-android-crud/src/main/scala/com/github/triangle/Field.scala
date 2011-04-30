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
 *   Field[String](persisted("name"), viewId(R.id.name, textView))),
 *   Field[Double](persisted("score"), viewId(R.id.score, formatted[Double](textView)))
 * )
 * </pre>
 * <p>
 * Usage of implicits make this syntax concise for the simple cases, but allow for very complex situations as well
 * by providing custom implementations for the implicits.
 */
final class Field[T](access: PartialFieldAccess[T]) extends CopyableField with Logging {
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
   * Gets the value, similar to {@link Map#apply}, and the value must not be None.
   * @see #findValue
   */
  def apply(readable: AnyRef): T = access.getter(readable).get

  /**
   * Finds a value of out <code>from</code>.
   * @returns Some(Some(value)) if successful, Some(None) if a PartialFieldAccess applied but the value was None,
   * or None if no PartialFieldAccess applied.
   */
  def findOptionalValue(from: AnyRef): Option[Option[T]] = access.getter.lift(from)

  /**
   * Sets a value in <code>to</code> by using all FieldAccesses that can handle it.
   * @return true if any were successful
   */
  def setValue(to: AnyRef, value: Option[T]): Boolean = {
    val defined = access.setter.isDefinedAt(to)
    if (defined) access.setter(to)(value)
    if (!defined) debug("Unable to set value of field " + this + " into " + to + " to " + value + ".")
    defined
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

  /**
   * Traverses all of the FieldAccesses in this Field, returning the desired information.
   * Anything not matched will be traversed deeper, if possible, or else ignored.
   * <pre>
   *   fieldAccessFlatMap {
   *     case foo: BarFieldAccess => List(foo.myInfo)
   *   }
   * </pre>
   */
  def fieldAccessFlatMap[B](f: PartialFunction[PartialFieldAccess[_], Traversable[B]]): List[B] = access.flatMap(f).toList
}

/**
 * The base trait of all FieldAccesses.  This is based on PartialFunction.
 * @param T the value type that this FieldAccess consumes and provides.
 * @see #getter
 * @see #setter
 */
trait PartialFieldAccess[T] {
  /**
   * PartialFunction for getting an optional value from an AnyRef.
   */
  def getter: PartialFunction[AnyRef,Option[T]]

  /**
   * Finds a value out of <code>readable</code>.
   * @returns Some(value) if successful, otherwise None (whether this PartialFieldAccess didn't apply or because the value was None)
   * @see #getter to differentiate the two None cases
   */
  def findValue(readable:AnyRef): Option[T] = getter.orElse(Field.alwaysNone)(readable)

  /**
   * Gets the value, similar to {@link Map#apply}
   */
  def apply(readable:AnyRef): T = findValue(readable).get

  /**
   * PartialFunction for setting an optional value in an AnyRef.
   */
  def setter: PartialFunction[AnyRef,Option[T] => Unit]

  /**
   * Adds two PartialFieldAccess objects together.
   */
  def +(access: PartialFieldAccess[T]): PartialFieldAccess[T] = Field.variations(this, access)

  /**
   * Traverses all of the PartialFieldAccesses in this PartialFieldAccess, returning the desired information.
   * Anything not matched will be traversed deeper, if possible, or else ignored.
   * <pre>
   *   flatMap {
   *     case foo: BarFieldAccess => List(foo.myInfo)
   *   }
   * </pre>
   */
  def flatMap[B](f: PartialFunction[PartialFieldAccess[_], Traversable[B]]): Traversable[B] = {
    f.lift(this) match {
      case Some(t: Traversable[B]) => t
      case None => None
    }
  }
}

trait DelegatingPartialFieldAccess[T] extends PartialFieldAccess[T] {
  protected def delegate: PartialFieldAccess[T]

  def getter = delegate.getter

  def setter = delegate.setter

  override def flatMap[B](f: PartialFunction[PartialFieldAccess[_], Traversable[B]]) = {
    f.lift(this).getOrElse(delegate.flatMap(f))
  }
}

/**
 * {@PartialFieldAccess} support for getting a value as an Option if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class FieldGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PartialFieldAccess[T] with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): Option[T]

  def getter = { case readable: R if readableManifest.erasure.isInstance(readable) => get(readable) }
}

trait NoSetter[T] extends PartialFieldAccess[T] {
  def setter = Field.emptyPartialFunction
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

  def setter = {
    case writable: W if writableManifest.erasure.isInstance(writable) => set(writable, _)
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
   * Combines all of the fieldAccesses' getters, using the first one that is applicable.
   */
  lazy val getter = fieldAccesses.tail.foldLeft(fieldAccesses.head.getter)((result, access) => result.orElse(access.getter))

  /**
   * Combines all of fieldAccesses' setters, calling all that are applicable.
   */
  lazy val setter = new PartialFunction[AnyRef,Option[T] => Unit] {
    def isDefinedAt(x: AnyRef) = fieldAccesses.exists(_.setter.isDefinedAt(x))

    def apply(writable: AnyRef) = { value =>
      val definedAccesses = fieldAccesses.filter(_.setter.isDefinedAt(writable))
      if (definedAccesses.isEmpty) {
        throw new MatchError("setter in " + FieldAccessVariations.this)
      } else {
        definedAccesses.foreach(_.setter(writable)(value))
      }
    }
  }

  override def flatMap[B](f: PartialFunction[PartialFieldAccess[_], Traversable[B]]) = {
    val lifted = f.lift
    fieldAccesses.flatMap(access => lifted(access) match {
      case Some(t: Traversable[B]) => t
      case None => access.flatMap(f)
    })
  }
}

/**
 * Factory methods for basic FieldAccesses.  This should be imported as Field._.
 */
object Field {
  def emptyPartialFunction[A,B] = new PartialFunction[A,B] {
    def isDefinedAt(x: A) = false

    def apply(v1: A) = throw new MatchError("emptyPartialFunction")
  }

  def alwaysNone[T]: PartialFunction[AnyRef,Option[T]] = { case _ => None }

  //This is here so that getters can be written more simply by not having to explicitly wrap the result in a "Some".
  implicit def toSome[T](value: T): Option[T] = Some(value)

  /** Defines read-only fieldAccess for a field value for a Readable type. */
  def readOnly[R,T](getter1: R => Option[T])
                   (implicit typeManifest: ClassManifest[R]): FieldGetter[R,T] = {
    new FieldGetter[R,T] with NoSetter[T] {
      def get(readable: R) = getter1(readable)
    }
  }

  /** Defines write-only fieldAccess for a field value for a Writable type. */
  def writeOnly[W,T](setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                    (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] = {
    new FieldSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter1(writable)(value)
          case None => clearer(writable)
        }
      }

      def getter = emptyPartialFunction
    }
  }

  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PartialFieldAccess[T] = new PartialFieldAccess[T] with NoSetter[T] {
    def getter = { case Unit => Some(value) }
  }

  /**
   * Defines a flow for a field value from a Readable type to a Writable type.
   * The value never actually is taken directly from the Readable and set in the Writable.
   * It is copied to and from other objects.
   * @param R the Readable type to get the value out of
   * @param W the Writable type to put the value into
   * @param T the value type
   */
  def flow[R,W,T](getter1: R => Option[T], setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): FieldAccess[R,W,T] = {
    new FieldAccess[R,W,T] {
      def get(readable: R) = getter1(readable)

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter1(writable)(value)
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

  def formatted[T](format: ValueFormat[T], access: PartialFieldAccess[String]) = new PartialFieldAccess[T] {
    def getter = access.getter.andThen(value => value.flatMap(format.toValue(_)))

    def setter = access.setter.andThen(setter => setter.compose(value => value.map(format.toString _)))
  }

  /**
   * formatted replacement for primitive values.
   */
  def formatted[T <: AnyVal](access: PartialFieldAccess[String])(implicit m: Manifest[T]): PartialFieldAccess[T] =
    formatted(new BasicValueFormat[T](), access)

  /**
   * Allow creating a Field without using "new".
   */
  def apply[T](access: PartialFieldAccess[T]): Field[T] = new Field[T](access)
}
