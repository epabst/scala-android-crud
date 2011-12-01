package com.github.scala.android.crud

import action.UriPath
import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.matchers.MustMatchers
import android.widget.ListAdapter
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.hamcrest.CoreMatchers._
import org.mockito.Mockito._
import ParentField.foreignKey
import org.mockito.Matchers
import com.github.triangle.PortableField._
import common.PlatformTypes._

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/12/11
 * Time: 7:59 PM
 */

@RunWith(classOf[RobolectricTestRunner])
class GeneratedCrudTypeSpec extends MustMatchers with MyEntityTesting with CrudMockitoSugar {
  val seqPersistence = mock[SeqCrudPersistence[mutable.Map[String,Any]]]
  val crudContext = mock[CrudContext]
  val listActivity = mock[CrudListActivity]
  val listAdapterCapture = capturingAnswer[Unit] { Unit }
  val otherType = new MyEntityType(seqPersistence, mock[ListAdapter])
  val foreign = foreignKey(otherType)
  val generatedEntityName = "Generated"
  val listUri = UriPath(otherType.entityName, "123", generatedEntityName)
  stub(listActivity.currentUriPath).toReturn(listUri)

  @Test
  def itMustCreateListAdapterWithUriPathUsedForCriteria() {
    val generatedType = new GeneratedCrudType[mutable.Map[String,Any]] with StubEntityType {
      def entityName = generatedEntityName
      def valueFields = List(foreign)
      protected def createEntityPersistence(crudContext: CrudContext) = seqPersistence
    }
    stub(seqPersistence.entityType).toReturn(generatedType)

    when(seqPersistence.findAll(listUri)).thenReturn(List.empty)
    when(listActivity.setListAdapter(Matchers.argThat(notNullValue()))).thenAnswer(listAdapterCapture)

    generatedType.setListAdapterUsingUri(seqPersistence, crudContext, listActivity)
    val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    listAdapter.getCount must be (0)
  }

  @Test
  def itsListAdapterMustGetTheItemIdUsingTheIdField() {
    val generatedType = new GeneratedCrudType[mutable.Map[String,Any]] with StubEntityType {
      def entityName = generatedEntityName
      override protected def idField = mapField[ID]("longId") + super.idField
      def valueFields = Nil
      protected def createEntityPersistence(crudContext: CrudContext) = seqPersistence
    }
    stub(seqPersistence.entityType).toReturn(generatedType)

    when(seqPersistence.findAll(listUri)).thenReturn(List(mutable.Map("longId" -> 456L)))
    when(listActivity.setListAdapter(Matchers.argThat(notNullValue()))).thenAnswer(listAdapterCapture)

    generatedType.setListAdapterUsingUri(seqPersistence, crudContext, listActivity)
    val listAdapter = listAdapterCapture.params(0).asInstanceOf[ListAdapter]
    listAdapter.getItemId(0) must be (456L)
  }
}