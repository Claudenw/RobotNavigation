package org.xenei.robot.common.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class ChainedFuture<T> implements Future<T> {
    private final Future<?> future;
    private final Supplier<T> supplier;

    public ChainedFuture(Future<?> future, T value) {
        this(future, () -> value);
    }

    public ChainedFuture(Future<?> future, Supplier<T> supplier) {
        this.future = future;
        this.supplier = supplier;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        future.get();
        return supplier.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        future.get(timeout, unit);
        return supplier.get();
    }
}
