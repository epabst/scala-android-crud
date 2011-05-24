package com.github.triangle

import com.github.scala_android.crud.monitor.Logging
import collection._

/** A trait for {@link PortableField} for convenience such as when defining a List of heterogeneous Fields. */
trait BaseField {
  /**
   * Copies this field from <code>from</code> to <code>to</code>.
   * @returns true if successfully set a value
   */
  def copy(from: AnyRef, to: AnyRef): Boolean

  /**
   * Copies this field from the first applicable item in <code>fromItems</code> to <code>to</code>.
   * @returns true if successfully set a value
   */
  def copyFromItem(fromItems: List[AnyRef], to: AnyRef): Boolean

  /**
   * Transforms the <code>initial</code> subject using the <code>data</code> for this field..
   * @returns the transformed subject, which could be the initial instance
   */
  def transform[S <: AnyRef](initial: S, data: AnyRef): S

  /**
   * Transforms the <code>initial</code> subject using the first applicable item in <code>dataItems</code> for this field..
   * @returns the transformed subject, which could be the initial instance
   */
  def transformWithItem[S <: AnyRef](initial: S, dataItems: List[AnyRef]): S

  /**
   * Traverses all of the PortableFieldes in this PortableField, returning the desired information.
   * Anything not matched will be traversed deeper, if possible, or else ignored.
   * <pre>
   *   flatMap {
   *     case foo: BarField => List(foo.myInfo)
   *   }
   * </pre>
   */
  def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]): Traversable[B] = {
    f.lift(this) match {
      case Some(t: Traversable[B]) => t
      case None => None
    }
  }
}

/**
 * A portable field of a specific type which applies to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import com.github.triangle.PortableField._
 * import com.github.scala_android.crud.CursorField._
 * import com.github.scala_android.crud.PersistedType._
 * import com.github.scala_android.crud.ViewField._
 *
 * val fields = List(
 *   persisted[String]("name") + viewId(R.id.name, textView),
 *   persisted[Double]("score") + viewId(R.id.score, formatted[Double](textView))
 * )
 * </pre>
 * <p>
 * Usage of implicits and defaults make this syntax concise for the simple cases,
 * but allow for very complex situations as well by providing explicit values when needed.
 * @param T the value type that this PortableField gets and sets.
 * @see #getter
 * @see #setter
 */
trait PortableField[T] extends BaseField with Logging {
  /**
   * PartialFunction for getting an optional value from an AnyRef.
   */
  def getter: PartialFunction[AnyRef,Option[T]]

  /** extractor for finding the applicable items, if any. */
  private object ApplicableItems {
    def unapply(items: List[AnyRef]): Option[List[AnyRef]] = {
      val applicableItems = items.filter(getter.isDefinedAt(_))
      if (applicableItems.isEmpty) None else Some(applicableItems)
    }
  }

  /**
   * PartialFunction for getting an optional value from the first AnyRef in the List that has Some value.
   * If none of them has Some value, then it will return None if at least one of them applies.
   * If none of them even apply, the PartialFunction won't match at all (i.e. isDefinedAt will be false).
   */
  def getterFromItem: PartialFunction[List[AnyRef],Option[T]] = {
    case ApplicableItems(items) => items.view.map(getter(_)).find(_.isDefined).getOrElse(None)
  }

  /**
   * Overrides what to do if getter isn't applicable to a readable.
   * The default is to throw a MatchError.
   */
  def getOrReturn(readable: AnyRef, default: => Option[T]): Option[T] =
    getter.lift(readable).getOrElse(useDefault(default, readable))

  /**
   * Overrides what to do if getter isn't applicable to a readable.
   * The default is to throw a MatchError.
   */
  def getFromItemOrReturn(fromItems: List[AnyRef], default: => Option[T]): Option[T] =
    getterFromItem.lift(fromItems).getOrElse(useDefault(default, fromItems))

  private def useDefault(default: Option[T], readable: AnyRef): Option[T] = {
    debug("Unable to find value in " + readable + " for field " + this + ", so returning default: " + default)
    default
  }

