package com.github.scala_android.crud

import android.app.backup.{BackupDataOutput, BackupDataInput, BackupAgent}
import com.github.triangle.Logging
import persistence.CursorField._
import android.os.ParcelFileDescriptor
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import scala.collection.JavaConversions._
import java.util.{Map => JMap,HashMap}
import android.content.Context
import android.net.Uri
import collection.BufferedIterator

object CrudBackupAgent {
  private val backupStrategyVersion: Int = 1

  private[crud] def marshall(map: Map[String,Any]): Array[Byte] = {
    val out = new ByteArrayOutputStream
    try {
      val objectStream = new ObjectOutputStream(out)
      objectStream.writeInt(backupStrategyVersion)
      val jmap: JMap[String,Any] = map
      val hashMap: JMap[String,Any] = new HashMap(jmap)
      objectStream.writeObject(hashMap)
      out.toByteArray
    } finally out.close()
  }

  private[crud] def unmarshall(bytes: Array[Byte]): Map[String,Any] = {
    val objectStream = new ObjectInputStream(new ByteArrayInputStream(bytes))
    try {
      val strategyVersion = objectStream.readInt()
      if (strategyVersion != backupStrategyVersion) throw new IllegalStateException
      objectStream.readObject().asInstanceOf[JMap[String,Any]].toMap
    } finally objectStream.close()
  }
}

/**
 * A BackupAgent for a CrudApplication.
 * It must be subclassed in order to put it into the AndroidManifest.xml.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/7/11
 * Time: 4:35 PM
 */

import CrudBackupAgent._

class CrudBackupAgent(application: CrudApplication) extends BackupAgent with Logging {
  final def onBackup(oldState: ParcelFileDescriptor, data: BackupDataOutput, newState: ParcelFileDescriptor) {
    onBackup(oldState, new BackupTarget {
      def writeEntity(key: String, mapOpt: Option[Map[String,Any]]) {
        debug("Backing up " + key + " <- " + mapOpt)
        mapOpt match {
          case Some(map) =>
            val bytes = marshall(map)
            data.writeEntityHeader(key, bytes.length)
            data.writeEntityData(bytes, bytes.length)
            debug("Backed up " + key + " with " + bytes.length + " bytes")
          case None => data.writeEntityHeader(key, -1)
        }
      }
    }, newState)
  }

  def onBackup(oldState: ParcelFileDescriptor, data: BackupTarget, newState: ParcelFileDescriptor) {
    info("Backing up " + application)
    DeletedEntityIdCrudType.writeEntityRemovals(data, this)
    val crudContext = new CrudContext(this, application)
    application.allEntities.map(_ match {
      case generated: GeneratedCrudType[_] => //skip
      case entityType: CrudType => onBackup(entityType, data, crudContext)
    })
  }

  def onBackup(entityType: CrudType, data: BackupTarget, crudContext: CrudContext) {
    entityType.withEntityPersistence[Unit](crudContext, persistence => {
      persistence.findAll(Uri.EMPTY).foreach(entity => {
        val map = entityType.transform(Map[String,Any](), entity)
        val id = persistedId(entity)
        data.writeEntity(entityType.entityName + "#" + id, Some(map))
      })
    })
  }

  final def onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) {
    onRestore(new CalculatedIterator[RestoreItem] {
      def calculateNextValue(): Option[RestoreItem] = {
        if (data.readNextHeader) {
          val key = data.getKey
          val size = data.getDataSize
          val bytes = new Array[Byte](size)
          val actualSize = data.readEntityData(bytes, 0, size)
          debug("Restoring " + key + ": expected " + size + " bytes, read " + actualSize + " bytes")
          if (actualSize != size) throw new IllegalStateException("readEntityData returned " + actualSize + " instead of " + size)
          try {
            val map = unmarshall(bytes)
            debug("Restoring " + key + ": read Map: " + map)
            Some(RestoreItem(key, map))
          } catch {
            case e: Exception =>
              error("Unable to restore " + key, e)
              //skip this one and do the next
              calculateNextValue()
          }
        } else {
          None
        }
      }
    }, appVersionCode, newState)
  }

  def onRestore(data: Iterator[RestoreItem], appVersionCode: Int, newState: ParcelFileDescriptor) {
    info("Restoring backup of " + application)
    val crudContext = new CrudContext(this, application)
    val entities = application.allEntities
    data.foreach(restoreItem => {
      val entityName = restoreItem.key.substring(0, restoreItem.key.lastIndexOf("#"))
      entities.find(_.entityName == entityName).map(_ match {
        case entityType: CrudType => onRestore(entityType, restoreItem, crudContext)
      })
    })
  }

  def onRestore(entityType: CrudType, restoreItem: RestoreItem, crudContext: CrudContext) {
    debug("Restoring " + restoreItem.key + " <- " + restoreItem.map)
    val id = restoreItem.key.substring(restoreItem.key.lastIndexOf("#") + 1).toLong
    val writable = entityType.transform(entityType.newWritable, restoreItem.map)
    entityType.withEntityPersistence(crudContext, _.save(Some(id), writable))
    Unit
  }
}

private[crud] trait CalculatedIterator[T] extends BufferedIterator[T] {
  private var calculatedNextValue: Option[Option[T]] = None

  def calculateNextValue(): Option[T]

  private def determineNextValue(): Option[T] = {
    if (!calculatedNextValue.isDefined) {
      calculatedNextValue = Some(calculateNextValue())
    }
    calculatedNextValue.get
  }

  def hasNext = determineNextValue().isDefined

  def head = determineNextValue().get

  def next() = {
    val next = head
    calculatedNextValue = None
    next
  }
}

trait BackupTarget {
  def writeEntity(key: String, map: Option[Map[String,Any]])
}

case class RestoreItem(key: String, map: Map[String,Any])

/**
 * Helps prevent restoring entities that the user deleted when an onRestore operation happens.
 * It only contains the entityName and ID since it is not intended as a recycle bin,
 * but to delete data in the Backup Service.
 * This entity is in its own CrudApplication by itself, separate from any other CrudApplication.
 * It is intended to be in a separate database owned by the scala-android-crud framework.
 */
object DeletedEntityIdCrudType extends SQLiteCrudType with HiddenEntityType {

  def entityName = "DeletedEntityId"

  private val entityNameField = persisted[String]("entityName")
  private val entityIdField = persisted[ID]("entityId")
  def valueFields = List(entityNameField, entityIdField)

  private val application = new CrudApplication {
    def name = "scala_android_crud_deleted"

    def allEntities = List(DeletedEntityIdCrudType)
  }

  /**
   * Records that a deletion happened so that it is deleted from the Backup Service.
   * It's ok for this to happen immediately because if a delete is undone,
   * it will be restored independent of this support, and it will then be re-added to the Backup Service later
   * just like any new entity being added.
   */
  def recordDeletion(entityType: CrudType, id: ID, context: Context) {
    val crudContext = new CrudContext(context, application)
    val writable = transform(newWritable, Map(entityNameField.name -> entityType.entityName, entityIdField.name -> id))
    withEntityPersistence(crudContext, { persistence =>
      persistence.save(None, writable)
    })
  }

  def writeEntityRemovals(data: BackupTarget, context: Context) {
    val crudContext = new CrudContext(context, application)
    withEntityPersistence(crudContext, { persistence =>
      persistence.findAll(Uri.EMPTY).foreach { entity =>
        val deletedEntityName: String = entityNameField(entity)
        val deletedId: ID = entityIdField(entity)
        data.writeEntity(deletedEntityName + "#" + deletedId, None)
      }
    })
  }
}
