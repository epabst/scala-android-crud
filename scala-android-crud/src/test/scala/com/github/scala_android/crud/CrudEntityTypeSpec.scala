package com.github.scala_android.crud

import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.scala_android.crud.CursorFieldAccess._
import com.github.triangle.Field

/**
 * A behavior specification for {@link CrudEntityType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudEntityTypeSpec extends Spec with ShouldMatchers with MyEntityTesting {

  it("should derive parent entities from foreignKey fields") {
    val persistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val entityType1 = new MyEntityType(persistence)
    val entityType2 = new MyEntityType(persistence)
    val entityType3 = new MyEntityType(persistence) {
      override val fields = Field(foreignKey(entityType1)) :: Field(foreignKey(entityType2)) :: super.fields
    }
    entityType3.parentEntities should be (List(entityType1, entityType2))
  }
}