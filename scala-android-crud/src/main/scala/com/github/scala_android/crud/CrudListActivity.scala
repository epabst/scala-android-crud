package com.github.scala_android.crud

import _root_.android.widget.ListView
import _root_.android.app.ListActivity
import android.os.Bundle
import android.net.Uri
import android.view.{ContextMenu, View, MenuItem, Menu}
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo

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
class CrudListActivity[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](val entityType: CrudEntityType[Q,L,R,W], val childEntities: List[CrudEntityTypeRef])
  extends ListActivity with CrudContext[Q,L,R,W] {

  type ID = Long
  val ADD_DIALOG_ID = 100
  val EDIT_DIALOG_ID = 101

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityType.entityName);
  private var persistence: Option[EntityPersistence[Q,L,R,W]] = None

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(entityType.listLayout)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    if (getIntent.getData() == null) getIntent.setData(defaultContentUri);

    val view = getListView();
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater().inflate(entityType.headerLayout, null));
    registerForContextMenu(getListView)

    val persistence = openEntityPersistence()
    setListAdapter(persistence.createListAdapter(this))
    this.persistence = Some(persistence)
  }


  override def onDestroy {
    persistence.map(_.close())
    persistence = None
    super.onDestroy
  }

  override def onResume() {
    verbose("onResume")
    entityType.refreshAfterSave(getListAdapter)
    super.onResume
  }

  protected def contextMenuActions: List[UIAction[ID]] = entityType.getEntityActions(actionFactory) match {
    case _ :: tail => tail.filter(_.title.isDefined)
    case Nil => Nil
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    val actions = contextMenuActions
    actions.foreach(action => menu.add(0, actions.indexOf(action), actions.indexOf(action), action.title.get))
  }

  override def onContextItemSelected(item: MenuItem) = {
    val actions = contextMenuActions
    val action = actions(item.getItemId)
    val info = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]
    action.apply(info.id)
    true
  }

  protected def optionsMenuActions: List[UIAction[Unit]] =
    entityType.getListActions(actionFactory).filter(action => action.title.isDefined || action.icon.isDefined)

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val listActions = optionsMenuActions
    for (action <- listActions) {
      val menuItem = if (action.title.isDefined) {
        menu.add(0, listActions.indexOf(action), listActions.indexOf(action), action.title.get)
      } else {
        menu.add(0, listActions.indexOf(action), listActions.indexOf(action), "")
      }
      action.icon.map(icon => menuItem.setIcon(icon))
    }
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    val listActions = optionsMenuActions
    val action = listActions(item.getItemId)
    action.apply(Unit)
    true
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: ID) {
    if (id >= 0) {
      entityType.getEntityActions(actionFactory).headOption.map(_(id)).getOrElse {
        warn("There are no entity actions defined for " + entityType)
      }
    } else {
      debug("Ignoring " + entityType + ".onListItemClick(" + id + ")")
    }
  }
}
