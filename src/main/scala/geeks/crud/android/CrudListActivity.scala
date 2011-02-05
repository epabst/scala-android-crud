package geeks.crud.android

import android.os.Bundle
import android.widget.{SimpleCursorAdapter, ListAdapter, CursorAdapter}
import android.app.{AlertDialog, ListActivity}
import geeks.financial.futurebalance.persistence.EntityPersistence
import android.view.{View, MenuItem, Menu}
import android.content.DialogInterface

trait Field[E] {
  def copyToView(fromEntity: E, toEntryView: View)
  def copyToEntity(fromEntryView: View, toEntity: E)
  /** These will be put into a {@link android.content.ContentValues} */
  def valuesToPersist(entity: E): Map[String, Object]
  val queryFieldNames: List[String]
  val persistedFieldNamesWithView: List[String]
  //These should correspond with parallel entries in viewPersistedFieldNames
  val viewResourceIds: List[Int]
}

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
trait CrudListActivity[T] extends ListActivity {
  private val DIALOG_ID = 100

  val listLayout: Int
  val headerLayout: Int
  val rowLayout: Int
  val addItemString: Int
  val cancelItemString: Int
  val entryLayout: Int
  val addDialogTitleString: Int

  val fields: List[Field[T]]

  def listAdapter: ListAdapter

  def persistence: EntityPersistence[T]

  def refreshAfterSave(entity: T)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

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

trait SQLiteField

trait SQLiteCrud[T] extends CrudListActivity[T] {
  def persistence: SQLiteEntityPersistence[T]
  val fields: List[Field[T]]

  lazy val dataSource: CursorAdapter = new SimpleCursorAdapter(this, rowLayout, persistence.data,
    fields.flatMap(_.persistedFieldNamesWithView).toArray, fields.flatMap(_.viewResourceIds).toArray);

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave(entity: T) = dataSource.getCursor.requery
}