package com.sohu.smc.spring;

import com.sohu.smc.anno.*;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public class CacheSourcePointcut extends StaticMethodMatcherPointcut {

    private final static Set<Class<? extends Annotation>> SET = new HashSet<>();
    static {
        SET.add(MdBatchCache.class);
        SET.add(MdCacheable.class);
        SET.add(MdCacheEvict.class);
        SET.add(MdCacheEvicts.class);
        SET.add(MdCachePut.class);
        SET.add(MdCachePuts.class);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return SET.stream()
                .anyMatch(method::isAnnotationPresent);
    }
}
