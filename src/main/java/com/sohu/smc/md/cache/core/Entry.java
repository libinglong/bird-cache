package com.sohu.smc.md.cache.core;

import lombok.Data;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
@Data
public class Entry {

    private Object keyObj;
    private Object originObj;
    private ValueWrapper valueWrapper;

}
