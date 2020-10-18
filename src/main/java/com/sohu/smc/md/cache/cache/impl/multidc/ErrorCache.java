package com.sohu.smc.md.cache.cache.impl.multidc;

import java.util.Map;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
class ErrorCache {

    String cacheSpaceName;
    ErrorOp errorOp;
    Object key;
    Object value;
    Map<Object,Object> kvs;
    Throwable e;

}
