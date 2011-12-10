package com.github.scala.android.crud.sample

import com.github.scala.android.crud.{SQLitePersistenceFactory, CrudBackupAgent, CrudApplication}


/**
 * The sample application
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:53 PM
 */

object SampleApplication extends CrudApplication with AuthorContext with BookContext {
  val name = "Sample Application"

  val AuthorCrudType = new AuthorCrudType(SQLitePersistenceFactory)
  val BookCrudType = new BookCrudType(SQLitePersistenceFactory)

  def allCrudTypes = List(AuthorCrudType, BookCrudType)
}

class SampleBackupAgent extends CrudBackupAgent(SampleApplication)