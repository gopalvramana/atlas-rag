package com.atlas.retrieval;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wraps a virtual-thread executor with a semaphore to bound admission.
 * Virtual threads handle I/O blocking cheaply; the semaphore prevents
 * unbounded concurrent DB connections under high request load.
 */
class BoundedVirtualExecutor implements ExecutorService {

    private final ExecutorService delegate;
    private final Semaphore semaphore;

    BoundedVirtualExecutor(ExecutorService delegate, Semaphore semaphore) {
        this.delegate = delegate;
        this.semaphore = semaphore;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> {
            semaphore.acquireUninterruptibly();
            try {
                command.run();
            } finally {
                semaphore.release();
            }
        });
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(() -> {
            semaphore.acquireUninterruptibly();
            try {
                return task.call();
            } finally {
                semaphore.release();
            }
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(() -> {
            semaphore.acquireUninterruptibly();
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        }, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(() -> {
            semaphore.acquireUninterruptibly();
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        });
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }
}
