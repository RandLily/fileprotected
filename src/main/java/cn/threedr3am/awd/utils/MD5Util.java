package cn.threedr3am.awd.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author xuanyh
 * @create 2018-12-01 10:26
 **/
public class MD5Util {
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
}
