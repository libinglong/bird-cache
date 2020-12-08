package com.sohu.smc;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/17
 */
public class Test1 {
    {
        Hooks.onOperatorDebug();
    }

    private CountDownLatch latch = new CountDownLatch(1);

//    @Test
    public void fun() throws InterruptedException {

        Flux.interval(Duration.of(10, ChronoUnit.MILLIS))
                .onBackpressureDrop()
                .flatMap(aLong -> {
                    System.out.println("map:" + aLong);
                    return Mono.error(new RuntimeException("hehehhe"))
                            .doOnError(throwable -> System.out.println("error1234"))
                            .onErrorResume(throwable -> Mono.empty())
                            ;
                }, 4,7)
//                .onErrorResume(throwable -> Mono.empty())
                .subscribe(new BaseSubscriber<Object>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Object value) {
                        System.out.println("sub hookOnNext:" + value);
                        request(1);
                    }

                    @Override
                    protected void hookOnError(Throwable throwable) {
                        System.out.println("sub error");
                        super.hookOnError(throwable);
                    }

                    @Override
                    protected void hookOnComplete() {
                        System.out.println("sub complete");
                        request(1);
                    }
                });
//                .map(aLong -> {
//                    System.out.println(aLong);
//                    return aLong;
//                })
//                .flatMap(aLong -> {
//                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        return "s";
//                    });
//                    return Mono.fromCompletionStage(future)
////                            .timeout(Duration.of(5, ChronoUnit.MILLIS))
////                            .onErrorResume(throwable -> Mono.empty())
//                            ;
//                })
//                .subscribe(new BaseSubscriber<Object>() {
//                    @Override
//                    protected void hookOnSubscribe(Subscription subscription) {
//                        request(1);
//                    }
//
//                    @Override
//                    protected void hookOnNext(Object value) {
//                        super.hookOnNext(value);
//                        request(1);
//                    }
//                });
        latch.await();
    }



//    @Test
    public void j() throws InterruptedException, ExecutionException {
        Scheduler elastic = Schedulers.elastic();
        Mono.just(1)
                .doOnNext(integer -> System.out.println("1" + Thread.currentThread().getName()))
                .doOnTerminate(() -> Mono.just(1).doOnNext(integer -> {
                    System.out.println("1:" +System.currentTimeMillis());
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("2" + Thread.currentThread().getName());
                }).subscribeOn(elastic).subscribe())
                .doOnNext(integer -> System.out.println("2:" + System.currentTimeMillis()))
                .subscribe();
        latch.await();
    }


}
