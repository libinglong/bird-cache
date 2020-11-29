package com.sohu.smc.md.cache.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

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

    @Getter
    private final MethodInvocation methodInvocation;

    private final Map<Object, OpContext> opContextMap = new ConcurrentHashMap<>(4);

    /**
     * 执行时间 ms
     */
    @Setter
    private Long execTime;

    public InvocationContext(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }

    public OpContext getOpContext(Object op) {
        return opContextMap.computeIfAbsent(op, unused -> new OpContext());
    }

    public Mono<?> doInvoke() {
        if (methodInvocation.getMethod()
                .getReturnType()
                .isAssignableFrom(Mono.class)) {
            return (Mono<?>) call();
        } else if (methodInvocation.getMethod()
                .getReturnType()
                .isAssignableFrom(Flux.class)) {
            return Mono.error(new RuntimeException("do not support Flux return type"));
        } else if (methodInvocation.getMethod()
                .getReturnType()
                .isAssignableFrom(CompletionStage.class)) {
            return Mono.fromCompletionStage((CompletionStage<?>) call());
        }
        return Mono.fromCallable(this::call)
                .subscribeOn(Schedulers.elastic())
                .timeout(Duration.of(execTime, ChronoUnit.MILLIS));
    }

    private Object call() {
        try {
            return methodInvocation.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
