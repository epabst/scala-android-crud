package com.github.scala_android_crud.sample

import com.github.scala_android.crud._
import CursorField._
import ViewField._
import com.github.triangle._

/**
 * A CRUD type for Author.
 * @author pabstec
 */

object AuthorCrudType extends SQLiteCrudType {
  def entityName = "Author"

  def fields = List(persisted[String]("name") + viewId(R.id.name, textView))

  //Use the same layout for the header
  def headerLayout = R.layout.author_row
  def listLayout = R.layout.entity_list
  def displayLayout = None
  def entryLayout = R.layout.author_entry
  def rowLayout = R.layout.author_row

  def cancelItemString = res.R.string.cancel_item
  def editItemString = R.string.edit_author
  def addItemString = R.string.add_author

  def activityClass = classOf[AuthorActivity]
  def listActivityClass = classOf[AuthorListActivity]
}

class AuthorListActivity extends CrudListActivity(AuthorCrudType, SampleApplication)
class AuthorActivity extends CrudActivity(AuthorCrudType, SampleApplication)
