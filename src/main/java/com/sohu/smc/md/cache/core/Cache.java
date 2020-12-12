package com.sohu.smc.md.cache.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public interface Cache {

    /**
     * 获取当前缓存命名空间的名字
     * @return 当前缓存命名空间的名字
     */
    String getCacheSpaceName();

    /**
     * 设置key的过期时间
     * key在time指定的时间后过期
     * @param key the key of this cache
     * @param milliseconds time to expire in ms
     * @return
     */
    Mono<Void> expire(Object key, long milliseconds);

    /**
     * 删除key
     * @param key the key of this cache
     * @return
     */
    Mono<Void> delete(Object key);

    /**
     * 添加缓存
     * @param key the key of this cache
     * @param val the value to be updated of this cache
     * @param milliseconds time to expire in ms
     * @return
     */
    Mono<Void> set(Object key, Object val, long milliseconds);

    /**
     * 添加缓存
     * @param kvs kvs
     * @param milliseconds time time to expire in ms
     * @return Flux<String>
     */
    Mono<Void> setKvs(Map<Object,Object> kvs, long milliseconds);

    /**
     * 缓存查询
     * @param key the key of this cache
     * @return 缓存的对象.如果缓存未命中,不会有数据发射(相当于Mono.empty());如果缓存的值为null,发射{@link NullValue#NULL},
     */
    Mono<Object> get(Object key);

    /**
     * 批量缓存查询需要实现此接口,来达到更好的性能
     * 参数和返回值顺序必须是对应的
     * @param keys the keys of this cache
     * @return the list mapping to keys in order
     */
    Flux<Object> get(List<Object> keys);

    /**
     * 清空缓存
     * @return
     */
    Mono<Void> clear();

    Mono<Long> pttl(Object key);
}
