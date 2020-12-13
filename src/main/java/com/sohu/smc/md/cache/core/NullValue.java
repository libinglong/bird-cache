package com.sohu.smc.md.cache.core;

import org.springframework.util.Assert;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 这个类解决如下问题
 * 缓存null值和缓存没有命中的区别
 * reactor不支持发射null
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public class NullValue {

    /**
     * 表示缓存没有命中
     */
    public static final NullValue MISS_NULL = new NullValue(null, true);
    /**
     * 表示结果本身就是NULL
     */
    public static final NullValue REAL_NULL = new NullValue(null, false);

    /**
     * 处理批量逻辑时,如果缓存的值为NULL时,该key作为缓存的键.
     */
    private final Object key;
    /**
     * true表示当前值是缓存没有命中生成的
     */
    private final boolean isMissing;

    private NullValue(Object key, boolean isMissing) {
        this.key = key;
        this.isMissing = isMissing;
    }

    public static NullValue of(Object key) {
        Assert.notNull(key, "key must not be null");
        return new NullValue(key, false);
    }

    public Object get() {
        if (key == null){
            throw new NoSuchElementException("key must not be null");
        }
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NullValue nullValue = (NullValue) o;
        return isMissing == nullValue.isMissing &&
                Objects.equals(key, nullValue.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, isMissing);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NullValue.class.getSimpleName() + "[", "]")
                .add("key=" + key)
                .add("isMissing=" + isMissing)
                .toString();
    }
}