  /**
   * Gets the value, similar to {@link Map#apply}, and the value must not be None.
   * @see #getter
   * @returns the value
   * @throws NoSuchElementException if the value was None
   * @throws MatchError if readable is not an applicable type
   */
  def apply(readable:AnyRef): T = getter(readable).get

  /**
   * PartialFunction for setting an optional value in an AnyRef.
   */
  def setter: PartialFunction[AnyRef,Option[T] => Unit]

  /**
   * Sets a value in <code>to</code> by using all embedded PortableFields that can handle it.
   * @return true if any were successful
   */
  def setValue(to: AnyRef, value: Option[T]): Boolean = {
    val defined = setter.isDefinedAt(to)
    if (defined) setter(to)(value)
    if (!defined) debug("Unable to set value of field " + this + " into " + to + " to " + value + ".")
    defined
  }

  /**
   * PartialFunction for transforming an AnyRef using an optional value.
   * This delegates to <code>setter</code> for mutable objects.
   * <code>transformer(foo)(value)<code> should return a transformed version of foo (which could be the same instance if mutable).
   * @param a subject to be transformed, whether immutable or mutable
   */
  def transformer[S <: AnyRef]: PartialFunction[S,Option[T] => S]

  private def transformUsingGetFunction[S <: AnyRef,F <: AnyRef](get: PartialFunction[F,Option[T]], initial: S, data: F) = {
    if (get.isDefinedAt(data)) {
      if (transformer.isDefinedAt(initial)) {
        val value = get(data)
        debug("Transforming " + initial + " with value " + value + " for field " + this)
        transformer(initial)(value)
      } else {
        debug("Unable to transform " + initial + " with " + data + " for field " + this + " because of transformer.")
        initial
      }
    } else {
      debug("Unable to transform " + initial + " with " + data + " for field " + this + " because of getter.")
      initial
    }
  }

  //inherited
  def transform[S <: AnyRef](initial: S, data: AnyRef) = transformUsingGetFunction[S,AnyRef](getter, initial, data)

  //inherited
  def transformWithItem[S <: AnyRef](initial: S, dataItems: List[AnyRef]) = transformUsingGetFunction[S,List[AnyRef]](getterFromItem, initial, dataItems)

  def copy(from: AnyRef, to: AnyRef): Boolean = {
    copyUsingGetFunction(getter, from, to)
  }

  //inherited
  def copyFromItem(fromItems: List[AnyRef], to: AnyRef): Boolean = {
    copyUsingGetFunction(getterFromItem, fromItems, to)
  }

  private def copyUsingGetFunction[F <: AnyRef](get: PartialFunction[F,Option[T]], from: F, to: AnyRef): Boolean = {
    val defined = get.isDefinedAt(from) && setter.isDefinedAt(to)
    if (defined) {
      val value = get(from)
      debug("Copying " + value + " from " + from + " to " + to + " for field " + this)
      setter(to)(value)
    } else {
      debug("Unable to copy field " + this + " from " + from + " to " + to + ".")
    }
    defined
  }

