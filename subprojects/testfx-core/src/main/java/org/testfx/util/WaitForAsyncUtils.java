/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2015 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;

import com.google.common.base.Stopwatch;
import org.testfx.api.annotation.Unstable;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Provides static methods for handling execution on different threads. The
 * "Test Thread" is usually running on the main thread, while the GUI runs on the
 * "FX Application Thread". Additionally, tasks may also be started on different
 * asynchronous threads.
 * <p>
 * General convention:
 * <ul>
 * <li>{@code async} without a suffix refers to an unknown thread in a ThreadPool
 * <li>the suffix {@code Fx} refers to the FX application thread
 * </ul>
 * <p>
 * <strong>Exception handling</strong>
 * <p>
 * As exceptions on different threads are thrown the caller is usually not aware
 * of these exceptions. Exceptions returned directly from this Framework are wrapped
 * in {@code RuntimeExceptions}.
 * <p>
 * There are two ways this class notifies the user of exceptions:
 * <ul>
 * <li>the returned future
 * <li>the internal exception stack
 * </ul>
 * <p>
 * Usually exceptions are forwarded to the {@code Future} returned by the methods
 * of this class. When the calling thread calls the getter of the Future any
 * exceptions encountered during execution will be thrown. All {@code waitFor} methods
 * acquire the value of the {@code Future} and accordingly throw the same exceptions.
 * <p>
 * The <b>internal exception stack</b> stores all unhandled exceptions thrown during
 * direct calls to the {@code async} methods. As this class can not guarantee that
 * exceptions in these methods are handled properly, it will internally store
 * these exceptions. The exceptions will be in the stack, until they are handled
 * somewhere in the application. If the field {@code autoCheckException} is set to
 * {@literal true}, any subsequent calls to one of the {@code async} methods will
 * throw one of those exceptions.
 */
@Unstable
public class WaitForAsyncUtils {

    // ---------------------------------------------------------------------------------------------
    // CONSTANTS.
    // ---------------------------------------------------------------------------------------------

    private static final long CONDITION_SLEEP_IN_MILLIS = 10;

    private static final long SEMAPHORE_SLEEP_IN_MILLIS = 10;

    private static final int SEMAPHORE_LOOPS_COUNT = 5;

    // ---------------------------------------------------------------------------------------------
    // STATIC FIELDS.
    // ---------------------------------------------------------------------------------------------

    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

    /**
     * If {@literal true} any exceptions encountered during execution of the
     * {@code async} methods will be printed to stderr.
     */
    public static boolean printException = true;

    /**
     * If {@literal true} any call to the {@code async} methods will check for
     * unhandled exceptions.
     */
    public static boolean autoCheckException = true;

    // ---------------------------------------------------------------------------------------------
    // STATIC METHODS.
    // ---------------------------------------------------------------------------------------------

    // ASYNC METHODS.

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param runnable the runnable to run
     * @return the {@code Future} result of the runnable
     */
    public static Future<Void> async(Runnable runnable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<Void> call = new ASyncFXCallable<Void>(runnable, true);
        return executorService.submit(call);
    }

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param runnable the runnable to run
     * @param throwExceptions whether or not to throw exceptions on the executing
     *      thread
     * @return the {@code Future} result of the runnable
     */
    public static Future<Void> async(Runnable runnable, boolean throwExceptions) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<Void> call = new ASyncFXCallable<Void>(runnable, throwExceptions);
        return executorService.submit(call);
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param callable the callable to run
     * @param <T> the return type of the callable
     * @return the {@code Future} result of the callable
     */
    public static <T> Future<T> async(Callable<T> callable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<T> call = new ASyncFXCallable<T>(callable, true);
        executorService.submit((Runnable) call); // exception handling not guaranteed
        return call;
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param callable the callable to run
     * @param throwExceptions whether or not to throw exceptions on the executing
     *      thread
     * @param <T> the return type of the callable
     * @return the {@code Future} result of the callable
     */
    public static <T> Future<T> async(Callable<T> callable, boolean throwExceptions) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<T> call = new ASyncFXCallable<T>(callable, throwExceptions);
        return executorService.submit(call); //exception handling not guaranteed
    }

