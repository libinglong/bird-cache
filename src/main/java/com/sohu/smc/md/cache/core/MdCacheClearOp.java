package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheClear;
import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class MdCacheClearOp extends AbstractOp<MdCacheClear> {

    public MdCacheClearOp(MetaData<MdCacheClear> metaData, Cache cache, CacheProperties cacheProperties,
                         SpelParseService spelParseService) {
        super(metaData, cache, cacheProperties, spelParseService);
    }

    public void clear(){
        cache.clear();
    }

}
