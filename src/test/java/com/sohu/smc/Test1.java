package com.sohu.smc;

import com.sohu.smc.md.cache.cache.SyncOp;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/17
 */
public class Test1 {

    private CountDownLatch latch = new CountDownLatch(1);

    @Test
    public void fun() throws InterruptedException {
        Hooks.onOperatorDebug();
        Flux.interval(Duration.of(10, ChronoUnit.MILLIS))
                .map(aLong -> {
                    System.out.println(aLong);
                    return aLong;
                })
                .flatMap(aLong -> {
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return "s";
                    });
                    return Mono.fromCompletionStage(future)
                            .timeout(Duration.of(5, ChronoUnit.MILLIS))
                            .onErrorResume(throwable -> Mono.empty());
                })
                .subscribe(new CoreSubscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String s) {
                        System.out.println("onNext");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("onError");
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });
        latch.await();
    }


}
