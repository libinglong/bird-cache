package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.MdBatchCache;
import com.sohu.smc.md.cache.anno.MdCacheEvict;
import com.sohu.smc.md.cache.anno.MdCachePut;
import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.core.*;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Service
public class CacheOpParseService implements ApplicationContextAware {

    private ApplicationContext ctx;

    private ConcurrentReferenceHashMap<CacheKey, List<?>> cacheOpsMap = new ConcurrentReferenceHashMap<>(256);
    private ConcurrentReferenceHashMap<CacheKey, Object> cacheOpMap = new ConcurrentReferenceHashMap<>(256);


    public <O> List<O> getOps(Method method, Class<O> opCls){
        CacheKey cacheKey = new CacheKey();
        cacheKey.setMethod(method);
        cacheKey.setOpCls(opCls);
        List<?> cacheOps = cacheOpsMap.get(cacheKey);
        if (cacheOps != null){
            //noinspection unchecked
            return (List<O>) cacheOps;
        }
        //noinspection unchecked
        return (List<O>) cacheOpsMap.computeIfAbsent(cacheKey, this::parseCacheOps);
    }

    public <O> O getOp(Method method, Class<O> opCls){
        CacheKey cacheKey = new CacheKey();
        cacheKey.setMethod(method);
        cacheKey.setOpCls(opCls);
        Object o = cacheOpMap.get(cacheKey);
        if (o != null){
            //noinspection unchecked
            return (O) o;
        }
        //noinspection unchecked
        return (O) cacheOpMap.computeIfAbsent(cacheKey, this::parseCacheOp);
    }

    private List<?> parseCacheOps(CacheKey cacheKey){
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(cacheKey.getMethod(), getAnnoByOp(cacheKey.getOpCls()))
                .stream()
                .map(anno -> getOpBean(cacheKey,anno))
                .collect(Collectors.toList());
    }

    private Object parseCacheOp(CacheKey cacheKey){
        Annotation anno = AnnotatedElementUtils.findMergedAnnotation(cacheKey.getMethod(), getAnnoByOp(cacheKey.getOpCls()));
        if (anno == null){
            return null;
        }
        return getOpBean(cacheKey,anno);
    }

    private Object getOpBean(CacheKey cacheKey, Annotation anno){
        MetaData<Object> metaData = new MetaData<>();
        metaData.setMethod(cacheKey.getMethod());
        metaData.setOpCls(cacheKey.getOpCls());
        metaData.setAnno(anno);
        return ctx.getBean(cacheKey.getOpCls(), metaData);
    }

    private Class<? extends Annotation> getAnnoByOp(Class<?> op){
        if (MdCacheableOpAbstract.class.equals(op)){
            return MdCacheable.class;
        }
        else if (MdBatchCacheOp.class.equals(op)){
            return MdBatchCache.class;
        }
        else if (MdCacheEvictOpAbstract.class.equals(op)){
            return MdCacheEvict.class;
        }
        else if (MdCachePutOpAbstract.class.equals(op)){
            return MdCachePut.class;
        }
        else {
            throw new RuntimeException("code should never go here");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }


    @Data
    private static class CacheKey {
        private Method method;
        private Class<?> opCls;
    }

}
