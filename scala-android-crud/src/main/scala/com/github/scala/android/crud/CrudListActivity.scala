package com.github.scala.android.crud

import android.widget.{ListAdapter, ListView}
import _root_.android.app.ListActivity
import action.{UriPath, Action}
import android.os.Bundle
import android.view.{ContextMenu, View, MenuItem}
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import com.github.triangle.{JavaUtil, PortableValue}
import JavaUtil.toRunnable
import persistence.PersistenceListener

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
    val view = getListView;
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater.inflate(entityType.headerLayout, null));
    registerForContextMenu(getListView)

    entityType.addPersistenceListener(new PersistenceListener {
      def onSave(id: ID) { entityType.refreshAfterDataChanged(getListAdapter) }
      def onDelete(uri: UriPath) { entityType.refreshAfterDataChanged(getListAdapter) }
    }, crudContext.context)

    entityType.setListAdapterUsingUri(crudContext, this)
    future {
      //copy each parent Entity's data to the Activity if identified in the currentUriPath
      val portableValues: List[PortableValue] = entityType.parentEntities.flatMap(_ match {
        case parentType: CrudType => parentType.copyFromPersistedEntity(uriPath, crudContext)
      })
      runOnUiThread {
        portableValues.foreach(_.copyTo(this))
      }
    }
  }

  override def setListAdapter(adapter: ListAdapter) {
    super.setListAdapter(adapter)
    adapter match {
      case adapterCaching: entityType.AdapterCaching =>
        entityType.addPersistenceListener(adapterCaching.cacheClearingPersistenceListener(this), this)
      case _ =>
    }
  }

  override def onDestroy() {
    entityType.destroyContextVars(crudContext.vars)
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
