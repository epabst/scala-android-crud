package com.github.scala_android.crud.generate

import com.github.scala_android.crud.{CursorField, CrudType}
import android.view.View
import com.github.scala_android.crud.ViewField.ViewIdField
import com.github.triangle.{BaseField, SubjectField, FieldList}

/**
 * A UI Generator for a CrudTypes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/4/11
 * Time: 3:19 PM
 */

object CrudUIGenerator {
  def generateLayouts(crudType: CrudType) {//}, baseOutputDirectory: Path = Directory.Current.get) {
    println("Generating layout for " + crudType)
    crudType.fields.foreach { field =>
      val viewIdFields = this.viewIdFields(field)
      val viewFields = this.viewFields(FieldList.toFieldList(viewIdFields))
      val persistedFields = CursorField.persistedFields(field)
      println("For " + field + ":\n    viewIds: " + viewIdFields.map(_.viewResourceId).mkString(",") + " / view: " + viewFields +
              " / persisted: " + persistedFields)
    }
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
