package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.*;
import com.sohu.smc.md.cache.core.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class CacheOpParseService {

    private final CacheManager cacheManager;
    private final CacheProperty defaultConfig;
    private final SpelParseService spelParseService;

    public CacheOpParseService(CacheManager cacheManager, CacheProperty defaultConfig, SpelParseService spelParseService) {
        this.cacheManager = cacheManager;
        this.defaultConfig = defaultConfig;
        this.spelParseService = spelParseService;
    }

    public MethodOpContext getContext(Method method){
        MethodOpContext methodOpContext = new MethodOpContext();
        methodOpContext.setEvictOps(parseEvictOps(method));
        methodOpContext.setPutOps(parsePutOps(method));
        methodOpContext.setCacheableOp(parseCacheableOp(method));
        methodOpContext.setBatchCacheOp(parseBatchCacheOp(method));
        methodOpContext.setClearOp(parseClearOp(method));
        return methodOpContext;
    }

    private MdCacheClearOp parseClearOp(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, MdCacheClear.class))
                .map(mdCacheClear -> new MdCacheClearOp(cacheManager.getCache(cacheSpaceName), cacheManager.needSync(), cacheManager.getSyncHandler()))
                .orElse(null);
    }

    private MdBatchCacheOp parseBatchCacheOp(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, MdBatchCache.class))
                .map(mdBatchCache -> new MdBatchCacheOp(mdBatchCache, cacheManager.getCache(cacheSpaceName),
                        cacheManager.getSecondaryCache(cacheSpaceName), mergedCacheConfig(method, defaultConfig), spelParseService))
                .orElse(null);
    }

    private MdCacheableOp parseCacheableOp(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, MdCacheable.class))
                .map(mdCacheable -> new MdCacheableOp(mdCacheable, cacheManager.getCache(cacheSpaceName),
                        cacheManager.getSecondaryCache(cacheSpaceName), mergedCacheConfig(method, defaultConfig), spelParseService))
                .orElse(null);
    }

    private List<MdCachePutOp> parsePutOps(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(method, MdCachePut.class)
                .stream()
                .map(mdCachePut -> new MdCachePutOp(mdCachePut, cacheManager.getCache(cacheSpaceName), mergedCacheConfig(method, defaultConfig),
                        spelParseService, cacheManager.needSync(), cacheManager.getSyncHandler()))
                .collect(Collectors.toList());
    }

    private List<MdCacheEvictOp> parseEvictOps(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(method, MdCacheEvict.class)
                .stream()
                .map(mdCacheEvict -> new MdCacheEvictOp(mdCacheEvict, cacheManager.getCache(cacheSpaceName), mergedCacheConfig(method, defaultConfig),
                        spelParseService, cacheManager.needSync(), cacheManager.getSyncHandler()))
                .collect(Collectors.toList());
    }


    private CacheProperty mergedCacheConfig(Method method, CacheProperty defaultCacheProperty){
        BeanWrapper retWrap = new BeanWrapperImpl(defaultCacheProperty.clone());
        MethodProp config = AnnotationUtils.getAnnotation(method, MethodProp.class);
        if (config != null){
            Prop[] props = config.props();
            Arrays.stream(props)
                    .forEach(prop -> retWrap.setPropertyValue(prop.name(),prop.value()));
        }
        return (CacheProperty) retWrap.getWrappedInstance();
    }

}
