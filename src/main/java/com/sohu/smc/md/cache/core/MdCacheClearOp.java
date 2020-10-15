package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheClear;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class MdCacheClearOp extends AbstractOp<MdCacheClear> {

    public MdCacheClearOp(MetaData<MdCacheClear> metaData, Cache cache, CacheConfig cacheConfig,
                         SpelParseService spelParseService) {
        super(metaData, cache, cacheConfig, spelParseService);
    }

    public void clear(){
        cache.clear();
    }

}
