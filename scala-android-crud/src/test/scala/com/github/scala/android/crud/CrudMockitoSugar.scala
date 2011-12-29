package com.github.scala.android.crud

import org.scalatest.mock.MockitoSugar
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.mockito.{Matchers, Mockito}

/** MockitoSugar with some additions.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait CrudMockitoSugar extends MockitoSugar {

  def namedMock[T <: AnyRef](name: String)(implicit manifest: Manifest[T]): T = {
    Mockito.mock(manifest.erasure.asInstanceOf[Class[T]], name)
  }

  class CapturingAnswer[T](result: => T) extends Answer[T] {
    var params: List[Any] = Nil

    def answer(invocation: InvocationOnMock) = {
      params = invocation.getArguments.toList
      result
    }
  }

  def capturingAnswer[T](result: => T): CapturingAnswer[T] = new CapturingAnswer({ result })

  def answer[T](result: => T) = new Answer[T] {
    def answer(invocation: InvocationOnMock) = result
  }

  def answerWithInvocation[T](result: InvocationOnMock => T) = new Answer[T] {
    def answer(invocation: InvocationOnMock) = result(invocation)
  }

  def eql[T](value: T): T = Matchers.eq(value)
}
