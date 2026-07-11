package com.zbkj.service.util.yly;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String getMD5Str(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] encrypt = md5.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte t : encrypt) {
                sb.append(HEX[(t >>> 4) & 0x0f]);
                sb.append(HEX[t & 0x0f]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM不支持MD5", e);
        }
    }

    public static boolean isNull(String content) {
        return content == null || content.isEmpty();
    }

    public static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }
}
