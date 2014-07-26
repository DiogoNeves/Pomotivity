package com.mindfulst.dneves.pomotivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.EventListener;
import java.util.EventObject;
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

  public final class PomodoroEvent extends EventObject {
    public final int           currentTime;
    public final PomodoroState currentState;

    /**
     * Constructor.
     *
     * @param source       PomodoroApi that triggered the event.
     * @param currentTime  Milliseconds since the current state started (break or pomodoro).
     * @param currentState Current state of execution.
     */
    protected PomodoroEvent(Object source, int currentTime, PomodoroState currentState) {
      super(source);
      this.currentTime = currentTime;
      this.currentState = currentState;
    }
  }

  /**
   * Interface to be implemented by all listeners of pomodoro actions.
   * <p/>
   * On a typical pomodoro the order of the events are:
   */
  public interface PomodoroEventListener extends EventListener {
    /**
     * Action triggered when a new Pomodoro starts.
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroStarted(final PomodoroEvent event);

    /**
     * Action triggered every time the clock ticks.
     * WARNING: Not guaranteed to be called from the UI thread!
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroTicked(final PomodoroEvent event);

    /**
     * Action triggered when the Pomodoro part ends (before the break).
     * WARNING: Not guaranteed to be called from the UI thread!
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroEnded(final PomodoroEvent event);

    /**
     * Action triggered when the Break part starts.
     * Same for short and long break. You can get the current state from the event object.
     * WARNING: Not guaranteed to be called from the UI thread!
     *
     * @param event Information about the Pomodoro Event.
     */
    public void breakStarted(final PomodoroEvent event);

    /**
     * Action triggered when the current Pomodoro finishes.
     * WARNING: Not guaranteed to be called from the UI thread!
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroFinished(final PomodoroEvent event);
  }

  /**
   * Enum with actions that can be notified to listeners.
   */
  private enum ListenerAction {
    START, TICK, END_POMODORO, START_BREAK, FINISH
  }

  /**
   * Enum with possible internal Pomodoro states.
   */
  public enum PomodoroState {
    NONE, POMODORO, SHORT_BREAK, LONG_BREAK
  }

  /**
   * Class responsible for keeping simple stats state.
   */
  public static final class Stats {
    public final int finishedToday;
    public final int allTime;
    public final int totalDays;

    /**
     * Default constructor.
     */
    protected Stats() {
      finishedToday = 0;
      allTime = 0;
      totalDays = 0;
    }

    /**
     * Attribute constructor.
     *
     * @param finishedToday Today's counter.
     * @param allTime       All time pomodoro counter.
     * @param totalDays     Total days with Pomodoros.
     */
    protected Stats(int finishedToday, int allTime, int totalDays) {
      this.finishedToday = finishedToday;
      this.allTime = allTime;
      this.totalDays = totalDays;
    }

    /**
     * Constructor that loads the initial values from the preferences.
     *
     * @param context     Context where to get the attribute keys from.
     * @param preferences Preferences to load the attributes from.
     */
    protected Stats(Context context, SharedPreferences preferences) {
      this.finishedToday = preferences.getInt(context.getString(R.string.finished_today_key), 0);
      this.allTime = preferences.getInt(context.getString(R.string.all_time_key), 0);
      this.totalDays = preferences.getInt(context.getString(R.string.total_days_key), 0);
    }

    /**
     * Resets the today counter but keeps the other counters intact.
     *
     * @return new Stats instance with reset today counter.
     */
    protected Stats resetToday() {
      return new Stats(0, allTime, totalDays);
    }

    /**
     * Increments both finished today and all time.
     *
     * @return new Stats instance with the incremented counters.
     */
    protected Stats incrementCounter() {
      return new Stats(finishedToday + 1, allTime + 1, totalDays);
    }

    /**
     * Increments the day counter.
     * This will reset the finishedToday value.
     *
     * @return new Stats instance with the information about the next day.
     */
    protected Stats nextDay() {
      return new Stats(0, allTime, totalDays + 1);
    }

    @Override
    public String toString() {
      return String
          .format("PomodoroApi.Stats(finishedToday:%d, allTime:%d, totalDays:%d)", finishedToday, allTime, totalDays);
    }

    /**
     * Saves all the Stats attributes but doesn't call apply() or commit().
     *
     * @param context    Context where to get the attribute keys from.
     * @param prefEditor Editor used to save the state.
     */
    protected void save(Context context, SharedPreferences.Editor prefEditor) {
      prefEditor.putInt(context.getString(R.string.finished_today_key), finishedToday)
                .putInt(context.getString(R.string.all_time_key), allTime)
                .putInt(context.getString(R.string.total_days_key), totalDays);
    }
  }

  private static final String DEBUG_TAG            = "pomoapi";
  /**/
  private static final int    POMODORO_DURATION    = 25 * 60;
  private static final int    SHORT_BREAK_DURATION = 5 * 60;
  private static final int    LONG_BREAK_DURATION  = 14 * 60;
  /*/
  // Test times (for debugging)
  private static final int    POMODORO_DURATION = 6;
  private static final int    SHORT_BREAK_DURATION = 3;
  private static final int    LONG_BREAK_DURATION = 4;
  /**/

  private static PomodoroApi mInstance = null;

  private PomodoroEventListener mListener = null;

  private final ScheduledExecutorService         mExecutionService = Executors.newSingleThreadScheduledExecutor();
  private       AtomicReference<ScheduledFuture> mCurrentPomodoro  = new AtomicReference<ScheduledFuture>();

  private boolean mIsPaused  = false;
  private boolean mAutoStart = false;

  private Stats         mStats            = new Stats();
  private DateTime      mLastPomodoroDate = new DateTime(0).withTime(4, 0, 0, 0);
  private PomodoroState mCurrentState     = PomodoroState.NONE;

  public static PomodoroApi getInstance() {
    if (mInstance == null) {
      mInstance = new PomodoroApi();
    }
    return mInstance;
  }

  private PomodoroApi() {}

  public Stats getStats() {
    return mStats;
  }

  public void save(Context context, SharedPreferences.Editor prefEditor) {
    DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    prefEditor.putString(context.getString(R.string.last_pomodoro_key), formatter.print(mLastPomodoroDate));
    mStats.save(context, prefEditor);
  }

  /**
   * Loads the state from the given preferences.
   *
   * @param context     Context where to get the attribute keys from.
   * @param preferences Preferences to load the state from.
   */
  public void load(Context context, SharedPreferences preferences) {
    mStats = new Stats(context, preferences);
    DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    final String defaultDate = formatter.print(new DateTime(0).withTime(4, 0, 0, 0));
    String lastPomodoroStr = preferences.getString(context.getString(R.string.last_pomodoro_key), defaultDate);
    try {
      mLastPomodoroDate = formatter.parseDateTime(lastPomodoroStr);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    // We don't change the date because we only only to count another day when a pomodoro finishes
    // This is just to avoid displaying today's pomodoros if a few days have passed since last running the app
    DateTime now = DateTime.now().withTime(4, 0, 0, 0);
    if (Days.daysBetween(now, mLastPomodoroDate).getDays() != 0) {
      mStats = mStats.resetToday();
    }
  }

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

      private void endPomodoro() {
        incrementStats();
        notifyListener(ListenerAction.END_POMODORO, 0, mCurrentState);
        if (mStats.finishedToday % 4 == 0) {
          mCurrentState = PomodoroState.LONG_BREAK;
          mCurrentTime = LONG_BREAK_DURATION;
        }
        else {
          mCurrentState = PomodoroState.SHORT_BREAK;
          mCurrentTime = SHORT_BREAK_DURATION;
        }
        notifyListener(ListenerAction.START_BREAK, mCurrentTime, mCurrentState);
      }

      private void endBreak() {
        stop();
        notifyListener(ListenerAction.FINISH, 0, mCurrentState);
        mCurrentState = PomodoroState.NONE;
        if (mAutoStart) {
          try {
            // Force the UI to catch up and and give time to breath. We only need to do it here because of the
            // autostart, which will trigger another notification update immediately.
            Thread.sleep(1000);
            start();
          }
          catch (AlreadyRunningException e) {
            Log.w(DEBUG_TAG, "It failed to auto-start because it was already running, but I just stopped...");
          }
          catch (InterruptedException e) {
            Log.w(DEBUG_TAG, "Ooops, thread was interruped");
          }
        }
      }

      @Override
      public void run() {
        if (mIsPaused) {
          return;
        }

        --mCurrentTime;
        if (mCurrentTime <= 0) {
          if (mCurrentState == PomodoroState.POMODORO) {
            Log.d(DEBUG_TAG, "Pomodoro ended after " + ((System.nanoTime() - mStartTime) * 1e-9));
            endPomodoro();
          }
          else { // LONG or SHORT break
            Log.d(DEBUG_TAG, "Pomodoro and break ended after " + ((System.nanoTime() - mStartTime) * 1e-9));
            endBreak();
          }
        }
        else {
          Log.d(DEBUG_TAG, "Timer: " + mCurrentTime);
          notifyListener(ListenerAction.TICK, mCurrentTime, mCurrentState);
        }
      }
    };

    Log.i(DEBUG_TAG, "Pomodoro started");
    mIsPaused = false;
    mCurrentState = PomodoroState.POMODORO;
    mCurrentPomodoro.set(mExecutionService.scheduleAtFixedRate(pomodoroTick, 1, 1, TimeUnit.SECONDS));
    notifyListener(ListenerAction.START, POMODORO_DURATION, mCurrentState);
  }

  private void notifyListener(ListenerAction action, int currentTime, PomodoroState state) {
    PomodoroEventListener listener = mListener;
    if (listener == null) {
      return;
    }

    try {
      // I used actions because creating and passing callables for something so static isn't convenient ;)
      PomodoroEvent event = new PomodoroEvent(this, currentTime, state);
      switch (action) {
        case START:
          mListener.pomodoroStarted(event);
          break;
        case TICK:
          mListener.pomodoroTicked(event);
          break;
        case END_POMODORO:
          mListener.pomodoroEnded(event);
          break;
        case START_BREAK:
          mListener.breakStarted(event);
          break;
        case FINISH:
          mListener.pomodoroFinished(event);
          break;
      }
    }
    catch (Exception e) {
      Log.d(DEBUG_TAG, "Exception thrown while calling the listener: " + e.toString());
    }
  }

  private void incrementStats() {
    // We consider the start of the day at 4am as this should be the least convenient time to use pomodoros
    // see https://www.ted.com/talks/rives_on_4_a_m
    DateTime now = DateTime.now().withTime(4, 0, 0, 0);
    if (Days.daysBetween(now, mLastPomodoroDate).getDays() != 0) {
      mStats = mStats.nextDay();
      mLastPomodoroDate = now;
    }
    // Do this after the next day because it will reset the today counter
    mStats = mStats.incrementCounter();
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

  public void setPomodoroListener(PomodoroEventListener listener) {
    mListener = listener;
  }
}
