package com.github.triangle

import com.github.scala_android.crud.common.Logging
import collection._

/** A trait for {@link PortableField} for convenience such as when defining a List of heterogeneous Fields. */
trait BaseField {
  /**
   * Copies this field, the same as {@link #copy(AnyRef,AnyRef)} except that
   * the copying from <code>from</code> happens immediately (on the current thread),
   * and the returned PortableValue can copy into the target on a separate thread, if desired..
   * It's a clean separation so that each step of the copy only accesses one of the objects.
   * @returns a PortableValue that copies the value into its parameter
   */
  def copyFrom(from: AnyRef): PortableValue

  /**
   * Copies this field from the first applicable item in <code>fromItems</code>.
   * @returns a PortableValue that copies the value into its parameter
   */
  def copyFromItem(fromItems: List[AnyRef]): PortableValue

  /**
   * Copies this field from <code>from</code> to <code>to</code>, if possible.
   */
  def copy(from: AnyRef, to: AnyRef) { copyFrom(from).copyTo(to) }

  /**
   * Copies this field from the first applicable item in <code>fromItems</code> to <code>to</code>, if possible.
   */
  def copyFromItem(fromItems: List[AnyRef], to: AnyRef) { copyFromItem(fromItems).copyTo(to) }

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
   * Traverses all of the PortableFields in this PortableField, returning the desired information.
   * Anything not matched will be traversed deeper, if possible, or else ignored.
   * <pre>
   *   deepCollect {
   *     case foo: BarField => foo.myInfo
   *   }
   * </pre>
   */
  // Default implementation only checks this field.  This should be overridden for any field wrapping other fields.
  def deepCollect[R](f: PartialFunction[BaseField, R]): List[R] = f.lift(this).toList

  protected def from_to_for_field_message(from: AnyRef, to: AnyRef, field: BaseField): String =
    " from " + truncate(from) + " to " + truncate(to) + " for field " + truncate(field)

  protected def transform_with_forField_message(initial: AnyRef, data: Any, field: BaseField): String =
    "transform " + truncate(initial) + " with " + truncate(data) + " for field " + truncate(field)

  private def truncate(any: Any): String = {
    val string = any.toString
    string.substring(0, math.min(string.length, 25))
  }
}

trait PortableValue {
  /**
   * Copies this value to <code>to</code>, if possible.
   */
  def copyTo(to: AnyRef)

  /**
   * Copies this value to <code>to</code> without seeing if the setter isDefinedAt that <code>to</code>.
   */
  protected[triangle] def copyToDefinedAt(to: AnyRef)

  /**
   * Transforms the <code>initial</code> subject using this value.
   * @returns the transformed subject, which could be the initial instance
   */
  def transform[S <: AnyRef](initial: S): S
}

