package com.github.scala.android.crud

import action.ContextVars
import common.UriPath
import org.junit.runner.RunWith
import persistence.EntityType
import org.scalatest.matchers.MustMatchers
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.mockito.Mockito._
import org.mockito.Matchers._
import com.github.triangle.PortableField._
import common.PlatformTypes._
import android.app.Activity
import android.view.LayoutInflater
import android.widget.{BaseAdapter, AdapterView, ListAdapter}

/** A behavior specification for [[com.github.scala.android.crud.GeneratedPersistenceFactory]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[RobolectricTestRunner])
class GeneratedCrudTypeSpec extends MustMatchers with CrudMockitoSugar {
  val seqPersistence = mock[SeqCrudPersistence[Map[String,Any]]]
  val adapterView = mock[AdapterView[BaseAdapter]]
  val activity = mock[Activity]
  val listAdapterCapture = capturingAnswer[Unit] { Unit }
  val generatedEntityName = "Generated"
  val crudContext = mock[CrudContext]
  val layoutInflater = mock[LayoutInflater]

  @Test
  def itsListAdapterMustGetTheItemIdUsingTheIdField() {
    val factory = new GeneratedPersistenceFactory[Map[String,Any]] {
      def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = seqPersistence
    }
    val entityType = new EntityType {
      override protected def idField = mapField[ID]("longId") + super.idField
      def entityName = generatedEntityName
      def valueFields = Nil
    }
    stub(activity.getLayoutInflater).toReturn(layoutInflater)
    val generatedCrudType = new CrudType(entityType, factory) with StubCrudType
    stub(crudContext.vars).toReturn(new ContextVars {})
    when(adapterView.setAdapter(anyObject())).thenAnswer(listAdapterCapture)
    val persistence = mock[CrudPersistence]
    when(crudContext.openEntityPersistence(entityType)).thenReturn(persistence)
    val uri = UriPath.EMPTY
    when(persistence.findAll(uri)).thenReturn(List(Map("longId" -> 456L)))
    generatedCrudType.setListAdapter(adapterView, entityType, uri, crudContext, Nil, activity, 123)
    verify(adapterView).setAdapter(anyObject())
    val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    listAdapter.getItemId(0) must be (456L)
  }
}
