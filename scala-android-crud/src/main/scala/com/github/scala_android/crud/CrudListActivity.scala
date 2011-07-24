package com.github.scala_android.crud

import _root_.android.widget.ListView
import _root_.android.app.ListActivity
import android.os.Bundle
import android.net.Uri
import android.view.{ContextMenu, View, MenuItem, Menu}
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import scala.actors.Futures.future
import com.github.triangle.{JavaUtil, PortableValue}
import JavaUtil.toRunnable

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
class CrudListActivity(val entityType: CrudType, val application: CrudApplication)
  extends ListActivity with BaseCrudActivity {

  val ADD_DIALOG_ID = 100
  val EDIT_DIALOG_ID = 101

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityType.entityName);

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityType.listLayout)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    val intent = getIntent
    future {
      if (intent.getData == null) intent.setData(defaultContentUri);
      val contextItems = List(intent, crudContext, Unit)
      //copy each parent Entity's data to the Activity if identified in the Intent's URI
      val portableValues: List[PortableValue] = entityType.parentEntities.flatMap(_ match {
        case parentType: CrudType =>
          parentType.findId(intent.getData).flatMap { id =>
            parentType.withEntityPersistence(crudContext, { persistence =>
              persistence.find(id).map { readable =>
                debug("Copying " + entityType.entityName + "#" + id + " to " + this)
                entityType.copyFromItem(readable :: contextItems)
              }
            })
          }
      })
      runOnUiThread {
        portableValues.foreach(_.copyTo(this))
      }
    }
    val view = getListView;
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater.inflate(entityType.headerLayout, null));
    registerForContextMenu(getListView)

    setListAdapter(entityType.createListAdapter(crudContext, this))
  }


  override def onDestroy() {
    entityType.destroyContextVars(crudContext)
    super.onDestroy()
  }

  override def onResume() {
    verbose("onResume")
    entityType.refreshAfterSave(crudContext)
    super.onResume()
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
