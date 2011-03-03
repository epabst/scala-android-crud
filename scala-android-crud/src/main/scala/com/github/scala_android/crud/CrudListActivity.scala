package com.github.scala_android.crud

import _root_.android.widget.ListView
import _root_.android.app.ListActivity
import android.os.Bundle
import android.net.Uri
import android.view.{View, MenuItem, Menu}

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 * @param Q the query criteria type
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */
abstract class CrudListActivity[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](val entityConfig: CrudEntityConfig[Q,L,R,W])
  extends ListActivity with CrudContext[Q,L,R,W] {

  type ID = Long
  val ADD_DIALOG_ID = 100
  val EDIT_DIALOG_ID = 101

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityConfig.entityName);

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(entityConfig.listLayout)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    if (getIntent.getData() == null) getIntent.setData(defaultContentUri);

    val view = getListView();
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater().inflate(entityConfig.headerLayout, null));

    val persistence = entityConfig.openEntityPersistence(activity)
    setListAdapter(persistence.createListAdapter(this))
  }

  override def onResume() {
    super.onResume
    refreshAfterSave()
  }

  //todo add support for item actions on long touch on an item

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val listActions = entityConfig.getListActions(actionFactory)
    for (action <- listActions if (action.title.isDefined || action.icon.isDefined)) {
      val menuItem = if (action.title.isDefined) {
        menu.add(0, listActions.indexOf(action), listActions.indexOf(action), action.title.get)
      } else {
        menu.add(0, listActions.indexOf(action), listActions.indexOf(action), "")
      }
      action.icon.map(icon => menuItem.setIcon(icon))
    }
    true
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = {
    val listActions = entityConfig.getListActions(actionFactory)
    val action = listActions(item.getItemId)
    action.apply()
    true
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: ID) {
    entityConfig.getEntityActions(actionFactory, id).headOption.map(_())
  }

  def refreshAfterSave()
}