    /**
     * Runs the given {@link Runnable} on the JavaFX Application Thread at some
     * unspecified time in the future and returns a {@link Future} that is set
     * on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle
     * exceptions after the task has finished.
     *
     * @param runnable the runnable to run
     * @return the {@code Future} result of the runnable
     */
    public static Future<Void> asyncFx(Runnable runnable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<Void> call = new ASyncFXCallable<Void>(runnable, true);
        runOnFxThread(call);
        return call;
    }

    /**
     * Calls the given {@link Callable} on the JavaFX Application Thread at some
     * unspecified time in the future and returns a {@link Future} that is set
     * on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle
     * exceptions after the task has finished.
     *
     * @param callable the callable
     * @param <T> the callable type
     * @return a future
     */
    public static <T> Future<T> asyncFx(Callable<T> callable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<T> call = new ASyncFXCallable<T>(callable, true);
        runOnFxThread(call);
        return call;
    }

    // WAIT-FOR METHODS.

    /**
     * Waits for the given {@link Future} to be set and then returns the
     * future result of type {@code T}.
     *
     * @param future the future to wait for to be set
     * @param <T> the type of the future
     * @return the result of the future
     */
    public static <T> T waitFor(Future<T> future) {
        try {
            return future.get();
        }
        catch (ExecutionException exception) {
            // if the computation threw an exception.
            throw new RuntimeException(exception.getCause());
        }
        catch (InterruptedException ignore) {
            // if the current thread was interrupted while waiting.
            return null;
        }
    }


    /**
     * Waits for given {@link Future} to be set and returns {@code T}, otherwise times out
     * with a {@link TimeoutException}.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param future the future to wait for to be set
     * @param <T> the type of  the future
     * @return the result of the future
     * @throws TimeoutException
     */
    public static <T> T waitFor(long timeout, TimeUnit timeUnit, Future<T> future) throws TimeoutException {
        try {
            return future.get(timeout, timeUnit);
        }
        catch (ExecutionException exception) {
            // if the computation threw an exception.
            throw new RuntimeException(exception.getCause());
        }
        catch (InterruptedException ignore) {
            // if the current thread was interrupted while waiting.
            return null;
        }
    }

