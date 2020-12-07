package com.sohu.smc.md.cache.core;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public class NullValue {

    private Object key;

    /**
     * 如果缓存null值,则使用该实例表示null
     */
    public static final NullValue NULL = new NullValue();

    private NullValue() {
    }

    private NullValue(Object key) {
        this.key = key;
    }

    public NullValue of(Object key){
        return new NullValue(key);
    }

    public Object getKey() {
        return key;
    }
}
