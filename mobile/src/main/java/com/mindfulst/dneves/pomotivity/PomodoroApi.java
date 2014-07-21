package com.mindfulst.dneves.pomotivity;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class responsible for communicating with the underlying pomodoro logic.
 * <p/>
 * This serves as a layer between the UI on the various devices and the logic.
 * Notifications, sounds, UI and update of third party services (e.g. sharing) are outside the scope of the API.
 * State, core logic and methods are the scope of this API.
 * <p/>
 * P.s. Also a nice way for me to hack the whole thing here and move to the right place later ;)
 */
public class PomodoroApi {
  public class AlreadyRunningException extends Exception {}

  /**
   * Class responsible for keeping simple stats state.
   */
  public static final class Stats {
    public final int finishedToday;
    public final int allTime;
    public final int totalDays;

    protected Stats() {
      finishedToday = 0;
      allTime = 0;
      totalDays = 0;
    }

    protected Stats(int finishedToday, int allTime, int totalDays) {
      this.finishedToday = finishedToday;
      this.allTime = allTime;
      this.totalDays = totalDays;
    }

    /**
     * Increments both finished today and all time.
     *
     * @param last last known Stats.
     * @return new Stats instance with the incremented counters.
     */
    protected static Stats incrementCounter(final Stats last) {
      return new Stats(last.finishedToday + 1, last.allTime + 1, last.totalDays);
    }

    /**
     * Increments the day counter.
     * This will reset the finishedToday value.
     *
     * @param last last known Stats.
     * @return new Stats instance with the information about the next day.
     */
    protected static Stats nextDay(final Stats last) {
      return new Stats(0, last.allTime, last.totalDays + 1);
    }

    @Override
    public String toString() {
      return String
          .format("PomodoroApi.Stats(finishedToday:%d, allTime:%d, totalDays:%d)", finishedToday, allTime, totalDays);
    }
  }

  private static final String DEBUG_TAG         = "pomoapi";
  // 25 mins in seconds
  private static final int    POMODORO_DURATION = 25 * 60;

  private static PomodoroApi mInstance = null;

  private final ScheduledExecutorService         mExecutionService = Executors.newSingleThreadScheduledExecutor();
  private       AtomicReference<ScheduledFuture> mCurrentPomodoro  = new AtomicReference<ScheduledFuture>();

  private boolean mIsPaused  = false;
  private boolean mAutoStart = false;

  private Stats    mStats            = new Stats();
  private DateTime mLastPomodoroDate = new DateTime(0).withTime(4, 0, 0, 0);

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
          incrementStats();
          if (mAutoStart) {
            try {
              start();
            }
            catch (AlreadyRunningException e) {
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

  private void incrementStats() {
    mStats = Stats.incrementCounter(mStats);
    // We consider the start of the day at 4am as this should be the least convenient time to use pomodoros
    // see https://www.ted.com/talks/rives_on_4_a_m
    DateTime now = DateTime.now().withTime(4, 0, 0, 0);
    if (Days.daysBetween(now, mLastPomodoroDate).getDays() != 0) {
      mStats = Stats.nextDay(mStats);
      mLastPomodoroDate = now;
    }
    Log.d(DEBUG_TAG, "Current stats: " + mStats);
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
