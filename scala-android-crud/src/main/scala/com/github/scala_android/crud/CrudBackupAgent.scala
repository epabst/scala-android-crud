package com.github.scala_android.crud

import android.app.backup.{BackupDataOutput, BackupDataInput, BackupAgent}
import monitor.Logging
import CursorFieldAccess._
import android.os.ParcelFileDescriptor
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import scala.collection.mutable
import scala.collection.JavaConversions._
import java.util.{Map => JMap,HashMap}

object CrudBackupAgent {
  private val backupStrategyVersion: Int = 1

  private[crud] def marshall(map: mutable.Map[String,Any]): Array[Byte] = {
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

  private[crud] def unmarshall(bytes: Array[Byte]): mutable.Map[String,Any] = {
    val objectStream = new ObjectInputStream(new ByteArrayInputStream(bytes))
    try {
      val strategyVersion = objectStream.readInt()
      if (strategyVersion != backupStrategyVersion) throw new IllegalStateException
      objectStream.readObject().asInstanceOf[JMap[String,Any]]
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
      def writeEntity(key: String, mapOpt: Option[mutable.Map[String,Any]]) {
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
    val crudContext = new CrudContext(this)
    application.allEntities.map(_ match {
      case generated: GeneratedCrudType[_,_] => //skip
      case entityType: CrudEntityType[_,_,_,_] => onBackup(entityType, data, crudContext)
    })
  }

  def onBackup[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityType: CrudEntityType[Q,L,R,W], data: BackupTarget, crudContext: CrudContext) {
    entityType.withEntityPersistence[Unit](crudContext, persistence => {
      val all = persistence.findAll(persistence.newCriteria)
      persistence.toIterator(all).foreach(entity => {
        val map = mutable.Map[String,Any]()
        entityType.copyFields(entity, map)
        val id = persistedId.partialGet(entity).get.get
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
    val crudContext = new CrudContext(this)
    val entities = application.allEntities
    data.foreach(restoreItem => {
      val entityName = restoreItem.key.substring(0, restoreItem.key.lastIndexOf("#"))
      entities.find(_.entityName == entityName).map(_ match {
        case entityType: CrudEntityType[_,_,_,_] => onRestore(entityType, restoreItem, crudContext)
      })
    })
  }

  def onRestore[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityType: CrudEntityType[Q,L,R,W], restoreItem: RestoreItem, crudContext: CrudContext) {
    debug("Restoring " + restoreItem.key + " <- " + restoreItem.map)
    val id = restoreItem.key.substring(restoreItem.key.lastIndexOf("#") + 1).toLong
    val writable = entityType.newWritable
    entityType.copyFields(restoreItem.map, writable)
    entityType.withEntityPersistence(crudContext, _.save(Some(id), writable))
    Unit
  }
}

private[crud] trait CalculatedIterator[T] extends Iterator[T] {
  private var calculatedNextValue: Option[Option[T]] = None

  def calculateNextValue(): Option[T]

  private def determineNextValue(): Option[T] = {
    if (!calculatedNextValue.isDefined) {
      calculatedNextValue = Some(calculateNextValue())
    }
    calculatedNextValue.get
  }

  def hasNext = determineNextValue().isDefined

  def next() = {
    val next = determineNextValue().get
    calculatedNextValue = None
    next
  }
}

trait BackupTarget {
  def writeEntity(key: String, map: Option[mutable.Map[String,Any]])
}

case class RestoreItem(key: String, map: mutable.Map[String,Any])