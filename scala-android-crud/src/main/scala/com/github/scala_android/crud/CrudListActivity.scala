package com.github.scala_android.crud

import _root_.android.widget.ListView
import _root_.android.app.ListActivity
import action.Action
import android.os.Bundle
import android.view.{ContextMenu, View, MenuItem}
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import com.github.triangle.{JavaUtil, PortableValue}
import JavaUtil.toRunnable

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
class CrudListActivity(val entityType: CrudType, val application: CrudApplication)
  extends ListActivity with BaseCrudActivity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityType.listLayout)

    val uriPath = currentUriPath
    future {
      //copy each parent Entity's data to the Activity if identified in the currentUriPath
      val portableValues: List[PortableValue] = entityType.parentEntities.flatMap(_ match {
        case parentType: CrudType => parentType.copyFromPersistedEntity(uriPath, crudContext)
      })
      runOnUiThread {
        portableValues.foreach(_.copyTo(this))
      }
    }
    val view = getListView;
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater.inflate(entityType.headerLayout, null));
    registerForContextMenu(getListView)

    entityType.setListAdapter(crudContext, this)
  }


  override def onDestroy() {
    entityType.destroyContextVars(crudContext)
    super.onDestroy()
  }

  override def onResume() {
    trace("onResume")
    entityType.refreshAfterDataChanged(getListAdapter)
    super.onResume()
  }

  protected def contextMenuActions: List[Action] = entityType.getEntityActions(application) match {
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
    val info = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]
    actions(item.getItemId).invoke(uriWithId(info.id), this)
    true
  }

  protected def applicableActions = entityType.getListActions(application)

  override def onListItemClick(l: ListView, v: View, position: Int, id: ID) {
    if (id >= 0) {
      entityType.getEntityActions(application).headOption.map(_.invoke(uriWithId(id), this)).getOrElse {
        warn("There are no entity actions defined for " + entityType)
      }
    } else {
      debug("Ignoring " + entityType + ".onListItemClick(" + id + ")")
    }
  }
}
