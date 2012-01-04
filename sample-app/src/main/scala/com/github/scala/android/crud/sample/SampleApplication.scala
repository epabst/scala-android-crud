package com.github.scala.android.crud.sample

import com.github.scala.android.crud.{SQLitePersistenceFactory, CrudBackupAgent, CrudApplication}


/** The sample application
  * @author Eric Pabst (epabst@gmail.com)
  */
object SampleApplication extends CrudApplication {
  val name = "Sample Application"

  val AuthorCrudType = new AuthorCrudType(SQLitePersistenceFactory)
  val BookCrudType = new BookCrudType(SQLitePersistenceFactory)
  val PublisherCrudType = new PublisherCrudType(SQLitePersistenceFactory)

  def allCrudTypes = List(AuthorCrudType, BookCrudType, PublisherCrudType)

  def dataVersion = 2
}

class SampleBackupAgent extends CrudBackupAgent(SampleApplication)