package com.github.scala_android.crud

import org.junit.runner.RunWith
import persistence.SeqEntityPersistence
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import android.widget.ListAdapter
import android.content.Intent
import com.github.scala_android.crud.action.Action.toUri
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import android.app.ListActivity
import org.easymock.EasyMock._
import ParentField.foreignKey

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/12/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class GeneratedCrudTypeSpec extends Spec with MustMatchers with MyEntityTesting with CrudEasyMockSugar {

  @Test
  def itMustCreateListAdapterWithIntentUsedForCriteria() {
    val seqPersistence = mock[SeqEntityPersistence[mutable.Map[String,Any]]]
    val crudContext = mock[CrudContext]
    val activity = mock[ListActivity]
    val listAdapterCapture = capturingAnswer[Unit] { Unit }
    val otherType = new MyEntityType(seqPersistence, mock[ListAdapter])
    val foreign = foreignKey(otherType)
    val generatedType = new GeneratedCrudType[mutable.Map[String,Any]] with StubEntityType {
      def entityName = "Generated"
      def valueFields = List(foreign)
      def openEntityPersistence(crudContext: CrudContext) = seqPersistence
    }
    expecting {
      call(activity.getIntent).andReturn(new Intent("List", toUri(otherType.entityName, "123")))
      call(seqPersistence.newCriteria).andReturn(mutable.Map[String,Any]())
      call(seqPersistence.findAll(mutable.Map[String,Any](ParentField(otherType).fieldName -> 123L))).andReturn(List.empty)
      call(activity.setListAdapter(notNull())).andAnswer(listAdapterCapture)
      call(seqPersistence.entityType).andStubReturn(generatedType)
    }
    whenExecuting(seqPersistence, crudContext, activity) {
      generatedType.setListAdapter(seqPersistence, crudContext, activity)
      val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
      listAdapter.getCount must be (0)
    }
  }
}