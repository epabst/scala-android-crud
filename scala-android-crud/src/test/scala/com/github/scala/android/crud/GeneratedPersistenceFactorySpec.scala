package com.github.scala.android.crud

import org.junit.runner.RunWith
import persistence.EntityType
import org.scalatest.matchers.MustMatchers
import android.widget.ListAdapter
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.mockito.Mockito._
import org.mockito.Matchers._
import com.github.triangle.PortableField._
import common.PlatformTypes._

/**
 * A behavior specification for {@link GeneratedPersistenceFactory}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/12/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class GeneratedPersistenceFactorySpec extends MustMatchers with CrudMockitoSugar {
  val seqPersistence = mock[SeqCrudPersistence[Map[String,Any]]]
  val listActivity = mock[CrudListActivity]
  val listAdapterCapture = capturingAnswer[Unit] { Unit }
  val generatedEntityName = "Generated"

  @Test
  def itsListAdapterMustGetTheItemIdUsingTheIdField() {
    val factory = new GeneratedPersistenceFactory[Map[String,Any]] {
      def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = seqPersistence
    }
    val generatedCrudType = new GeneratedCrudType[Map[String, Any]](factory) with StubEntityType {
      override protected def idField = mapField[ID]("longId") + super.idField
      def entityName = generatedEntityName
      def valueFields = Nil
    }
    when(listActivity.setListAdapter(anyObject())).thenAnswer(listAdapterCapture)
    factory.setListAdapter(generatedCrudType, List(Map("longId" -> 456L)), Nil, listActivity)
    val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    listAdapter.getItemId(0) must be (456L)
  }
}
