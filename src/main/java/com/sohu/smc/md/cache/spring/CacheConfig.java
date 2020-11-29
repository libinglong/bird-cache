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
public class CacheConfig {

    /**
     * 默认的过期时间 ms
     * 通常为节约使用缓存,缓存应该有过期时间限制,默认两周
     */
    private Long defaultExpireTime = 14 * 24 * 60 * 60 * 1000L;

    /**
     * 当缓存发生变更前,会将对应的key过期,然后执行方法,之后再删除key.延迟过期时间应该略大于方法执行时间,默认200ms
     */
    private Long defaultDelayInvalidTime = 200L;

}
