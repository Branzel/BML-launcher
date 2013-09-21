package net.branzel.launcher.updater;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.branzel.launcher.Launcher;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor {
    public ExceptionalThreadPoolExecutor(int threadCount) {
        super(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if ((t == null) && ((r instanceof Future)))
            try {
                Future future = (Future)r;
                if (future.isDone())
                    future.get();
            } catch (CancellationException | ExecutionException ce) {
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
    {
        return new ExceptionalFutureTask(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
    {
        return new ExceptionalFutureTask(callable);
    }

    public class ExceptionalFutureTask<T> extends FutureTask<T> {
        public ExceptionalFutureTask(Callable callable) {
            super(callable);
        }

        public ExceptionalFutureTask(Runnable runnable, T result) {
            super(runnable, result);
        }

        @Override
        protected void done()
        {
            try {
                get();
            } catch (InterruptedException | ExecutionException t) {
                Launcher.getInstance().println("Unhandled exception in executor " + this, t);
            }
        }
    }
}
