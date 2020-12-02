package com.sohu.smc.md.cache.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/2
 */
public class CompletionStageUtils {

    public static <T> CompletionStage<T> wrap(Callable<CompletionStage<T>> callable){
        try {
            return callable.call();
        } catch (Exception e){
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

}
