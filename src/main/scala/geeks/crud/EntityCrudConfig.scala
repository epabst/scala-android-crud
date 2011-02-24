package geeks.crud

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 * @param TT a translatable text identifier
 * @param LT a layout configuration
 */

trait EntityCrudConfig[TT,LT] {
  def entityName: String

  def fields: List[CopyableField]

  def headerLayout: LT
  def listLayout: LT
  def rowLayout: LT
  def entryLayout: LT

  def addItemString: TT
  def editItemString: TT
  def cancelItemString: TT
}