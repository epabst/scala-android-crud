package geeks.crud

/**
 * A value format.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 */
trait ValueFormat[V] {
  /** May need to be overridden */
  def toString(value: V): String = value.toString

  def toValue(s: String): V
}

class BasicValueFormat[V](implicit m: Manifest[V]) extends ValueFormat[V] {
  /** May need to be overridden */
  def toValue(s: String): V = m match {
    case c: ClassManifest[V] => {
      c.erasure match {
        case x: Class[_] if (x == classOf[String]) => s.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Int]) => s.toInt.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Long]) => s.toLong.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Short]) => s.toShort.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Byte]) => s.toByte.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Double]) => s.toDouble.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Float]) => s.toFloat.asInstanceOf[V]
        case x: Class[_] if (x == classOf[Boolean]) => s.toBoolean.asInstanceOf[V]
      }
    }
  }
}
