package com.github.scala.android.crud.view

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.testres._

/**
 * A behavior specification for {@link AndroidResourceAnalyzer}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/13/11
 * Time: 9:55 PM
 */
@RunWith(classOf[JUnitRunner])
class AndroidResourceAnalyzerSpec extends Spec with MustMatchers {
  describe("detectRIdClasses") {
    it("must be able to find all of the R.id instances") {
      AndroidResourceAnalyzer.detectRIdClasses(classOf[SiblingToR]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala.android.crud.res.R.id]))
    }

    it("must look in parent packages to find the application R.id instance") {
      AndroidResourceAnalyzer.detectRIdClasses(classOf[subpackage.ClassInSubpackage]) must
              be (Seq(classOf[R.id], classOf[android.R.id], classOf[com.github.scala.android.crud.res.R.id]))
    }
  }

  describe("detectRLayoutClasses") {
    it("must be able to find all of the R.layout instances") {
      AndroidResourceAnalyzer.detectRLayoutClasses(classOf[SiblingToR]) must
              be (Seq(classOf[R.layout], classOf[android.R.layout], classOf[com.github.scala.android.crud.res.R.layout]))
    }

    it("must look in parent packages to find the application R.layout instance") {
      AndroidResourceAnalyzer.detectRLayoutClasses(classOf[subpackage.ClassInSubpackage]) must
              be (Seq(classOf[R.layout], classOf[android.R.layout], classOf[com.github.scala.android.crud.res.R.layout]))
    }
  }

  it("must locate a resource field by name") {
    AndroidResourceAnalyzer.findResourceFieldWithName(List(classOf[R.id]), "foo").map(_.get(null)) must be (Some(123))
  }

  it("must locate a resource field by value") {
    AndroidResourceAnalyzer.findResourceFieldWithIntValue(List(classOf[R.id]), 123).map(_.getName) must be (Some("foo"))
  }
}
