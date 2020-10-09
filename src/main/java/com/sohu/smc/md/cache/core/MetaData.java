package com.sohu.smc.md.cache.core;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
@Data
public class MetaData<A> {
    private Method method;
    private Class<?> opCls;
    private A anno;
}