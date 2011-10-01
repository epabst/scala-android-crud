package com.github.scala.android.crud.common

/**
 * A single place to specify types that could vary between different platforms.
 * <p />
 * Note: When used for a constructor parameter, you may need to refer to the actual type instead of these defined types.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 10:13 PM
 */

trait PlatformTypes {
  /** An entity ID */
  final type ID = Long
  /** A string key used with translation. */
  final type SKey = Int
  /** An image key used with translation. */
  final type ImgKey = Int
  /** A layout key. */
  final type LayoutKey = Int
  /** A view key, which is a single element of a layout. */
  final type ViewKey = Int
}