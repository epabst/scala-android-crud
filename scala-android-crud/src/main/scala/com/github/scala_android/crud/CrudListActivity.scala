package com.github.scala_android.crud

import _root_.android.widget.ListView
import _root_.android.app.ListActivity
import action.Action
import android.os.Bundle
import android.net.Uri
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
      //copy each parent Entity's data to the Activity if identified in the Intent's URI
      val portableValues: List[PortableValue] = entityType.parentEntities.flatMap(_ match {
        case parentType: CrudType => parentType.copyFromPersistedEntity(intent, crudContext)
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
    verbose("onResume")
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
