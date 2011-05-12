package com.github.scala_android.crud

import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.scala_android.crud.CursorField._
import com.github.triangle.PortableField
import android.widget.ListAdapter
import android.content.Intent
import ActivityUIActionFactory._
import android.app.Activity
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test

/**
 * A behavior specification for {@link CrudEntityType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/12/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class GeneratedCrudTypeSpec extends Spec with ShouldMatchers with MyEntityTesting {

  @Test
  def itShouldCreateListAdapterWithIntentUsedForCriteria() {
    val listPersistence = mock[ListEntityPersistence[mutable.Map[String,Any]]]
    val crudContext = mock[CrudContext]
    val activity = mock[Activity]
    val otherType = new MyEntityType(listPersistence, mock[ListAdapter])
    val foreign = foreignKey(otherType)
    expecting {
      call(activity.getIntent).andReturn(new Intent("List", toUri(otherType.entityName, "123")))
      call(listPersistence.newCriteria).andReturn(mutable.Map[String,Any]())
      call(listPersistence.findAll(mutable.Map[String,Any](foreign.fieldName -> 123L))).andReturn(List.empty)
    }
    whenExecuting(listPersistence, crudContext, activity) {
      val generatedType = new GeneratedCrudType[mutable.Map[String,Any]] with StubEntityType {
        def entityName = "Generated"
        def fields = List(foreign)
        def openEntityPersistence(crudContext: CrudContext) = listPersistence
      }
      val listAdapter = generatedType.createListAdapter(listPersistence, crudContext, activity)
      listAdapter.getCount should be (0)
    }
  }
}