package com.sohu.smc.md.cache.core;

import org.aopalliance.intercept.MethodInvocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于缓存一次调用中的临时变量
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
