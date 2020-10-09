package com.sohu.smc.md.cache.core;

import java.io.Serializable;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
enum NullValue implements Serializable {

    /**
     * 如果缓存null值,则使用该实例表示null
     */
    NULL;

    private static final long serialVersionUID = 1L;

}
