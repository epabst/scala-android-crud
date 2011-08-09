package com.github.scala_android.crud.generate

import android.view.View
import com.github.scala_android.crud.ViewField.ViewIdField
import com.github.triangle.{BaseField, SubjectField, FieldList}
import com.github.scala_android.crud.{PlatformTypes, CursorField, CrudType}
import java.lang.reflect.{Modifier, Field}
import com.github.scala_android.crud.monitor.Logging
import java.lang.IllegalStateException
import com.github.scala_android.crud.model.IdPk

/**
 * A UI Generator for a CrudTypes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/4/11
 * Time: 3:19 PM
 */

object CrudUIGenerator extends PlatformTypes with Logging {
  def generateLayouts(crudType: CrudType) {//}, baseOutputDirectory: Path = Directory.Current.get) {
    generateLayouts(crudType, detectResourceIdClasses(crudType.getClass))
  }

  private[generate] def detectResourceIdClasses(clazz: Class[_]): Seq[Class[_]] = {
    findResourceIdClass(clazz.getClassLoader, clazz.getPackage.getName).toSeq ++ Seq(classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id])
  }

  private[generate] def findResourceIdClass(classLoader: ClassLoader, packageName: String): Option[Class[_]] = {
    try { Some(classLoader.loadClass(packageName + ".R$id")) }
    catch { case e: ClassNotFoundException =>
      val parentPackagePieces = packageName.split('.').dropRight(1)
      if (parentPackagePieces.isEmpty) None else findResourceIdClass(classLoader, parentPackagePieces.mkString("."))
    }
  }

  def generateLayouts(crudType: CrudType, resourceIdClasses: Seq[Class[_]]) {
    println("Generating layout for " + crudType)
    val excludedFields = List(CursorField.persistedId, IdPk.idField)
    crudType.fields.filterNot(excludedFields.contains).foreach { field =>
      val viewIdFields = this.viewIdFields(field)
      val viewFieldsWithId = this.viewFields(FieldList.toFieldList(viewIdFields))
      val otherViewFields = this.viewFields(field).filterNot(viewFieldsWithId.contains)
      val viewResourceIds = viewIdFields.map(_.viewResourceId).map { id =>
        findFieldWithIntValue(resourceIdClasses, id).map(_.getName).getOrElse {
          throw new IllegalStateException("Unable to find R.id with value " + id)
        }
      }
      val foreignKeys = CursorField.foreignKeys(field)
      val persistedFieldsInForeignKeys = foreignKeys.flatMap(CursorField.persistedFields(_))
      val otherPersistedFields = CursorField.persistedFields(field).filterNot(persistedFieldsInForeignKeys.contains)
      println("viewIds: " + viewResourceIds + " tied to " +
              viewFieldsWithId + "  /  other views: " + otherViewFields + "  /  foreignKeys: " + foreignKeys + " / other persisted: " + otherPersistedFields)
    }
  }

  private def findFieldWithIntValue(classes: Seq[Class[_]], value: Int): Option[Field] = {
    classes.view.flatMap { c =>
      c.getDeclaredFields.find { field => Modifier.isStatic(field.getModifiers) &&  field.getInt(null) == value }
    }.headOption
  }

  def viewFields(field: BaseField): List[SubjectField] = {
    field.deepCollect[SubjectField] {
      case subjectField: SubjectField if classOf[View].isAssignableFrom(subjectField.subjectManifest.erasure) => {
        subjectField
      }
    }
  }

  def viewIdFields(field: BaseField): List[ViewIdField[_]] = {
    field.deepCollect[ViewIdField[_]] {
      case viewIdField: ViewIdField[_] => viewIdField
    }
  }
}
