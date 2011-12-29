package com.github.scala.android.crud

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec

/** A behavior specification for [[com.github.scala.android.crud.CrudApplication]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class CrudApplicationSpec extends Spec with MustMatchers {

  it("must provide a valid nameId") {
    val application = new CrudApplication {
      def name = "A diFFicult name to use as an ID"
      def allCrudTypes = List()
      def dataVersion = 1
    }
    application.nameId must be ("a_difficult_name_to_use_as_an_id")
  }
}