/**
 * A portable field of a specific type which applies to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import com.github.triangle.PortableField._
 * import com.github.scala_android.crud.persistence.CursorField._
 * import com.github.scala_android.crud.persistence.PersistedType._
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
trait PortableField[T] extends BaseField with Logging { self =>
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
   * Gets the value, similar to {@link Map#apply}, and the value must not be None.
   * @see #getter
   * @returns the value
   * @throws NoSuchElementException if the value was None
   * @throws MatchError if subject is not an applicable type
   */
  def apply(subject: AnyRef): T = getter(subject).get

  /**
   * PartialFunction for setting an optional value in an AnyRef.
   */
  def setter: PartialFunction[AnyRef,Option[T] => Unit]

  /**
   * Sets a value in <code>subject</code> by using all embedded PortableFields that can handle it.
   * @return true if any were successful
   */
  def setValue(subject: AnyRef, value: Option[T]): Boolean = {
    val defined = setter.isDefinedAt(subject)
    if (defined) setter(subject)(value)
    else debug("Unable to set value " + value + " into " + subject + " for field " + this + ".")
    defined
  }

  /**
   * PartialFunction for transforming an AnyRef using an optional value.
   * This delegates to <code>setter</code> for mutable objects.
   * <code>transformer(foo)(value)<code> should return a transformed version of foo (which could be the same instance if mutable).
   * @param a subject to be transformed, whether immutable or mutable
   */
  def transformer[S <: AnyRef]: PartialFunction[S,Option[T] => S]

  private def transformUsingGetFunctionCheckingTransformerFirst[S <: AnyRef,F <: AnyRef](get: PartialFunction[F,Option[T]], initial: S, data: F) = {
    if (transformer.isDefinedAt(initial)) {
      copyFromUsingGetFunction(get, data).transform(initial)
    } else {
      debug("Unable to " + transform_with_forField_message(initial, data, this) + " because of transformer.")
      initial
    }
  }

  //inherited
  def transform[S <: AnyRef](initial: S, data: AnyRef) =
    transformUsingGetFunctionCheckingTransformerFirst[S,AnyRef](getter, initial, data)

  //inherited
  def transformWithItem[S <: AnyRef](initial: S, dataItems: List[AnyRef]) =
    transformUsingGetFunctionCheckingTransformerFirst[S,List[AnyRef]](getterFromItem, initial, dataItems)

  def copyFrom(from: AnyRef) = copyFromUsingGetFunction(getter, from)

  def copyFromItem(fromItems: List[AnyRef]) = copyFromUsingGetFunction(getterFromItem, fromItems)

  override def copy(from: AnyRef, to: AnyRef) {
    copyUsingGetFunctionCheckingSetterFirst(getter, from, to)
  }

  //inherited
  override def copyFromItem(fromItems: List[AnyRef], to: AnyRef) {
    copyUsingGetFunctionCheckingSetterFirst(getterFromItem, fromItems, to)
  }

  private def copyUsingGetFunctionCheckingSetterFirst[F <: AnyRef](get: PartialFunction[F,Option[T]], from: F, to: AnyRef) {
    if (setter.isDefinedAt(to)) {
      copyFromUsingGetFunction(get, from).copyTo(to)
    } else {
      debug("Unable to copy" + from_to_for_field_message(from, to, this)  + " due to setter.")
    }
  }

  private def copyFromUsingGetFunction[F <: AnyRef](get: PartialFunction[F,Option[T]], from: F): PortableValue = {
    if (get.isDefinedAt(from)) {
      val value = get(from)
      val field = this
      new PortableValue {
        def copyTo(to: AnyRef) {
          if (setter.isDefinedAt(to)) {
            copyToDefinedAt(to)
          }
        }

        protected[triangle] def copyToDefinedAt(to: AnyRef) {
          debug("Copying " + value + from_to_for_field_message(from, to, field))
          setter(to)(value)
        }

        def transform[S <: AnyRef](initial: S): S = {
          debug("About to " + transform_with_forField_message(initial, " value " + value, self))
          transformer(initial)(value)
        }

        override def toString = "PortableValue(" + value + ")"
      }
    } else {
      new PortableValue {
        def copyTo(to: AnyRef) {
          debug("Unable to copy" + from_to_for_field_message(from, to, self) + " due to getter.")
        }

        protected[triangle] def copyToDefinedAt(to: AnyRef) { new IllegalStateException("value not available") }

        def transform[S <: AnyRef](initial: S): S = {
          debug("Unable to " + transform_with_forField_message(initial, from, self) + " because of getter.")
          initial
        }

        override def toString = "PortableValue(Nothing from " + self + ")"
      }
    }
  }

  /**
   * Adds two PortableField objects together.
   */
  def +(other: PortableField[T]): PortableField[T] = {
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

        def apply(subject: AnyRef) = { value =>
          val definedFields = List(self, other).filter(_.setter.isDefinedAt(subject))
          if (definedFields.isEmpty) {
            throw new MatchError("setter in " + PortableField.this)
          } else {
            definedFields.foreach(_.setter(subject)(value))
          }
        }
      }

      def transformer[S <: AnyRef] = {
        case subject if self.transformer.isDefinedAt(subject) || other.transformer.isDefinedAt(subject) => { value =>
          val definedFields = List(self, other).filter(_.transformer.isDefinedAt(subject))
          definedFields.foldLeft(subject)((subject, field) => field.transformer(subject)(value))
        }
      }

      override def deepCollect[R](f: PartialFunction[BaseField, R]) = {
        super.deepCollect(f) match {
          case Nil =>
            val lifted = f.lift
            List(self, other).flatMap(field => lifted(field).map(List(_)).getOrElse(field.deepCollect(f)))
          case x => x
        }
      }
    }
  }
}

