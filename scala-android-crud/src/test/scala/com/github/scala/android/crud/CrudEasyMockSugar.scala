package com.github.scala.android.crud

import org.scalatest.mock.EasyMockSugar
import org.easymock.{IAnswer, EasyMock}

/**
 * EasyMockSugar with some additions.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/19/11
 * Time: 9:47 AM
 */

trait CrudEasyMockSugar extends EasyMockSugar {

  def namedMock[T <: AnyRef](name: String)(implicit manifest: Manifest[T]): T = {
    EasyMock.createMock(name, manifest.erasure.asInstanceOf[Class[T]])
  }

  class CapturingAnswer[T](result: => T) extends IAnswer[T] {
    var params: List[Any] = Nil

    def answer() = {
      params = EasyMock.getCurrentArguments.toList
      result
    }
  }

  def capturingAnswer[T](result: => T): CapturingAnswer[T] = new CapturingAnswer({ result })

  def answer[T](result: => T) = new IAnswer[T] {
    def answer = result
  }

  def eql[T](value: T): T = org.easymock.EasyMock.eq(value)
}
