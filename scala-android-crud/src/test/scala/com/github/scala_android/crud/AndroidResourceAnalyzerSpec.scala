package com.github.scala_android.crud

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import testres._

/**
 * A behavior specification for {@link AndroidResourceAnalyzer}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/13/11
 * Time: 9:55 PM
 */
@RunWith(classOf[JUnitRunner])
class AndroidResourceAnalyzerSpec extends Spec with MustMatchers {
  describe("detectResourceIdClasses") {
    it("must be able to find all of the R.id instances") {
      AndroidResourceAnalyzer.detectResourceIdClasses(classOf[SiblingToR]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id]))
    }

    it("must look in parent packages to find the application R.id instance") {
      AndroidResourceAnalyzer.detectResourceIdClasses(classOf[subpackage.ClassInSubpackage]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id]))
    }
  }

  it("must locate a resource field by name") {
    AndroidResourceAnalyzer.findResourceFieldWithName(List(classOf[R.id]), "foo").map(_.get(null)) must be (Some(123))
  }

  it("must locate a resource field by value") {
    AndroidResourceAnalyzer.findResourceFieldWithIntValue(List(classOf[R.id]), 123).map(_.getName) must be (Some("foo"))
  }
}
