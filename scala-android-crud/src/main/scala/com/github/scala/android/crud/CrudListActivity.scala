package com.github.scala.android.crud

import action.Action
import android.widget.ListView
import _root_.android.app.ListActivity
import android.os.Bundle
import android.view.{ContextMenu, View, MenuItem}
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import com.github.triangle.{JavaUtil, PortableValue}
import common.UriPath
import JavaUtil.toRunnable
import persistence.PersistenceListener
import common.PlatformTypes._

/** A generic ListActivity for CRUD operations
  * @author Eric Pabst (epabst@gmail.com)
  */
class CrudListActivity(val crudType: CrudType, val application: CrudApplication)
  extends ListActivity with BaseCrudActivity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(crudType.listLayout)

    val view = getListView;
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater.inflate(crudType.headerLayout, null));
    registerForContextMenu(getListView)

    crudType.addPersistenceListener(new PersistenceListener {
      def onSave(id: ID) { crudType.refreshAfterDataChanged(getListAdapter) }
      def onDelete(uri: UriPath) { crudType.refreshAfterDataChanged(getListAdapter) }
    }, crudContext.context)

    crudType.setListAdapterUsingUri(crudContext, this)
    future {
      populateFromParentEntities()
      crudType.addPersistenceListener(new PersistenceListener {
        def onDelete(uri: UriPath) {
          //Some of the parent fields may be calculated from the children
          populateFromParentEntities()
        }

        def onSave(id: ID) {
          //Some of the parent fields may be calculated from the children
          populateFromParentEntities()
        }
      }, this)
    }
  }

  private def populateFromParentEntities() {
    val uriPath = currentUriPath
    //copy each parent Entity's data to the Activity if identified in the currentUriPath
    val portableValues: List[PortableValue] = crudType.parentEntities(application).flatMap { parentType =>
      crudType.copyFromPersistedEntity(uriPath, crudContext)
    }
    runOnUiThread {
      portableValues.foreach(_.copyTo(this, List(crudContext)))
    }
  }

  override def onResume() {
    trace("onResume")
    crudType.refreshAfterDataChanged(getListAdapter)
    super.onResume()
  }

  protected def contextMenuActions: List[Action] = crudType.getEntityActions(application) match {
    case _ :: tail => tail.filter(_.command.title.isDefined)
    case Nil => Nil
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    val commands = contextMenuActions.map(_.command)
    for ((command, index) <- commands.zip(Stream.from(0)))
      menu.add(0, command.commandId, index, command.title.get)
  }

  override def onContextItemSelected(item: MenuItem) = {
    val actions = contextMenuActions
    val info = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]
    actions.find(_.commandId == item.getItemId) match {
      case Some(action) => action.invoke(uriWithId(info.id), this); true
      case None => super.onContextItemSelected(item)
    }
  }

  protected def normalActions = crudType.getListActions(application)

  override def onListItemClick(l: ListView, v: View, position: Int, id: ID) {
    if (id >= 0) {
      crudType.getEntityActions(application).headOption.map(_.invoke(uriWithId(id), this)).getOrElse {
        warn("There are no entity actions defined for " + entityType)
      }
    } else {
      debug("Ignoring " + entityType + ".onListItemClick(" + id + ")")
    }
  }
}
