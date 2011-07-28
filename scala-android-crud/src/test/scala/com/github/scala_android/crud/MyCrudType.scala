package com.github.scala_android.crud

import res.R
import com.github.triangle.BaseField
import android.app.ListActivity

/**
 * A simple CrudType for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudType extends CrudType with StubEntityType {
  def entityName = "MyEntity"

  def fields = List[BaseField]()

  def cancelItemString = R.string.cancel_item

  def refreshAfterSave(crudContext: CrudContext) {}

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    throw new UnsupportedOperationException
  }

  def openEntityPersistence(crudContext: CrudContext) = throw new UnsupportedOperationException

  def newWritable = throw new UnsupportedOperationException
}

trait StubEntityType extends CrudType {
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
