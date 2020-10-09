package com.sohu.smc.md.cache.util;

import java.util.Arrays;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class PrefixedKeyUtils {

    public static byte[] getPrefixedKey(byte[] prefix, byte[] rawKey){
        byte[] prefixedKey = Arrays.copyOf(prefix, prefix.length + rawKey.length);
        System.arraycopy(rawKey, 0, prefixedKey, prefix.length, rawKey.length);
        return prefixedKey;
    }

}