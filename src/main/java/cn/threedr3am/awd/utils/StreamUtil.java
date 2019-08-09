package cn.threedr3am.awd.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author met3d
 * @create 2018-12-04 12:09
 **/
public class StreamUtil {

  public static byte[] readBytes(File readFile) {
    BufferedInputStream bufferedInputStream = null;
    byte[] bytes = null;
    try {
      bufferedInputStream = new BufferedInputStream(new FileInputStream(readFile));
      bytes = new byte[bufferedInputStream.available()];
      bufferedInputStream.read(bytes);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (bufferedInputStream != null) {
        try {
          bufferedInputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return bytes;
  }

  public static File writeBytes(File write, byte[] bytes) {
    BufferedOutputStream targetBufferedOutputStream = null;
    try {
      targetBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(write));
      targetBufferedOutputStream.write(bytes);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (targetBufferedOutputStream != null) {
        try {
          targetBufferedOutputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return write;
  }
}
