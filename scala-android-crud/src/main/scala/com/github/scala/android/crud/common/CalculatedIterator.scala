package com.github.scala.android.crud.common

/**
 * An Iterator whose items are calculated lazily.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 11/11/11
 * Time: 3:39 PM
 */
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
