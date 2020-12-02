package com.sohu.smc;

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

//    @Test
    public void fun() throws InterruptedException {
        CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println(22234244314L);
                Thread.sleep(3000L);
                System.out.println(224314);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "sa";
        });
        Mono.fromFuture(stringCompletableFuture)
                .timeout(Duration.of(300, ChronoUnit.MILLIS))
                .subscribe(System.out::println);
        latch.await();
    }


}
