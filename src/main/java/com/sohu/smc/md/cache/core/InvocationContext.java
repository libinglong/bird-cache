package com.sohu.smc.md.cache.core;

import org.aopalliance.intercept.MethodInvocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此类用于缓存一些中间变量
 * 每个方法对应一个InvocationContext,多个AbstractOp
 * 因此一个InvocationContext对应多个AbstractOp
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/9
 */
public class InvocationContext {

    private final MethodInvocation methodInvocation;

    public InvocationContext(MethodInvocation methodInvocation){
        this.methodInvocation = methodInvocation;
    }

    private Map<AbstractOp<?>, OpContext> opContextMap = new ConcurrentHashMap<>(4);

    public MethodInvocation getMethodInvocation() {
        return methodInvocation;
    }

    public OpContext getOpContext(AbstractOp<?> abstractOp) {
        return opContextMap.computeIfAbsent(abstractOp, abstractOpTmp -> new OpContext());
    }
}
