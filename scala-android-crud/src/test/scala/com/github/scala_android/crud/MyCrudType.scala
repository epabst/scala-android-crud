package com.github.scala_android.crud

import res.R
import com.github.triangle.BaseField
import android.app.ListActivity
import android.widget.ListAdapter
import persistence.CursorField.persisted

/**
 * A simple CrudType for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudType extends MyCrudType

class MyCrudType extends CrudType with StubEntityType {
  def entityName = "MyEntity"

  def valueFields = List[BaseField](persisted[Int]("count"))

  def cancelItemString = R.string.cancel_item

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity) {
    throw new UnsupportedOperationException
  }

  def openEntityPersistence(crudContext: CrudContext): CrudPersistence = throw new UnsupportedOperationException

  def newWritable: AnyRef = throw new UnsupportedOperationException
}

trait StubEntityType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"
  val addItemString = R.string.add_item
  val editItemString = R.string.edit_item

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
