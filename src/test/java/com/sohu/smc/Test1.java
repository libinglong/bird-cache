package com.sohu.smc;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/17
 */
public class Test1 {

    private CountDownLatch latch = new CountDownLatch(1);

//    @Test
    public void fun() throws InterruptedException {
        CompletableFuture<Object> future = new CompletableFuture<>();
        boolean complete = future.complete(null);
        if (complete){
            System.out.println("com");
            Mono.fromFuture(future)
                    .subscribe(System.out::println);
        }
        System.out.println("aw");
        latch.await();
    }


}
