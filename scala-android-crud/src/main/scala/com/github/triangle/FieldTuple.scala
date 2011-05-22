package com.github.triangle

/**
 * A Tuple of Fields.
 * Use the right subtype for the correct -arity such as FieldTuple3.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 5/20/11
 * Time: 9:50 AM
 */
trait FieldTuple extends Product {
  /** a type which is a Tuple for the field values such as (Option[A], Option[B], Option[C]). */
  type ValuesTuple <: Product

  /**
   * Gets a Tuple with the results of calling each getter with <code>readable</code> as the parameter.
   */
  def valuesTuple(readable: AnyRef): ValuesTuple

  /**
   * Gets a Tuple with the results of calling each getterFromItem with <code>items</code> as the parameter.
   */
  def valuesTupleFromItem(items: List[AnyRef]): ValuesTuple

  /**
   * An extractor for the values of the fields used in the calculation of the calculated field value.
   * It handles both List[AnyRef] and AnyRef itself.
   */
  object Values {
    def unapply(readable: AnyRef): Option[ValuesTuple] = readable match {
      case x if productIterator.forall(_.asInstanceOf[PortableField[_]].getter.isDefinedAt(x)) => Some(valuesTuple(x))
      case x: List[AnyRef] if productIterator.forall(_.asInstanceOf[PortableField[_]].getterFromItem.isDefinedAt(x)) =>
        Some(valuesTupleFromItem(x))
      case _ => None
    }
  }

  def canEqual(that: Any) = that match {
    case x: AnyRef => this.getClass == x.getClass
    case _ => false
  }
}

class FieldTuple1[F1](val _1: PortableField[F1])
        extends FieldTuple with Product1[PortableField[F1]] {
  type ValuesTuple = (Option[F1])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items))
}

class FieldTuple2[F1,F2](val _1: PortableField[F1], val _2: PortableField[F2])
        extends FieldTuple with Product2[PortableField[F1],PortableField[F2]] {
  type ValuesTuple = (Option[F1], Option[F2])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items))
}

class FieldTuple3[F1,F2,F3](val _1: PortableField[F1], val _2: PortableField[F2], val _3: PortableField[F3])
        extends FieldTuple with Product3[PortableField[F1],PortableField[F2],PortableField[F3]] {
  type ValuesTuple = (Option[F1], Option[F2], Option[F3])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable), _3.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items), _3.getterFromItem(items))
}

class FieldTuple4[F1,F2,F3,F4](val _1: PortableField[F1], val _2: PortableField[F2], val _3: PortableField[F3],
                               val _4: PortableField[F4])
        extends FieldTuple with Product4[PortableField[F1],PortableField[F2],PortableField[F3],PortableField[F4]] {
  type ValuesTuple = (Option[F1], Option[F2], Option[F3], Option[F4])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable), _3.getter(readable), _4.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items), _3.getterFromItem(items),
          _4.getterFromItem(items))
}

class FieldTuple5[F1,F2,F3,F4,F5](val _1: PortableField[F1], val _2: PortableField[F2], val _3: PortableField[F3],
                                  val _4: PortableField[F4], val _5: PortableField[F5])
        extends FieldTuple with Product5[PortableField[F1],PortableField[F2],PortableField[F3],PortableField[F4],PortableField[F5]] {
  type ValuesTuple = (Option[F1], Option[F2], Option[F3], Option[F4], Option[F5])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable), _3.getter(readable),
          _4.getter(readable), _5.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items), _3.getterFromItem(items),
          _4.getterFromItem(items), _5.getterFromItem(items))
}

class FieldTuple6[F1,F2,F3,F4,F5,F6](val _1: PortableField[F1], val _2: PortableField[F2], val _3: PortableField[F3],
                                     val _4: PortableField[F4], val _5: PortableField[F5], val _6: PortableField[F6])
        extends FieldTuple with Product6[PortableField[F1],PortableField[F2],PortableField[F3],PortableField[F4],PortableField[F5],PortableField[F6]] {
  type ValuesTuple = (Option[F1], Option[F2], Option[F3], Option[F4], Option[F5], Option[F6])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable), _3.getter(readable),
          _4.getter(readable), _5.getter(readable), _6.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items), _3.getterFromItem(items),
          _4.getterFromItem(items), _5.getterFromItem(items), _6.getterFromItem(items))
}

class FieldTuple7[F1,F2,F3,F4,F5,F6,F7](val _1: PortableField[F1], val _2: PortableField[F2], val _3: PortableField[F3],
                                        val _4: PortableField[F4], val _5: PortableField[F5], val _6: PortableField[F6],
                                        val _7: PortableField[F7])
        extends FieldTuple with Product7[PortableField[F1],PortableField[F2],PortableField[F3],PortableField[F4],PortableField[F5],PortableField[F6],PortableField[F7]] {
  type ValuesTuple = (Option[F1], Option[F2], Option[F3], Option[F4], Option[F5], Option[F6], Option[F7])
  def valuesTuple(readable: AnyRef) = (_1.getter(readable), _2.getter(readable), _3.getter(readable),
          _4.getter(readable), _5.getter(readable), _6.getter(readable), _7.getter(readable))
  def valuesTupleFromItem(items: List[AnyRef]) = (_1.getterFromItem(items), _2.getterFromItem(items), _3.getterFromItem(items),
          _4.getterFromItem(items), _5.getterFromItem(items), _6.getterFromItem(items), _7.getterFromItem(items))
}
