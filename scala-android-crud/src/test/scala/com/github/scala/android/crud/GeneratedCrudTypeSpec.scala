package com.github.scala.android.crud

import action.UriPath
import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import android.widget.ListAdapter
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
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
  def itMustCreateListAdapterWithUriPathUsedForCriteria() {
    val seqPersistence = mock[SeqCrudPersistence[mutable.Map[String,Any]]]
    val crudContext = mock[CrudContext]
    val activity = mock[CrudListActivity]
    val listAdapterCapture = capturingAnswer[Unit] { Unit }
    val otherType = new MyEntityType(seqPersistence, mock[ListAdapter])
    val foreign = foreignKey(otherType)
    val generatedType = new GeneratedCrudType[mutable.Map[String,Any]] with StubEntityType {
      def entityName = "Generated"
      def valueFields = List(foreign)
      protected def createEntityPersistence(crudContext: CrudContext) = seqPersistence
    }
    expecting {
      val uri = UriPath(otherType.entityName, "123")
      call(activity.currentUriPath).andStubReturn(uri)
      call(seqPersistence.findAll(uri)).andReturn(List.empty)
      call(activity.setListAdapter(notNull())).andAnswer(listAdapterCapture)
      call(seqPersistence.entityType).andStubReturn(generatedType)
    }
    whenExecuting(seqPersistence, crudContext, activity) {
      generatedType.setListAdapterUsingUri(seqPersistence, crudContext, activity)
      val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
      listAdapter.getCount must be (0)
    }
  }
}