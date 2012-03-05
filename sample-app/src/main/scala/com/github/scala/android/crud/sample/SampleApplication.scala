package com.github.scala.android.crud.sample

import com.github.scala.android.crud._


/** The sample application
  * @author Eric Pabst (epabst@gmail.com)
  */
class SampleApplication extends CrudApplication {
  val name = "Sample Application"

  val AuthorCrudType = new CrudType(AuthorEntityType, SQLitePersistenceFactory)
  val BookCrudType = new CrudType(BookEntityType, SQLitePersistenceFactory)
  val PublisherCrudType = new CrudType(PublisherEntityType, SQLitePersistenceFactory)

  def allCrudTypes = List(AuthorCrudType, BookCrudType, PublisherCrudType)

  def dataVersion = 2
}

class SampleAndroidApplication extends CrudAndroidApplication(new SampleApplication)

class SampleBackupAgent extends CrudBackupAgent(new SampleApplication)