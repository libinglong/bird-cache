package com.sohu.smc.md.cache.cache.impl.multidc;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
public interface ErrorHandler {

    /**
     * 处理异步写次要缓存时发生的错误
     * @param errorCache 异步缓存错误事件
     */
    void handle(ErrorCache errorCache);

}