    /**
     * Waits for given {@link Callable} to return {@literal true} otherwise times out with
     * a {@link TimeoutException}. The condition will be evaluated at least once. This method
     * will wait for the last condition to finish after a timeout.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param condition the condition to wait for to be {@literal true}
     * @throws TimeoutException
     */
    public static void waitFor(long timeout, TimeUnit timeUnit, Callable<Boolean> condition)
            throws TimeoutException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (!callConditionAndReturnResult(condition)) {
            sleep(CONDITION_SLEEP_IN_MILLIS, MILLISECONDS);
            if (stopwatch.elapsed(timeUnit) > timeout) {
                throw new TimeoutException();
            }
        }
    }

    /**
     * Waits for given {@link ObservableBooleanValue} to return {@literal true} otherwise
     * times out with a {@link TimeoutException}.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param booleanValue the observable
     * @throws TimeoutException
     */
    public static void waitFor(long timeout, TimeUnit timeUnit, ObservableBooleanValue booleanValue)
            throws TimeoutException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
            if (newValue) {
                future.complete(null);
            }
        };
        booleanValue.addListener(changeListener);
        if (!booleanValue.get()) {
            waitFor(timeout, timeUnit, future);
        }
        booleanValue.removeListener(changeListener);
    }

    // WAIT-FOR-FX-EVENTS METHODS.

    /**
     * Waits for the event queue of the JavaFX Application Thread to be completed,
     * as well as any new events triggered in it.
     */
    public static void waitForFxEvents() {
        waitForFxEvents(SEMAPHORE_LOOPS_COUNT);
    }

    /**
     * Waits up to {@code attemptsCount} attempts for the event queue of the
     * JavaFX Application Thread to be completed, as well as any new events
     * triggered on it.
     *
     * @param attemptsCount the number of attempts to try
     */
    public static void waitForFxEvents(int attemptsCount) {
        for (int attempt = 0; attempt < attemptsCount; attempt++) {
            blockFxThreadWithSemaphore();
            sleep(SEMAPHORE_SLEEP_IN_MILLIS, MILLISECONDS);
        }
    }

    // SLEEP METHODS.

    /**
     * Sleeps the given duration.
     *
     * @param duration the duration to sleep
     * @param timeUnit the time unit {@code duration} is in
     */
    public static void sleep(long duration, TimeUnit timeUnit) {
        try {
            sleepWithException(duration, timeUnit);
        }
        catch (InterruptedException ignore) {
        }
    }

    /**
     * Sleeps for the given duration.
     *
     * @param duration the duration to sleep
     * @param timeUnit the time unit {@code duration} is in
     * @throws InterruptedException
     */
    public static void sleepWithException(long duration, TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(duration));
    }

    // WAIT-FOR-ASYNC METHODS.

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and waits {@code millis}
     * milliseconds for it to finish, otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param runnable the runnable to run
     */
    public static void waitForAsync(long millis, Runnable runnable) {
        // exceptions handled in wait are safe
        Future<Void> future = async(runnable, false);
        waitForMillis(millis, future);
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and waits {@code millis}
     * milliseconds for it to finish. If finished, returns the future result of type
     * {@code T}, otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param callable the callable to call
     * @param <T> the type returned by the callable
     * @return the result returned by the callable
     */
    public static <T> T waitForAsync(long millis, Callable<T> callable) {
        Future<T> future = async(callable, false); //exceptions handled in wait --> safe
        return waitForMillis(millis, future);
    }

    /**
     * Runs the given {@link Runnable} on the JavaFX Application Thread at some unspecified time
     * in the future and waits {@code millis} milliseconds for it to finish, otherwise times out with
     * a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param runnable the runnable to run
     */
    public static void waitForAsyncFx(long millis, Runnable runnable) {
        Future<Void> future = asyncFx(runnable);
        waitForMillis(millis, future);
    }

    /**
     * Calls the given {@link Callable} on the JavaFX Application Thread at some unspecified time
     * in the future and waits {@code millis} milliseconds for it to finish. If finished, returns
     * {@code T} otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param callable the callable to call
     * @param <T> the type returned by the callable
     * @return the result returned by the callable
     */
    public static <T> T waitForAsyncFx(long millis, Callable<T> callable) {
        Future<T> future = asyncFx(callable);
        return waitForMillis(millis, future);
    }


    /**
     * Checks if a exception in a async task occurred, that has not been checked currently.
     * If so, the first exception will be removed and thrown by this method.
     *
     * @throws Throwable the exception to check
     */
    public static void checkException() throws Throwable {
        Throwable throwable = getCheckException();
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Prints unhandled exceptions to stderr. The printed exceptions are not removed
     * from the stack.
     */
    public static void printExceptions() {
        for (int i = 0; i < exceptions.size(); i++) {
            System.err.println("ExceptionQueue: " + i);
            exceptions.get(i);
        }
    }

    /**
     * Clears all unhandled exceptions.
     */
    public static void clearExceptions() {
        exceptions.clear();
    }

    // ---------------------------------------------------------------------------------------------
    // PRIVATE STATIC METHODS.
    // ---------------------------------------------------------------------------------------------

    /**
     * Internal function that allows throws Exceptions. It does not require handling
     * of the Exceptions.
     */
    private static void checkExceptionWrapped() {
        Throwable throwable = getCheckException();
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Pops an exception from the stack and adds an entry in the stack trace
     * to notify the user that this is not the original place of the exception.
     *
     * @return the exception or {@literal null} if none in stack
     */
    private static Throwable getCheckException() {
        if (exceptions.size() > 0) {
            Throwable throwable = exceptions.get(0);
            StackTraceElement stackTraceElement = new StackTraceElement(WaitForAsyncUtils.class.getName(),
                    "---- Delayed Exception: (See Trace Below) ----",
                    WaitForAsyncUtils.class.getSimpleName() + ".java", 0);
            StackTraceElement[] stackTrace = new StackTraceElement[1];
            stackTrace[0] = stackTraceElement;
            throwable.setStackTrace(stackTrace);
            exceptions.remove(0);
            return throwable;
        }
        return null;
    }

    private static <T> T waitForMillis(long millis, Future<T> future) {
        try {
            // exceptions are thrown on current thread
            return waitFor(millis, MILLISECONDS, future);
        }
        catch (TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static boolean callConditionAndReturnResult(Callable<Boolean> condition) {
        try {
            return condition.call();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void blockFxThreadWithSemaphore() {
        Semaphore semaphore = new Semaphore(0);
        runOnFxThread(semaphore::release);
        try {
            semaphore.acquire();
        }
        catch (InterruptedException ignore) {
        }
    }

    /**
     * Internally used {@code Callable} that handles all the async stuff. All external
     * Callables/Runnables must be wrapped in this class.
     * <p>
     * <em>Note:</em> This is a single call object. Do not use twice!
     *
     * @param <X> the return type of the callable
     */
    private static class ASyncFXCallable<X> extends FutureTask<X> implements Callable<X> {

        /**
         * If {@literal true}, exceptions will be added to the internal stack.
         */
        private final boolean throwException;

        /**
         * Holds the stacktrace of the caller, for printing, if an Exception occurs.
         */
        private final StackTraceElement[] trace;
        private boolean running;

        /**
         * The unhandled exception.
         */
        private Throwable exception;

        public ASyncFXCallable(Runnable runnable, boolean throwException) {
            super(runnable, (X) null);
            this.throwException = throwException;
            trace = Thread.currentThread().getStackTrace();
        }

        public ASyncFXCallable(Callable<X> callable, boolean throwException) {
            super(callable);
            this.throwException = throwException;
            trace = Thread.currentThread().getStackTrace();
        }

        /**
         * Runs the task and evaluates exceptions encountered during execution.
         * Exceptions are printed and pushed on to the stack.
         */
        @Override
        public void run() {
            running = true;
            super.run();
            try {
                get();
            }
            catch (Exception e) {
                if (throwException) {
                    if (printException) {
                        String out = "--- Exception in Async Thread ---\n";
                        out += printException(e);
                        StackTraceElement[] st = e.getStackTrace();
                        out += printTrace(st);
                        Throwable cause = e.getCause();
                        while (cause != null) {
                            out += printException(cause);
                            st = cause.getStackTrace();
                            out += printTrace(st);
                            cause = cause.getCause();
                        }
                        out += "--- Trace of caller of unhandled Exception in Async Thread ---\n";
                        out += printTrace(trace);
                        System.err.println(out);
                    }
                    exception = transformException(e);
                    // Add exception to list of occured exceptions
                    exceptions.add(exception);
                }
                running = false;
                if (!Platform.isFxApplicationThread()) {
                    Throwable ex = transformException(e);
                    throwException(ex);
                }
            }
        }

        /**
         * Throws a transformed exception.
         *
         * @param exception the exception to throw
         */
        protected void throwException(Throwable exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof Error) {
                throw (Error) exception;
            }
        }

        /**
         * Transforms a exception to be throwable. Basically wraps the exception
         * in a RuntimeException, if it is not already one.
         *
         * @param exception the exception to transform
         * @return the throwable exception
         */
        protected Throwable transformException(Throwable exception) {
            // unwind one ExecutionException
            if (exception instanceof ExecutionException) {
                exception = exception.getCause();
            }
            if (exception instanceof RuntimeException) {
                return (RuntimeException) exception;
            }
            if (exception instanceof Error) {
                return (Error) exception;
            } else {
                return new RuntimeException(exception);
            }
        }

        @Override
        public X call() throws Exception {
            run();
            return get();
        }

        @Override
        public X get() throws InterruptedException, ExecutionException {
            try {
                return super.get();
            }
            catch (Exception e) {
                if ((!running) && (exception != null)) {
                    exceptions.remove(exception);
                    exception = null;
                }
                throw e;
            }
        }

        @Override
        public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return super.get(timeout, unit);
            }
            catch (Exception e) {
                if ((!running) && (exception != null)) {
                    exceptions.remove(exception);
                    exception = null;
                }
                throw e;
            }
        }

        /**
         * Get a string for the exception header.
         *
         * @param e the exception
         * @return the line containing the header
         */
        protected String printException(Throwable e) {
            return e.getClass().getName() + ": " + e.getMessage() + "\n";
        }

        /**
         * Returns a {@code String} containing the printed stacktrace.
         *
         * @param st the stacktrace
         * @return a {@code String} containing the printed stacktrace
         */
        protected String printTrace(StackTraceElement[] st) {
            String ret = "";
            for (int i = 0; i < st.length; i++) {
                ret += "\t" + st[i].toString() + "\n";
            }
            return ret;
        }
    }

}
