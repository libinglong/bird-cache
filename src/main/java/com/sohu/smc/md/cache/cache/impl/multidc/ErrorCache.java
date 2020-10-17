package com.sohu.smc.md.cache.cache.impl.multidc;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
class ErrorCache {

    String cacheSpaceName;
    String opName;
    Object key;
    Object value;
    Throwable e;

}