  /**
   * Adds two PortableField objects together.
   */
  def +(other: PortableField[T]): PortableField[T] = {
    val self = this
    new PortableField[T] {
      override def toString = self + " + " + other

      def getter = {
        case x if self.getter.isDefinedAt(x) || other.getter.isDefinedAt(x) => {
          val values = List(self.getter, other.getter).view.filter(_.isDefinedAt(x)).map(_(x))
          values.find(_.isDefined).getOrElse(None)
        }
      }

      override def getterFromItem = {
        case x if self.getterFromItem.isDefinedAt(x) || other.getterFromItem.isDefinedAt(x) => {
          val values = List(self.getterFromItem, other.getterFromItem).view.filter(_.isDefinedAt(x)).map(_(x))
          values.find(_.isDefined).getOrElse(None)
        }
      }

      /**
       * Combines the two setters, calling only applicable ones (not just the first though).
       */
      lazy val setter = new PartialFunction[AnyRef,Option[T] => Unit] {
        def isDefinedAt(x: AnyRef) = self.setter.isDefinedAt(x) || other.setter.isDefinedAt(x)

        def apply(writable: AnyRef) = { value =>
          val definedFields = List(self, other).filter(_.setter.isDefinedAt(writable))
          if (definedFields.isEmpty) {
            throw new MatchError("setter in " + PortableField.this)
          } else {
            definedFields.foreach(_.setter(writable)(value))
          }
        }
      }

      def transformer[S <: AnyRef] = {
        case subject if self.transformer.isDefinedAt(subject) || other.transformer.isDefinedAt(subject) => { value =>
          val definedFields = List(self, other).filter(_.transformer.isDefinedAt(subject))
          definedFields.foldLeft(subject)((subject, field) => field.transformer(subject)(value))
        }
      }

      override def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]) = {
        val lifted = f.lift
        List(self, other).flatMap(field => lifted(field) match {
          case Some(t: Traversable[B]) => t
          case None => field.flatMap(f)
        })
      }
    }
  }
}

trait DelegatingPortableField[T] extends PortableField[T] {
  protected def delegate: PortableField[T]

  def getter = delegate.getter

  def setter = delegate.setter

  def transformer[S <: AnyRef] = delegate.transformer

  override def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]) = {
    f.lift(this).getOrElse(delegate.flatMap(f))
  }
}

/**
 * {@PortableField} support for getting a value as an Option if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class FieldGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PortableField[T] with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): Option[T]

  def getter = { case readable: R if readableManifest.erasure.isInstance(readable) => get(readable) }
}

trait NoGetter[T] extends PortableField[T] {
  def getter = PortableField.emptyPartialFunction
}

trait NoSetter[T] extends PortableField[T] {
  def setter = PortableField.emptyPartialFunction
}

trait TransformerUsingSetter[T] extends PortableField[T] {
  def transformer[S <: AnyRef]: PartialFunction[S,Option[T] => S] = {
    case subject if setter.isDefinedAt(subject) => { value => setter(subject).apply(value); subject }
  }
}

/**
 * {@PortableField} support for setting a value if <code>writable</code> is of type W.
 * This is a trait so that it can be mixed with FieldGetter.
 * @param W the Writable type to put the value into
 */
trait FieldSetter[W,T] extends PortableField[T] with TransformerUsingSetter[T] with Logging {
  protected def writableManifest: ClassManifest[W]

  /** An abstract method that must be implemented by subtypes. */
  def set(writable: W, value: Option[T])

  def setter = {
    case writable: W if writableManifest.erasure.isInstance(writable) => set(writable, _)
  }
}

trait NoTransformer[T] extends NoSetter[T] {
  def transformer[S <: AnyRef] = PortableField.emptyPartialFunction
}

/**
 * Factory methods for basic PortableFields.  This should be imported as PortableField._.
 */
object PortableField {
  def emptyPartialFunction[A,B] = new PartialFunction[A,B] {
    def isDefinedAt(x: A) = false

    def apply(v1: A) = throw new MatchError("emptyPartialFunction")
  }

  //This is here so that getters can be written more simply by not having to explicitly wrap the result in a "Some".
  implicit def toSome[T](value: T): Option[T] = Some(value)

  /** Defines read-only field for a Readable type. */
  def readOnly[R,T](getter1: R => Option[T])
                   (implicit typeManifest: ClassManifest[R]): FieldGetter[R,T] = {
    new FieldGetter[R,T] with NoSetter[T] with NoTransformer[T] {
      def get(readable: R) = getter1(readable)

      override def toString = "readOnly[" + typeManifest.erasure.getSimpleName + "]"
    }
  }

