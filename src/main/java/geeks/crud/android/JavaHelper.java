package geeks.crud.android;

import android.content.ContentValues;

/**
 * A helper for allowing Scala to interact with Java.
 *
 * @author Eric Pabst (epabst@gmail.com)
 *         Date: 2/4/11
 *         Time: 5:56 PM
 */
public class JavaHelper {
  public static void putByte(ContentValues contentValues, String key, Byte value) {
    contentValues.put(key, value);
  }

  public static void putShort(ContentValues contentValues, String key, short value) {
    contentValues.put(key, value);
  }

  public static void putInt(ContentValues contentValues, String key, int value) {
    contentValues.put(key, value);
  }

  public static void putLong(ContentValues contentValues, String key, long value) {
    contentValues.put(key, value);
  }

  public static void putFloat(ContentValues contentValues, String key, float value) {
    contentValues.put(key, value);
  }

  public static void putDouble(ContentValues contentValues, String key, double value) {
    contentValues.put(key, value);
  }

  public static void putByteArray(ContentValues contentValues, String key, Byte[] value) {
    byte[] array = new byte[value.length];
    for (int i = 0; i < value.length; i++) {
      array[i] = value[i];
    }
    contentValues.put(key, array);
  }
}
