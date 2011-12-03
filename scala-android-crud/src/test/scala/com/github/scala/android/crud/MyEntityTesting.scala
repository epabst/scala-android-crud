package com.github.scala.android.crud

import common.UriPath
import common.Common
import scala.collection.mutable
import mutable.Map
import com.github.triangle._
import view.ViewField._
import persistence.CursorField._
import android.widget.ListAdapter
import res.R

/**
 * An object mother pattern for getting CrudType instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

trait MyEntityTesting extends Logging {
  protected def logTag = Common.logTag

  class MyEntityPersistence extends ListBufferCrudPersistence[Map[String,Any]](null, null)

  class MyEntityType(persistence: CrudPersistence, listAdapter: ListAdapter, val entityName: String = "MyMap")
          extends StubEntityType {
    var refreshCount = 0

    def valueFields = List[BaseField](
      persisted[String]("name") + viewId(R.id.name, textView),
      persisted[Int]("age") + viewId(R.id.age, intView),
      //here to test a non-UI field
      persisted[String]("uri") + Getter[UriPath,String](u => Some(u.toString)))


    override def rLayoutClasses = classOf[testres.R.layout] +: super.rLayoutClasses

    def newWritable = Map[String,Any]()

    protected def createEntityPersistence(crudContext: CrudContext) = persistence

    def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
      activity.setListAdapter(listAdapter)
    }

    def refreshAfterDataChanged(listAdapter: ListAdapter) {
      refreshCount += 1
    }
  }
}