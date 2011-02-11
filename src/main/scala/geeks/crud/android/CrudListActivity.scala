package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.view.{MenuItem, Menu}
import android.widget.{SimpleCursorAdapter, ListAdapter, CursorAdapter}
import geeks.crud.EntityPersistenceComponent
import android.content.{Context, DialogInterface}
import android.net.Uri

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
trait CrudListActivity[T] extends ListActivity with EntityPersistenceComponent[T] {
  private val DIALOG_ID = 100

  def entityName: String
  def listLayout: Int
  def headerLayout: Int
  def rowLayout: Int
  def entryLayout: Int
  def addItemString: Int
  def addDialogTitleString: Int
  def cancelItemString: Int

  def fields: List[Field[T]]

  def listAdapter: ListAdapter

  def context: Context = this

  def refreshAfterSave(entity: T)

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityName);

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    if (getIntent.getData() == null) getIntent.setData(defaultContentUri);

    val view = getListView();
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater().inflate(headerLayout, null));

    setListAdapter(listAdapter)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, DIALOG_ID, 1, addItemString)
    true
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = {
    if (item.getItemId == DIALOG_ID) {
      showDialog(DIALOG_ID)
    }
    true
  }

  protected override def onCreateDialog(id: Int) = {
    val builder = new AlertDialog.Builder(this)
    val entryView = getLayoutInflater.inflate(entryLayout, null)
    builder.setView(entryView)
    builder.setTitle(addDialogTitleString)
    builder.setPositiveButton(addItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss
        val newEntity = persistence.create
        fields.foreach(_.copyToEntity(entryView, newEntity))
        persistence.save(newEntity)
        refreshAfterSave(newEntity)
      }
    })
    builder.setNegativeButton(cancelItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.cancel
      }
    })
    builder.create
  }
}

trait SQLiteCrudListActivity[T] extends CrudListActivity[T] with SQLiteEntityPersistenceComponent[T] {
  lazy val dataSource: CursorAdapter = new SimpleCursorAdapter(this, rowLayout, persistence.data,
    fields.flatMap(_.persistedFieldNamesWithView).toArray, fields.flatMap(_.viewResourceIds).toArray);

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave(entity: T) = dataSource.getCursor.requery
}
