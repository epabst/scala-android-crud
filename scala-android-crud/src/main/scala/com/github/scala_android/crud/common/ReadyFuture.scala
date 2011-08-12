package com.github.scala_android.crud.common

import actors.{InputChannel, Future}

/**
 * A Future that is ready.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 7/25/11
 * Time: 9:52 AM
 */
class ReadyFuture[+T](val readyValue: T) extends Future[T] {
  def isSet = true

  def apply() = readyValue

  def respond(action: (T) => Unit) { action(readyValue) }

  lazy val inputChannel = new InputChannel[T] {
    def ? = readyValue

    def react(f: PartialFunction[T, Unit]): Nothing = { f(readyValue); throw new IllegalStateException("this method can't return") }

    def reactWithin(msec: Long)(f: PartialFunction[Any, Unit]): Nothing = { react(f) }

    def receive[R](f: PartialFunction[T, R]) = f(readyValue)

    def receiveWithin[R](msec: Long)(f: PartialFunction[Any, R]) = receive(f)
  }
}