/**
 * Any PortableField that contains another PortableField should extend this.
 * @see DelegatingPortableField
 */
trait FieldWithDelegate[T] extends PortableField[T] {
  protected def delegate: BaseField

  override def deepCollect[R](f: PartialFunction[BaseField, R]) = f.lift(this).map(List(_)).getOrElse(delegate.deepCollect(f))
}

/**
 * A FieldWithDelegate that delegates directly to its delegate field.
 */
trait DelegatingPortableField[T] extends FieldWithDelegate[T] {
  protected def delegate: PortableField[T]

  def getter = delegate.getter

  def setter = delegate.setter

  def transformer[S <: AnyRef] = delegate.transformer
}

trait SubjectField { self: PortableField[_] =>
  def subjectManifest: ClassManifest[_]
}

/**
 * {@PortableField} support for getting a value as an Option if <code>subject</code> is of type S.
 * @param T the value type
 * @param S the Readable type to get the value out of
 */
abstract class FieldGetter[S,T](implicit val subjectManifest: ClassManifest[S]) extends PortableField[T] with SubjectField with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(subject: S): Option[T]

  def getter = { case subject: S if subjectManifest.erasure.isInstance(subject) => get(subject) }
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
 * {@PortableField} support for setting a value if <code>subject</code> is of type S.
 * This is a trait so that it can be mixed with FieldGetter.
 * @param S the Writable type to put the value into
 */
trait FieldSetter[S,T] extends PortableField[T] with SubjectField with TransformerUsingSetter[T] with Logging {
  def subjectManifest: ClassManifest[S]

  /** An abstract method that must be implemented by subtypes. */
  def set(subject: S, value: Option[T])

  def setter = {
    case subject: S if subjectManifest.erasure.isInstance(subject) => set(subject, _)
  }
}

trait NoTransformer[T] extends NoSetter[T] {
  def transformer[S <: AnyRef] = PortableField.emptyPartialFunction
}

