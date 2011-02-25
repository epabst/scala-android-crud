package geeks.crud.android

/**
 * A CrudEntityConfig for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudEntityConfig extends AndroidCrudEntityConfig[Long]

trait SQLiteUIActionFactory extends AndroidUIActionFactory[Long]
