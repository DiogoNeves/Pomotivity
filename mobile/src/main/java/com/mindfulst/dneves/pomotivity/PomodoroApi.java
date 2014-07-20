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
  private static final int    POMODORO_DURATION = 25;

  private static PomodoroApi mInstance = null;

  private final ScheduledExecutorService         mExecutionService = Executors.newSingleThreadScheduledExecutor();
  private       AtomicReference<ScheduledFuture> mCurrentPomodoro  = new AtomicReference<ScheduledFuture>();

  private boolean mIsPaused  = false;
  private boolean mAutoStart = false;

  public static PomodoroApi getInstance() {
    if (mInstance == null) {
      mInstance = new PomodoroApi();
    }
    return mInstance;
  }

  private PomodoroApi() {}

  /**
   * Starts the pomodoro timer.
   *
   * @throws com.mindfulst.dneves.pomotivity.PomodoroApi.AlreadyRunningException if a pomodoro is already running.
   */
  public synchronized void start() throws AlreadyRunningException {
    // We don't care if it stops after this point, only that you called it while it was logically
    // running. Also, it is synchronized so, if it is null, there's no way it'll get another value
    // after this point ;)
    if (mCurrentPomodoro.get() != null) {
      throw new AlreadyRunningException();
    }

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
          if (mAutoStart) {
            try {
              start();
            } catch (AlreadyRunningException e) {
              Log.w(DEBUG_TAG, "It failed to auto-start because it was already running, but I just stopped...");
            }
          }
        }
        else {
          Log.d(DEBUG_TAG, "Timer: " + mCurrentTime);
        }
      }
    };

    Log.i(DEBUG_TAG, "Time started");
    mIsPaused = false;
    mCurrentPomodoro.set(mExecutionService.scheduleAtFixedRate(pomodoroTick, 1, 1, TimeUnit.SECONDS));
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

  /**
   * Sets the auto start flag.
   * If set to true, when a pomodoro finishes, another one will start immediately.
   *
   * @param autoStart new value.
   */
  protected void setAutoStart(boolean autoStart) {
    mAutoStart = autoStart;
  }
}
