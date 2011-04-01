package com.github.scala_android.crud

/**
 * A single place to specify types that could vary between different platforms.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 10:13 PM
 */

trait PlatformTypes {
  /** An entity ID */
  type ID = Long
  /** A string key used with translation. */
  type SKey = Int
}