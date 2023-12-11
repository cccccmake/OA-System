package com.imooc.oa.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5Utils {
    public static String md5Digest(String source) {
        return DigestUtils.md5Hex(source);
    }

    public static String md5Digest(String source, Integer salt) {
        char[] charArray = source.toCharArray();
        for(int i = 0; i < charArray.length; i++){
            charArray[i] = (char) (charArray[i] + salt);
        }
        String target = new String(charArray);
        System.out.println(target);
        return DigestUtils.md5Hex(target);
    }
}

