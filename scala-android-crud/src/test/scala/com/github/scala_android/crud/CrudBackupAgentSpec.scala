package com.github.scala_android.crud

import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers
import android.os.ParcelFileDescriptor
import android.widget.ListAdapter
import android.provider.BaseColumns
import scala.collection.mutable
import org.easymock.{IAnswer, EasyMock}
import EasyMock.notNull

/**
 * A test for {@link CrudBackupAgent}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/7/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudBackupAgentSpec extends MyEntityTesting with ShouldMatchers {
  val crudContext = new CrudContext(null)

  @Test
  def calculatedIteratorShouldWork() {
    val values = List("a", "b", "c").toIterator
    val iterator = new CalculatedIterator[String] {
      def calculateNextValue() = if (values.hasNext) Some(values.next()) else None
    }
    iterator.next() should be ("a")
    iterator.hasNext should be (true)
    iterator.hasNext should be (true)
    iterator.next() should be ("b")
    iterator.hasNext should be (true)
    iterator.next() should be ("c")
    iterator.hasNext should be (false)
    iterator.hasNext should be (false)
  }

  @Test
  def shouldSupportBackupAndRestore() {
    val application = mock[CrudApplication]
    val applicationB = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val backupTarget = mock[BackupTarget]
    val state1 = mock[ParcelFileDescriptor]
    val state1b = mock[ParcelFileDescriptor]

    val persistence = new MyEntityPersistence
    persistence.save(Some(100L), mutable.Map("name" -> "Joe", "age" -> 30))
    persistence.save(Some(101L), mutable.Map("name" -> "Mary", "age" -> 28))
    val persistence2 = new MyEntityPersistence
    persistence2.save(Some(101L), mutable.Map("city" -> "Los Angeles", "state" -> "CA"))
    persistence2.save(Some(104L), mutable.Map("city" -> "Chicago", "state" -> "IL"))
    val entityType = new MyEntityType(persistence, listAdapter)
    val entityType2 = new MyEntityType(persistence2, listAdapter, "OtherMap")
    val persistenceB = new MyEntityPersistence
    val persistence2B = new MyEntityPersistence
    val entityTypeB = new MyEntityType(persistenceB, listAdapter)
    val entityType2B = new MyEntityType(persistence2B, listAdapter, "OtherMap")
    val state0 = null
    var restoreItems = mutable.ListBuffer[RestoreItem]()
    expecting {
      call(application.allEntities).andReturn(List[CrudEntityTypeRef](entityType, entityType2))
      backupTarget.writeEntity(eql("MyMap#100"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("MyMap#101"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("OtherMap#101"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("OtherMap#104"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      call(applicationB.allEntities).andReturn(List[CrudEntityTypeRef](entityTypeB, entityType2B))
    }
    whenExecuting(application, applicationB, listAdapter, backupTarget, state1, state1b) {
      val backupAgent = new CrudBackupAgent(application)
      backupAgent.onCreate()
      backupAgent.onBackup(state0, backupTarget, state1)
      backupAgent.onDestroy()

      persistenceB.findAll(persistenceB.newCriteria).size should be (0)
      persistence2B.findAll(persistence2B.newCriteria).size should be (0)

      val backupAgentB = new CrudBackupAgent(applicationB)
      backupAgentB.onCreate()
      backupAgentB.onRestore(restoreItems.toIterator, 1, state1b)
      backupAgentB.onDestroy()

      val allB = persistenceB.findAll(persistenceB.newCriteria)
      allB.size should be (2)
      allB.map(persistenceB.getId) should be (List(100L, 101L))

      val all2B = persistence2B.findAll(persistence2B.newCriteria)
      all2B.size should be (2)
      all2B.map(persistence2B.getId) should be (List(101L, 104L))
    }
  }

  def saveRestoreItem(restoreItems: mutable.ListBuffer[RestoreItem]): IAnswer[Unit] = answer {
    val currentArguments = EasyMock.getCurrentArguments
    currentArguments(1).asInstanceOf[Option[Map[String,Any]]].foreach { map =>
      restoreItems += RestoreItem(currentArguments(0).asInstanceOf[String], map)
    }
  }
}