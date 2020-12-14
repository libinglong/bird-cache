package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.*;
import com.sohu.smc.md.cache.core.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
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

    private final CacheProperty globalCacheProperty;
    private final SpelParseService spelParseService;
    private final ApplicationContext ctx;

    public CacheOpParseService(ApplicationContext ctx, CacheProperty globalCacheProperty, SpelParseService spelParseService) {
        this.ctx = ctx;
        this.globalCacheProperty = globalCacheProperty;
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
                .map(mdCacheClear -> {
                    CacheProperty cacheProperty = mergedCacheConfig(method, globalCacheProperty);
                    CacheManager cacheManager = ctx.getBean(cacheProperty.getCacheManager(), CacheManager.class);
                    return new MdCacheClearOp(cacheSpaceName, cacheManager);
                })
                .orElse(null);
    }

    private MdBatchCacheOp parseBatchCacheOp(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, MdBatchCache.class))
                .map(mdBatchCache -> {
                    CacheProperty cacheProperty = mergedCacheConfig(method, globalCacheProperty);
                    CacheManager cacheManager = ctx.getBean(cacheProperty.getCacheManager(), CacheManager.class);
                    return new MdBatchCacheOp(mdBatchCache, cacheSpaceName, cacheManager, cacheProperty, spelParseService);
                })
                .orElse(null);
    }

    private MdCacheableOp parseCacheableOp(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, MdCacheable.class))
                .map(mdCacheable -> {
                    CacheProperty cacheProperty = mergedCacheConfig(method, globalCacheProperty);
                    CacheManager cacheManager = ctx.getBean(cacheProperty.getCacheManager(), CacheManager.class);
                    return new MdCacheableOp(mdCacheable, cacheSpaceName, cacheManager, cacheProperty, spelParseService);
                })
                .orElse(null);
    }

    private List<MdCachePutOp> parsePutOps(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(method, MdCachePut.class)
                .stream()
                .map(mdCachePut -> {
                    CacheProperty cacheProperty = mergedCacheConfig(method, globalCacheProperty);
                    CacheManager cacheManager = ctx.getBean(cacheProperty.getCacheManager(), CacheManager.class);
                    return new MdCachePutOp(mdCachePut, cacheSpaceName, cacheManager, cacheProperty, spelParseService);
                })
                .collect(Collectors.toList());
    }

    private List<MdCacheEvictOp> parseEvictOps(Method method) {
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(method, MdCacheEvict.class)
                .stream()
                .map(mdCacheEvict -> {
                    CacheProperty cacheProperty = mergedCacheConfig(method, globalCacheProperty);
                    CacheManager cacheManager = ctx.getBean(cacheProperty.getCacheManager(), CacheManager.class);
                    return new MdCacheEvictOp(mdCacheEvict, cacheSpaceName, cacheManager, cacheProperty, spelParseService);
                })
                .collect(Collectors.toList());
    }


    private CacheProperty mergedCacheConfig(Method method, CacheProperty defaultCacheProperty){
        BeanWrapper retWrap = new BeanWrapperImpl(defaultCacheProperty.clone());
        MProp clsConfig = AnnotationUtils.getAnnotation(method.getDeclaringClass(), MProp.class);
        if (clsConfig != null){
            Prop[] props = clsConfig.props();
            Arrays.stream(props)
                    .forEach(prop -> retWrap.setPropertyValue(prop.name(),prop.value()));
        }
        MProp methodconfig = AnnotationUtils.getAnnotation(method, MProp.class);
        if (methodconfig != null){
            Prop[] props = methodconfig.props();
            Arrays.stream(props)
                    .forEach(prop -> retWrap.setPropertyValue(prop.name(),prop.value()));
        }
        return (CacheProperty) retWrap.getWrappedInstance();
    }

}
