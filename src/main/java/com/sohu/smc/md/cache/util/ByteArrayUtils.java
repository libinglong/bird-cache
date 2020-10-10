package com.sohu.smc.md.cache.util;

import java.util.Arrays;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class ByteArrayUtils {

    public static byte[] combine(byte[] bytes1, byte[] bytes2){
        byte[] prefixedKey = Arrays.copyOf(bytes1, bytes1.length + bytes2.length);
        System.arraycopy(bytes2, 0, prefixedKey, bytes1.length, bytes2.length);
        return prefixedKey;
    }

}