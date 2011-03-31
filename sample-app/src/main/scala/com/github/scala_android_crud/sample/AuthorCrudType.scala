package com.github.scala_android_crud.sample

import com.github.scala_android.crud._
import CursorFieldAccess._
import ViewFieldAccess._
import com.github.triangle._
import android.widget.TextView
import android.content.Context

/**
 * A CRUD type for Author.
 * @author pabstec
 */

object AuthorCrudType extends SQLiteCrudEntityType {

  def entityName = "Author"

  def fields = List(Field[String](persisted("name"), viewId[TextView,String](R.id.name)))

  def childEntities: List[CrudEntityTypeRef] = Nil  //todo add Book

  def activityClass = classOf[AuthorActivity]

  def listActivityClass = classOf[AuthorListActivity]

  def getDatabaseSetup(context: Context) = null

  def entryLayout = R.layout.author_entry

  def rowLayout = R.layout.author_row

  //Use the same layout for the header
  def headerLayout = R.layout.author_row

  def listLayout = R.layout.entity_list

  def displayLayout = None

  def cancelItemString = res.R.string.cancel_item

  def editItemString = R.string.edit_author

  def addItemString = R.string.add_author
}

class AuthorListActivity extends CrudListActivity(AuthorCrudType, AuthorCrudType.childEntities)
class AuthorActivity extends CrudActivity(AuthorCrudType, AuthorCrudType.childEntities)
