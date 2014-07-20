package com.mindfulst.dneves.pomotivity;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class responsible for communicating with the underlying pomodoro logic.
 * <p/>
 * This serves as a layer between the UI on the various devices and the logic.
 * P.s. Also a nice way for me to hack the whole thing here and move to the right place later ;)
 */
public class PomodoroApi {
  public class AlreadyRunningException extends Exception {}

  private static final String DEBUG_TAG         = "pomoapi";
  // 25 mins in seconds
  private static final int    POMODORO_DURATION = 25 * 60;

  private final ScheduledExecutorService mExecutionService =
      Executors.newSingleThreadScheduledExecutor();

  private AtomicReference<ScheduledFuture> mCurrentPomodoro =
      new AtomicReference<ScheduledFuture>();
  private boolean                          mIsPaused        = false;

  /**
   * Starts the pomodoro timer.
   * <p/>
   * This method isn't thread-safe and assumes you always call it from the same thread.
   *
   * @throws com.mindfulst.dneves.pomotivity.PomodoroApi.AlreadyRunningException if you call this
   *                                                                             method while a
   *                                                                             pomodoro is
   *                                                                             running.
   */
  public void start() throws AlreadyRunningException {
    final Runnable pomodoroTick = new Runnable() {
      private int mCurrentTime = POMODORO_DURATION;
      private final long mStartTime = System.nanoTime();

      @Override
      public void run() {
        if (mIsPaused) {
          return;
        }

        --mCurrentTime;
        if (mCurrentTime <= 0) {
          Log.d(DEBUG_TAG, "Timer ended after " + ((System.nanoTime() - mStartTime) * 1e-9));
          stop();
        }
        else {
          Log.d(DEBUG_TAG, "Timer: " + mCurrentTime);
        }
      }
    };

    if (!mCurrentPomodoro.compareAndSet(null, mExecutionService
        .scheduleAtFixedRate(pomodoroTick, 1, 1, TimeUnit.SECONDS))) {
      throw new AlreadyRunningException();
    }
    else {
      // Set it here because there might be a paused timer already and we don't want to reset its
      // state do we? We have a second to do it ;) no need to panic
      mIsPaused = false;
      Log.i(DEBUG_TAG, "Timer started");
    }
  }

  /**
   * Stops the current timer or does nothing if no timer is running.
   */
  protected void stop() {
    ScheduledFuture pomodoro = mCurrentPomodoro.getAndSet(null);
    if (pomodoro != null) {
      pomodoro.cancel(false);
      Log.i(DEBUG_TAG, "Timer stopped");
    }
  }

  /**
   * Pauses the current timer or does nothing if no timer is running.
   */
  protected void pause() {
    mIsPaused = true;
    Log.i(DEBUG_TAG, "Timer paused");
  }

  /**
   * Resumes the current timer or does nothing if no timer is running.
   */
  protected void resume() {
    Log.i(DEBUG_TAG, "Timer resumed");
    // This means that we may have to wait almost a second before the next run, but it's a simple
    // mechanism ;)
    mIsPaused = false;
  }
}
