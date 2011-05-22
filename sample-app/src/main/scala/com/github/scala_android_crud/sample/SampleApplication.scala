package com.github.scala_android_crud.sample

import com.github.scala_android.crud.{SQLiteCrudType, CrudBackupAgent, CrudApplication}

/**
 * The sample application
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:53 PM
 */

object SampleApplication extends CrudApplication with AuthorContext with BookContext {
  val name = "Sample Application"

  object AuthorCrudType extends AuthorCrudType with SQLiteCrudType
  object BookCrudType extends BookCrudType with SQLiteCrudType

  def allEntities = List(AuthorCrudType, BookCrudType)
}

class SampleBackupAgent extends CrudBackupAgent(SampleApplication)