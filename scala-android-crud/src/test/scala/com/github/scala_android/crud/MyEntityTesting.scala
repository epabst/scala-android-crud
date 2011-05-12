package com.github.scala_android.crud

import org.scalatest.mock.EasyMockSugar
import scala.collection.mutable
import mutable.Map
import com.github.triangle._
import PortableField._
import ViewField._
import CursorField._
import android.content.Intent
import org.easymock.IAnswer
import org.easymock.classextension.EasyMock
import android.widget.{ListAdapter, TextView}
import res.R
import android.app.Activity

/**
 * An object mother pattern for getting CrudEntityType instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

trait MyEntityTesting extends EasyMockSugar {
  class MyEntityPersistence extends ListBufferEntityPersistence[Map[String,Any]] {
    def entityType = throw new UnsupportedOperationException

    def newCriteria = "TheCriteria"
  }

  class MyEntityType(persistence: CrudEntityPersistence, listAdapter: ListAdapter, val entityName: String = "MyMap")
          extends StubEntityType {
    var refreshCount = 0

    def fields: List[BaseField] = List(
      persisted[String]("name") + viewId(R.id.name, textView),
      persisted[Int]("age") + viewId(R.id.age, formatted[Int](textView)),
      //here to test a non-UI field
      persisted[String]("uri") + readOnly[Intent,String](_.getData.toString))

    def newWritable = Map[String,Any]()

    def openEntityPersistence(crudContext: CrudContext) = persistence

    def createListAdapter(persistence: CrudEntityPersistence, crudContext: CrudContext, activity: Activity) = listAdapter

    def refreshAfterSave(crudContext: CrudContext) {
      refreshCount += 1
    }

    val cancelItemString = R.string.cancel_item
  }

  trait StubEntityType extends CrudEntityType {
    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val displayLayout: Option[LayoutKey] = None
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item

    def listActivityClass = classOf[CrudListActivity]
    def activityClass = classOf[CrudActivity]
  }

  def namedMock[T <: AnyRef](name: String)(implicit manifest: Manifest[T]): T = {
    EasyMock.createMock(name, manifest.erasure.asInstanceOf[Class[T]])
  }

  def answer[T](result: => T) = new IAnswer[T] {
    def answer = result
  }

  def eql[T](value: T): T = org.easymock.EasyMock.eq(value)
}