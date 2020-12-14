package com.sohu.smc.md.cache.spring;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */

@Getter
@Setter
@Accessors(chain = true)
public class CacheProperty implements Cloneable {

    public static final long DEFAULT_EXPIRE_TIME = 14 * 24 * 60 * 60 * 1000L;
    public static final long DEFAULT_DELAY_INVALID_TIME = 200L;

    /**
     * 过期时间 毫秒 0表示不过期
     */
    private long expireTime = DEFAULT_EXPIRE_TIME;

    /**
     * 当缓存发生变更前,会将对应的key过期,然后执行方法,之后再删除key.延迟过期时间应该略大于方法执行时间,默认200ms
     */
    private long delayInvalidTime = DEFAULT_DELAY_INVALID_TIME;

    @Override
    public CacheProperty clone() {
        try {
            return (CacheProperty) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("should never happen");
        }
    }
}
