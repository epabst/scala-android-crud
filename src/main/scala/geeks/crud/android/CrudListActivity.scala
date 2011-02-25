package geeks.crud.android

import _root_.android.widget.{ListView, ListAdapter}
import android.content.Intent
import _root_.android.app.{Activity, AlertDialog, ListActivity}
import android.os.Bundle
import android.net.Uri
import android.view.{View, MenuItem, Menu}
import android.content.{Context, DialogInterface}
import geeks.crud._

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 * @param ID the ID type for the entity such as String or Long.
 * @param Q the query criteria type
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */
abstract class CrudListActivity[ID,Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityConfig: AndroidCrudEntityConfig[ID]) extends ListActivity {
  val ADD_DIALOG_ID = 100
  val EDIT_DIALOG_ID = 101

  def persistence: EntityPersistence[ID,Q,L,R,W]

  def listAdapter: ListAdapter

  def context: Context = this

  def refreshAfterSave()

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityConfig.entityName);

  private val activity: Activity = this

  lazy val actionFactory = new AndroidUIActionFactory[ID] {
    def currentUI = activity

    def startCreate(entityType: CrudEntityType[Int]) =
      throw new UnsupportedOperationException("not implemented yet")

    def displayList(entityType: CrudEntityType[Int], criteriaSource: AnyRef) = new AndroidCrudUIAction(entityConfig, activity) {
      def apply() {
        //todo put the entityConfig's entityName into the URI
        //todo don't assume it's a CursorCrudListActivity
        startActivity(new Intent(null, Uri.withAppendedPath(getIntent.getData, entityConfig.entityName), activity, classOf[CursorCrudListActivity]))
      }
    }

    /** By default, just startUpdate instead of just displaying. */
    def display(entityType: CrudEntityType[Int], id: ID) = startUpdate(entityConfig, id)

    def startUpdate(entityType: CrudEntityType[Int], id: ID) =
      throw new UnsupportedOperationException("not implemented yet")

    def startDelete(entityType: CrudEntityType[Int], ids: List[ID]) =
      throw new UnsupportedOperationException("not implemented yet")
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(entityConfig.listLayout)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    if (getIntent.getData() == null) getIntent.setData(defaultContentUri);

    val view = getListView();
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater().inflate(entityConfig.headerLayout, null));

    setListAdapter(listAdapter)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, ADD_DIALOG_ID, 1, entityConfig.addItemString)
    true
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = {
    if (item.getItemId == ADD_DIALOG_ID) {
      showDialog(ADD_DIALOG_ID)
    }
    true
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    entityConfig.getEntityActions(actionFactory, id.asInstanceOf[ID]).headOption.map(_())
  }

  /**
   * Creates an edit dialog in the given Context to edit the entity and save it.
   * @param entityToEdit an Entity instance to edit or None to add a new one
   */
  def createEditDialog(context: Activity, entityId: Option[ID], afterSave: () => Unit = () => {}): AlertDialog = {
    val builder = new AlertDialog.Builder(context)
    val entryView = context.getLayoutInflater.inflate(entityConfig.entryLayout, null)
    //Unit is used to set the default value if no entityId is provided
    val readable = entityId.map(persistence.find).getOrElse(Unit)
    entityConfig.copyFields(readable, entryView)
    builder.setView(entryView)
    builder.setTitle(if (entityId.isDefined) entityConfig.editItemString else entityConfig.addItemString)
    builder.setPositiveButton(if (entityId.isDefined) entityConfig.editItemString else entityConfig.addItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss
        val writable = persistence.newWritable
        entityConfig.copyFields(entryView, writable)
        persistence.save(entityId, writable)
        afterSave()
      }
    })
    builder.setNegativeButton(entityConfig.cancelItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.cancel
      }
    })
    builder.create
  }

  protected override def onCreateDialog(id: Int) = {
    createEditDialog(this, None, refreshAfterSave)
  }
}

abstract class AndroidCrudUIAction(val entityType: CrudEntityType[Int], val originalActivity: Activity) extends CrudUIAction[Int]
