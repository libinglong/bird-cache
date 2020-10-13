package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.*;
import com.sohu.smc.md.cache.core.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;

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

    @Autowired
    private CacheManage cacheManage;

    private ApplicationContext ctx;

    public MethodOpContext getContext(Method method){
        MethodOpContext methodOpContext = new MethodOpContext();
        methodOpContext.setEvictOps(parseCacheOps(method, MdCacheEvictOp.class));
        methodOpContext.setPutOps(parseCacheOps(method, MdCachePutOp.class));
        methodOpContext.setCacheableOp(parseCacheOp(method, MdCacheableOp.class));
        methodOpContext.setBatchCacheOp(parseCacheOp(method, MdBatchCacheOp.class));
        methodOpContext.setClearOp(parseCacheOp(method, MdCacheClearOp.class));
        return methodOpContext;
    }

    private <O> List<O> parseCacheOps(Method method, Class<O> opCls){
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(method, getAnnoByOp(opCls))
                .stream()
                .map(anno -> getOpBean(method, opCls, anno))
                .collect(Collectors.toList());
    }

    private <O> O parseCacheOp(Method method, Class<O> opCls){
        Annotation anno = AnnotatedElementUtils.findMergedAnnotation(method, getAnnoByOp(opCls));
        if (anno == null){
            return null;
        }
        return getOpBean(method, opCls, anno);
    }

    private <O> O getOpBean(Method method, Class<O> opCls, Annotation anno){
        MetaData<Object> metaData = new MetaData<>();
        metaData.setMethod(method);
        metaData.setOpCls(opCls);
        metaData.setAnno(anno);
        String cacheSpaceName = method.getDeclaringClass()
                .getName();
        return ctx.getBean(opCls, metaData, cacheManage.getCache(cacheSpaceName));
    }

    private Class<? extends Annotation> getAnnoByOp(Class<?> op){
        if (MdCacheableOp.class.equals(op)){
            return MdCacheable.class;
        }
        else if (MdBatchCacheOp.class.equals(op)){
            return MdBatchCache.class;
        }
        else if (MdCacheEvictOp.class.equals(op)){
            return MdCacheEvict.class;
        }
        else if (MdCachePutOp.class.equals(op)){
            return MdCachePut.class;
        }
        else if (MdCacheClearOp.class.equals(op)){
            return MdCacheClear.class;
        }
        else {
            throw new RuntimeException("code should never go here");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

}
