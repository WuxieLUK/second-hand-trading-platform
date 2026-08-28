package org.example.user.util;

import java.security.MessageDigest;

public final class Md5Util {

    private Md5Util() {
    }

    public static String getMD5String(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(source.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("md5 encrypt error", ex);
        }
    }
}
