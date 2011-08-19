package com.github.scala_android.crud

import org.scalatest.mock.EasyMockSugar
import persistence.{CrudPersistence, ListBufferEntityPersistence}
import scala.collection.mutable
import mutable.Map
import com.github.triangle._
import PortableField._
import ViewField._
import persistence.CursorField._
import android.content.Intent
import org.easymock.EasyMock
import android.widget.ListAdapter
import res.R
import android.app.ListActivity
import org.easymock.IAnswer
import scala.collection.JavaConversions._

/**
 * An object mother pattern for getting CrudType instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

trait MyEntityTesting {
  class MyEntityPersistence extends ListBufferEntityPersistence[Map[String,Any]] {
    def entityType = throw new UnsupportedOperationException
  }

  class MyEntityType(persistence: CrudPersistence, listAdapter: ListAdapter, val entityName: String = "MyMap")
          extends StubEntityType {
    var refreshCount = 0

    def valueFields = List[BaseField](
      persisted[String]("name") + viewId(R.id.name, textView),
      persisted[Int]("age") + viewId(R.id.age, formatted[Int](textView)),
      //here to test a non-UI field
      persisted[String]("uri") + readOnly[Intent,String](_.getData.toString))

    def newWritable = Map[String,Any]()

    def openEntityPersistence(crudContext: CrudContext) = persistence

    def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
      activity.setListAdapter(listAdapter)
    }

    def refreshAfterDataChanged(listAdapter: ListAdapter) {
      refreshCount += 1
    }

    val cancelItemString = R.string.cancel_item
  }
}