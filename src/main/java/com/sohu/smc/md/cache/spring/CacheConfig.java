package com.sohu.smc.md.cache.spring;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * 默认缓存两周
     */
    private static Long DEFAULT_EXPIRE_TIME = 14 * 24 * 60 * 60 * 1000L;

    /**
     * 默认的过期时间 ms
     */
    private Long expireTime = DEFAULT_EXPIRE_TIME;

    /**
     * 当缓存发生变更前,会将对应的key过期,然后执行方法,之后再删除key.过期时间应该等于方法执行时间加非预期时间
     * 方法预期的执行时间 ms
     * 参见{@link CacheConfig#unexpectedTime}
     */
    private Long execTime = 200L;

    /**
     * 方法预期的执行时间以外的无法预料的时间 ms
     */
    private Long unexpectedTime = 100L;

    /**
     * 异步执行默认线程池
     */
    private ExecutorService executorService = Executors.newCachedThreadPool();

}
