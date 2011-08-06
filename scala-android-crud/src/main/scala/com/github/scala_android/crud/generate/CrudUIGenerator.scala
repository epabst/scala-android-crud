package com.github.scala_android.crud.generate

//import tools.nsc.io.{Directory, Path}
import com.github.scala_android.crud.{ViewField, CursorField, CrudType}
import com.github.triangle.{SubjectField, FieldList}
import android.view.View

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
      val persistedFields = CursorField.persistedFields(FieldList(field))
      val viewFields = this.viewFields(FieldList(field))
      println("For " + field + ": persisted: " + persistedFields + " / view: " + viewFields)
    }
  }

  //todo test this
  def viewFields(fields: FieldList): List[SubjectField] = {
    fields.fieldFlatMap[SubjectField] {
      case subjectField: SubjectField if classOf[View].isAssignableFrom(subjectField.subjectManifest.erasure) => {
        List(subjectField)
      }
    }
  }
}