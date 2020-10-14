package com.sohu.smc.md.cache.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 此类用于缓存一些中间变量
 * 每个方法的一次调用对应一个InvocationContext
 * 每个方法对应多个AbstractOp
 * 因此一个InvocationContext对应多个AbstractOp
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/9
 */
@Slf4j
public class InvocationContext {

    private AsyncTaskExecutor mdInvExecutor;

    @Getter
    private final MethodInvocation methodInvocation;

    private Map<AbstractKeyOp<?>, OpContext> opContextMap = new ConcurrentHashMap<>(4);

    /**
     * 执行时间 ms
     */
    @Setter
    private Long execTime;

    public InvocationContext(MethodInvocation methodInvocation, AsyncTaskExecutor mdInvExecutor){
        this.methodInvocation = methodInvocation;
        this.mdInvExecutor = mdInvExecutor;
    }

    public OpContext getOpContext(AbstractKeyOp<?> abstractKeyOp) {
        return opContextMap.computeIfAbsent(abstractKeyOp, abstractOpTmp -> new OpContext());
    }

    public Object doInvoke() throws Throwable {
        return mdInvExecutor.submit(() -> {
            try {
                return methodInvocation.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }).get(execTime, TimeUnit.MILLISECONDS);
    }

}
