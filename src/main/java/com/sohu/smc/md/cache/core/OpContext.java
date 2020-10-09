package com.sohu.smc.md.cache.core;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/9
 */
public class OpContext {

    private byte[] prefixedKey;

    public byte[] getPrefixedKey() {
        return prefixedKey;
    }

    public void setPrefixedKey(byte[] prefixedKey) {
        this.prefixedKey = prefixedKey;
    }
}
