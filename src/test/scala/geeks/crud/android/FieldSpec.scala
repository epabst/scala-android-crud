package geeks.crud.android

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec

/**
 * A behavior specification for {@link SimpleField}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class FieldSpec extends Spec with ShouldMatchers {
  describe("SimpleField") {
    it("should use the ValueFormat to convert") {
      val value = 3232.11
      val map = scala.collection.mutable.Map[String, Double]()
      val field = new SimpleField[scala.collection.mutable.Map[String,Double],Double]("value", 0, _("value"), e => {v => e.put("value", v)})
      val string = field.format.toString(value)
      field.format.toValue(string).get should be (value)
      field.setValueFromView(map, field.format.toValue(string).get)
      map("value") should be (value)
    }
  }
}