  /** Defines a read-only field for returning the readable item itself (as an Option). */
  def identityField[R <: AnyRef](implicit typeManifest: ClassManifest[R]) = new DelegatingPortableField[R] {
    val delegate = readOnly[R,R](readable => Some(readable))

    override def toString = "identifyField[" + typeManifest.erasure.getSimpleName + "]"
  }

  /** Defines write-only field for a Writable type with Option as the type. */
  def writeOnlyOpt[W,T](setter1: W => Option[T] => Unit)
                       (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] = {
    new FieldSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, valueOpt: Option[T]) {
        setter1(writable)(valueOpt)
      }

      def getter = emptyPartialFunction

      override def toString = "writeOnlyOpt[" + typeManifest.erasure.getSimpleName + "]"
    }
  }

  /**
   * Defines write-only field for a Writable type.
   * The setter operates on a value directly, rather than on an Option.
   * The clearer is used when the value is None.
   */
  def writeOnly[W,T](setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                    (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] =
    writeOnlyOpt[W,T](writable => { valueOpt =>
      valueOpt match {
        case Some(value) => setter1(writable)(value)
        case None => clearer(writable)
      }
    })

  /**
   * {@PortableField} support for transforming a subject using a value if <code>subject</code> is of type S.
   * @param S the Subject type to transform using the value
   */
  def transformOnly[S <: AnyRef,T](theTransform: S => T => S, clearer: S => S)(implicit typeManifest: ClassManifest[S]): PortableField[T] =
    new PortableField[T] with NoSetter[T] with NoGetter[T] {
      def transformer[S1] = {
        case subject: S if typeManifest.erasure.isInstance(subject) => _ match {
          case Some(value) => theTransform(subject)(value).asInstanceOf[S1]
          case None => clearer(subject).asInstanceOf[S1]
        }
      }
    }

  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PortableField[T] = new PortableField[T] with NoSetter[T] with NoTransformer[T] {
    def getter = { case Unit => Some(value) }

    override def toString = "default(" + value + ")"
  }

  /**
   * Defines PortableField for  a field value using a getter, setter and clearer.
   * The setter operates on a value directly, rather than on an Option.
   * The clearer is used when the value is None.
   * @param M any mutable type
   * @param T the value type
   */
  def field[M,T](getter: M => Option[T], setter: M => T => Unit, clearer: M => Unit = {_: M => })
                (implicit typeManifest: ClassManifest[M]): PortableField[T] =
    readOnly[M,T](getter) + writeOnly[M,T](setter, clearer)

  /**
   * Defines PortableField for a field value using a setter and getter, both operating on an Option.
   * @param M any mutable type
   * @param T the value type
   */
  def fieldOpt[M,T](getter: M => Option[T], setter: M => Option[T] => Unit)
                   (implicit typeManifest: ClassManifest[M]): PortableField[T] =
    readOnly[M,T](getter) + writeOnlyOpt[M,T](setter)

  def mapField[T](name: String): PortableField[T] = new DelegatingPortableField[T] {
    val delegate = readOnly[Map[String,_ <: T],T](_.get(name)) + writeOnly[mutable.Map[String,_ >: T],T](m => v => m.put(name, v), _.remove(name)) +
            transformOnly[immutable.Map[String,_ >: T],T](map => value => map + (name -> value), _ - name)

    override def toString = "mapField(" + name + ")"
  }

  def formatted[T](format: ValueFormat[T], field: PortableField[String]) = new PortableField[T] {
    def getter = field.getter.andThen(value => value.flatMap(format.toValue(_)))

    def setter = field.setter.andThen(setter => setter.compose(value => value.map(format.toString _)))

    def transformer[S <: AnyRef] = {
      case subject if field.transformer[S].isDefinedAt(subject) => { value =>
        field.transformer(subject)(value.map(format.toString _))
      }
    }

    override def toString = "formatted(" + format + ", " + field + ")"
  }

  /**
   * formatted replacement for primitive values.
   */
  def formatted[T <: AnyVal](field: PortableField[String])(implicit m: Manifest[T]): PortableField[T] =
    formatted(new BasicValueFormat[T](), field)
}