case class FormattedField[T](format: ValueFormat[T], field: PortableField[String]) extends FieldWithDelegate[T] {
  protected def delegate = field

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
  def readOnly[S,T](getter1: S => Option[T])
                   (implicit subjectManifest: ClassManifest[S]): FieldGetter[S,T] = {
    new FieldGetter[S,T] with NoSetter[T] with NoTransformer[T] {
      def get(subject: S) = getter1(subject)

      override def toString = "readOnly[" + subjectManifest.erasure.getSimpleName + "]"
    }
  }

  /** Defines a read-only field for returning the subject item itself (as an Option). */
  def identityField[S <: AnyRef](implicit subjectManifest: ClassManifest[S]) = new DelegatingPortableField[S] {
    val delegate = readOnly[S,S](subject => Some(subject))

    override def toString = "identifyField[" + subjectManifest.erasure.getSimpleName + "]"
  }

  /** Defines write-only field for a Writable type with Option as the type. */
  def writeOnly[S,T](setter1: S => Option[T] => Unit)
                    (implicit _subjectManifest: ClassManifest[S]): FieldSetter[S,T] = {
    new FieldSetter[S,T] {
      def subjectManifest = _subjectManifest

      def set(subject: S, valueOpt: Option[T]) {
        setter1(subject)(valueOpt)
      }

      def getter = emptyPartialFunction

      override def toString = "writeOnly[" + subjectManifest.erasure.getSimpleName + "]"
    }
  }

  private def fromDirect[S,T](setter: S => T => Unit, clearer: S => Unit = {_: S => })
                             (implicit subjectManifest: ClassManifest[S]): S => Option[T] => Unit = {
    subject => { valueOpt =>
      valueOpt match {
        case Some(value) => setter(subject)(value)
        case None => clearer(subject)
      }
    }
  }

  /**
   * Defines write-only field for a Writable type.
   * The setter operates on a value directly, rather than on an Option.
   * The clearer is used when the value is None.
   */
  def writeOnlyDirect[S,T](setter1: S => T => Unit, clearer: S => Unit = {_: S => })
                          (implicit subjectManifest: ClassManifest[S]): FieldSetter[S,T] =
    writeOnly[S,T](fromDirect(setter1, clearer))

  /**
   * {@PortableField} support for transforming a subject using a value if <code>subject</code> is of type S.
   * @param S the Subject type to transform using the value
   */
  def transformOnly[S <: AnyRef,T](theTransform: S => Option[T] => S)(implicit _subjectManifest: ClassManifest[S]): PortableField[T] =
    new PortableField[T] with SubjectField with NoSetter[T] with NoGetter[T] {
      def transformer[S1] = {
        case subject: S if subjectManifest.erasure.isInstance(subject) => value =>
          theTransform(subject)(value).asInstanceOf[S1]
      }

      def subjectManifest = _subjectManifest

      override def toString = "transformOnly[" + subjectManifest.erasure.getSimpleName + "]"
    }

  /**
   * {@PortableField} support for transforming a subject using a value if <code>subject</code> is of type S.
   * theTransform operates on a value directly, rather than on an Option.
   * The clearer is used when the value is None.
   * @param S the Subject type to transform using the value
   */
  def transformOnlyDirect[S <: AnyRef,T](theTransform: S => T => S, clearer: S => S)(implicit subjectManifest: ClassManifest[S]): PortableField[T] =
    transformOnly[S,T](subject => { valueOpt =>
      valueOpt match {
        case Some(value) => theTransform(subject)(value).asInstanceOf[S]
        case None => clearer(subject).asInstanceOf[S]
      }
    })

  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PortableField[T] = new PortableField[T] with NoSetter[T] with NoTransformer[T] {
    def getter = { case Unit => Some(value) }

    override def toString = "default(" + value + ")"
  }

  /**
   * Defines PortableField for  a field value using a getter, setter and clearer.
   * The setter operates on a value directly, rather than on an Option.
   * The clearer is used when the value is None.
   * @param S the Subject being accessed
   * @param T the value type
   */
  def fieldDirect[S,T](getter: S => Option[T], setter: S => T => Unit, clearer: S => Unit = {_: S => })
                      (implicit subjectManifest: ClassManifest[S]): PortableField[T] =
    field[S,T](getter, fromDirect(setter, clearer))

  /**
   * Defines PortableField for a field value using a setter and getter, both operating on an Option.
   * @param S the subject being accessed
   * @param T the value type
   */
  def field[S,T](getter1: S => Option[T], setter1: S => Option[T] => Unit)
                (implicit _subjectManifest: ClassManifest[S]): PortableField[T] = new DelegatingPortableField[T] with SubjectField {
    val delegate = readOnly[S,T](getter1) + writeOnly[S,T](setter1)

    def subjectManifest = _subjectManifest

    override def toString = "field[" + subjectManifest.erasure.getSimpleName + "]"
  }

  def mapField[T](name: String): PortableField[T] = new DelegatingPortableField[T] {
    val delegate = readOnly[Map[String,_ <: T],T](_.get(name)) + writeOnlyDirect[mutable.Map[String,_ >: T],T](m => v => m.put(name, v), _.remove(name)) +
            transformOnlyDirect[immutable.Map[String,_ >: T],T](map => value => map + (name -> value), _ - name)

    override def toString = "mapField(" + name + ")"
  }

  def formatted[T](format: ValueFormat[T], field: PortableField[String]) = new FormattedField(format, field)

  /**
   * formatted replacement for primitive values.
   */
  def formatted[T <: AnyVal](field: PortableField[String])(implicit m: Manifest[T]): PortableField[T] =
    formatted(ValueFormat.basicFormat[T], field)
}
