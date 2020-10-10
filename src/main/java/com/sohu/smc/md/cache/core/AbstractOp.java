package com.sohu.smc.md.cache.core;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class AbstractOp<A extends Annotation> implements InitializingBean {

    protected MetaData<A> metaData;

    protected String cacheSpaceName;

    @Autowired
    protected CacheSpace cacheSpace;

    public AbstractOp(MetaData<A> metaData) {
        this.metaData = metaData;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cacheSpaceName = metaData.getMethod().getDeclaringClass().getName();
    }
}
