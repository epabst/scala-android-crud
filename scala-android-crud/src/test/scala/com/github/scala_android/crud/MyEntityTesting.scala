package com.github.scala_android.crud

import scala.collection.mutable
import mutable.Map
import com.github.triangle._
import PortableField._
import view.ViewField._
import persistence.CursorField._
import android.content.Intent
import android.widget.ListAdapter
import res.R
import android.app.ListActivity

/**
 * An object mother pattern for getting CrudType instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

trait MyEntityTesting {
  class MyEntityPersistence extends ListBufferCrudPersistence[Map[String,Any]] {
    def entityType = throw new UnsupportedOperationException
  }

  class MyEntityType(persistence: CrudPersistence, listAdapter: ListAdapter, val entityName: String = "MyMap")
          extends StubEntityType {
    var refreshCount = 0

    def valueFields = List[BaseField](
      persisted[String]("name") + viewId(R.id.name, textView),
      persisted[Int]("age") + viewId(R.id.age, intView),
      //here to test a non-UI field
      persisted[String]("uri") + readOnly[Intent,String](_.getData.toString))

    def newWritable = Map[String,Any]()

    def openEntityPersistence(crudContext: CrudContext) = persistence

    def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity) {
      activity.setListAdapter(listAdapter)
    }

    def refreshAfterDataChanged(listAdapter: ListAdapter) {
      refreshCount += 1
    }

    val cancelItemString = R.string.cancel_item
  }
}