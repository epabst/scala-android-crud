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

  /** extractor for finding the applicable item. */
  private object ApplicableItem {
    def unapply(items: List[AnyRef]): Option[AnyRef] = {
      items.find(getter.isDefinedAt(_))
    }
  }

  /**
   * PartialFunction for getting an optional value from the first AnyRef in the List that is applicable.
   */
  def getterFromItem: PartialFunction[List[AnyRef],Option[T]] = {
    case ApplicableItem(item) => getter(item)
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

  //inherited
  def transform[S <: AnyRef](initial: S, data: AnyRef) = {
    if (getter.isDefinedAt(data) && transformer.isDefinedAt(initial)) {
      val value = getter(data)
      debug("Transforming " + initial + " with value " + value + " for field " + this)
      transformer(initial)(value)
    } else {
      debug("Unable to transform " + initial + " with " + data + " for field " + this + ".")
      initial
    }
  }

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

      lazy val getter = self.getter.orElse(other.getter)

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
 * {@PortableField} support for getting and setting a value if <code>readable</code> and <code>writable</code>
 * are of the types R and W respectively.
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class FlowField[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends FieldGetter[R,T] with FieldSetter[W,T] with TransformerUsingSetter[T] {
  protected def writableManifest = _writableManifest
}

/**
 * A PortableField that is generated using the data in other fields.
 * @param fieldsUsed the fields whose getters must be applicable (i.e. isDefinedAt) in order for this GeneratedField to be applicable.
 */
abstract class GeneratedField[T](fieldsUsed: BaseField*) extends PortableField[T] with NoSetter[T] {
  /**
   * Generate the value.  The <code>fieldsUsed</code> can be used like this within this method:
   * <pre>
   *   val myOptionalFoo = fooField.getterFromItem(fromItems)
   *   val myRequiredBar = barField.getterFromItem(fromItems).get
   *   ...do the calculation...
   * </pre>
   */
  def generate(fromItems: List[AnyRef]): Option[T]

  /** Delegates to getterFromItem */
  def getter = {
    case from if getterFromItem.isDefinedAt(List(from)) => getterFromItem(List(from))
  }

  override def getterFromItem = new PartialFunction[List[AnyRef],Option[T]] {
    def isDefinedAt(fromItems: List[AnyRef]) =
      fieldsUsed.view.forall(_.asInstanceOf[PortableField[_]].getterFromItem.isDefinedAt(fromItems))

    def apply(fromItems: List[AnyRef]) = generate(fromItems)
  }
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
    }
  }

  /** Defines a read-only field for returning the readable item itself (as an Option). */
  def identityField[R <: AnyRef](implicit typeManifest: ClassManifest[R]) = readOnly[R,R](readable => Some(readable))

  /** Defines write-only field for a Writable type. */
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
   * Defines a flow for a field value from a Readable type to a Writable type.
   * The value never actually is taken directly from the Readable and set in the Writable.
   * It is copied to and from other objects.
   * @param R the Readable type to get the value out of
   * @param W the Writable type to put the value into
   * @param T the value type
   */
  def flow[R,W,T](getter1: R => Option[T], setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): FlowField[R,W,T] = {
    new FlowField[R,W,T] {
      def get(readable: R) = getter1(readable)

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter1(writable)(value)
          case None => clearer(writable)
        }
      }

      override def toString = {
        if (readableManifest == writableManifest) {
          "field[" + readableManifest.erasure.getSimpleName + "]"
        } else {
          "flow[" + readableManifest.erasure.getSimpleName + "," + writableManifest.erasure.getSimpleName + "]"
        }
      }
    }
  }

  /**
   *  Defines PortableField for a field value using a setter and getter.
   * @param M any mutable type
   * @param T the value type
   */
  def field[M,T](getter: M => Option[T], setter: M => T => Unit, clearer: M => Unit = {_: M => })
                 (implicit typeManifest: ClassManifest[M]): FlowField[M,M,T] = flow[M,M,T](getter, setter, clearer)

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
  }

  /**
   * formatted replacement for primitive values.
   */
  def formatted[T <: AnyVal](field: PortableField[String])(implicit m: Manifest[T]): PortableField[T] =
    formatted(new BasicValueFormat[T](), field)
}
