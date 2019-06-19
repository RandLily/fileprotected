package cn.threedr3am.awd.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author xuanyh
 * @create 2018-12-01 10:26
 **/
public class MD5Util {

    private static final int MAX_READ_LENGTH = 100 * 1024;//每次最大读取100K，防止溢出

    /**
     * 对字符串md5加密
     *
     * @param str
     * @return
     */
    public static String getMD5(String str) {
        return getMD5(str.getBytes());
    }

    public static String getMD5(byte[] bytes) {
        // 生成一个MD5加密计算摘要
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 计算md5函数
        md.update(bytes);
        byte[] hash = md.digest();
        StringBuilder secpwd = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            int v = hash[i] & 0xFF;
            if (v < 16) secpwd.append(0);
            secpwd.append(Integer.toString(v, 16));
        }
        return secpwd.toString();
    }

    public static String getMD5(File readFile) {
        String md5 = null;
        if (readFile.length() > MAX_READ_LENGTH) {
            try {
                long fileLength = readFile.length();
                for (long j = 0; j < fileLength ; j+=Integer.MAX_VALUE) {
                    int mapLength = fileLength < (j + Integer.MAX_VALUE) ? (int) (
                        fileLength - j) : Integer.MAX_VALUE;
                    MappedByteBuffer mappedByteBuffer = new RandomAccessFile(readFile, "r")
                        .getChannel()
                        .map(FileChannel.MapMode.READ_ONLY, j, mapLength);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (long i = 0; i < mapLength; i+=MAX_READ_LENGTH) {
                        int length = mapLength < (i + MAX_READ_LENGTH) ? (int) (
                            mapLength - i) : MAX_READ_LENGTH;
                        byte[] ds = new byte[length];
                        for (int index = 0; index < length; index++) {
                            byte b = mappedByteBuffer.get();
                            ds[index] = b;
                        }
                        stringBuilder.append(getMD5(ds));
                    }
                    md5 = MD5Util.getMD5(stringBuilder.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            byte[] bytes = StreamUtil.readBytes(readFile);
            md5 = bytes != null ? MD5Util.getMD5(bytes) : "";
        }
        return md5;
    }
}
