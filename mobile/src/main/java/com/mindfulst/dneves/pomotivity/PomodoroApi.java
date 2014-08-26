package com.mindfulst.dneves.pomotivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    public final boolean       autoStart;
    public final PomodoroState currentState;

    /**
     * Constructor.
     *
     * @param source       PomodoroApi that triggered the event.
     * @param currentTime  Milliseconds since the current state started (break or pomodoro).
     * @param currentState Current state of execution.
     */
    protected PomodoroEvent(Object source, int currentTime, boolean autoStart, PomodoroState currentState) {
      super(source);
      this.currentTime = currentTime;
      this.autoStart = autoStart;
      this.currentState = currentState;
    }
  }

  /**
   * Interface to be implemented by all listeners of pomodoro actions.
   * WARNING: Not guaranteed to be called from the UI thread!
   * <p/>
   * On a typical pomodoro the order of the events are:
   * pomodoroStarted - start() was called (could be user or auto-start)
   * pomodoroTicked - this is called every second, including breaks, so the app can update the UI
   * pomodoroEnded - this is called when the pomodoro part (before break) ends
   * breakStarted - this is called immediately after the break part starts
   * pomodoroFinished - this is called when both the pomodoro and break end or the stop() was called
   * the best way to know if it was due to an early stop is to check if (event.currentTime > 0)
   * <p/>
   * At any point in time, while pomodoros are running:
   * paused - called when pause() is called
   * resumed - called when resume() is called
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
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroTicked(final PomodoroEvent event);

    /**
     * Action triggered when the Pomodoro part ends (before the break).
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroEnded(final PomodoroEvent event);

    /**
     * Action triggered when the Break part starts.
     * Same for short and long break. You can get the current state from the event object.
     *
     * @param event Information about the Pomodoro Event.
     */
    public void breakStarted(final PomodoroEvent event);

    /**
     * Action triggered when the current Pomodoro finishes (could be early termination via stop()).
     *
     * @param event Information about the Pomodoro Event.
     */
    public void pomodoroFinished(final PomodoroEvent event);

    /**
     * Action triggered when pause() is called.
     *
     * @param event Information about the Pomodoro Event.
     */
    public void paused(final PomodoroEvent event);

    /**
     * Action triggered when resume() is called.
     *
     * @param event Information about the Pomodoro Event.
     */
    public void resumed(final PomodoroEvent event);
  }

  /**
   * Enum with actions that can be notified to listeners.
   */
  private enum ListenerAction {
    START, TICK, END_POMODORO, START_BREAK, FINISH, PAUSED, RESUMED
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
    public final  int                  finishedToday;
    public final  int                  allTime;
    public final  int                  totalDays;
    private final Map<String, Integer> mProjectMap;

    /**
     * Default constructor.
     */
    protected Stats() {
      finishedToday = 0;
      allTime = 0;
      totalDays = 0;
      // For simplicity always create the map even though it can't get changed
      mProjectMap = new HashMap<String, Integer>();
    }

    /**
     * Attribute constructor.
     *
     * @param finishedToday Today's counter.
     * @param allTime       All time pomodoro counter.
     * @param totalDays     Total days with Pomodoros.
     * @param projectMap    Map of current projects with their individual pomodoro counters.
     */
    protected Stats(int finishedToday, int allTime, int totalDays, Map<String, Integer> projectMap) {
      this.finishedToday = finishedToday;
      this.allTime = allTime;
      this.totalDays = totalDays;
      // Don't copy because this is accessed only from this class and it was mutated already before calling this
      this.mProjectMap = projectMap;
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
      this.mProjectMap = parseProjectMap(preferences.getStringSet(context.getString(R.string.projects), null));
    }

    /**
     * Resets the today counter but keeps the other counters intact.
     *
     * @return new Stats instance with reset today counter.
     */
    protected Stats resetToday() {
      return new Stats(0, allTime, totalDays, mProjectMap);
    }

    /**
     * Adds a project name to the map.
     *
     * @param project Project name to add.
     * @return A new stats object.
     */
    protected Stats addProject(String project) {
      if (project == null || project.isEmpty()) {
        return this;
      }

      // We need to copy because some code might use the previous Stats objects which should be immutable
      HashMap<String, Integer> newProjectsMap = new HashMap<String, Integer>(mProjectMap);
      newProjectsMap.put(project, newProjectsMap.containsKey(project) ? newProjectsMap.get(project) : 0);
      return new Stats(finishedToday, allTime, totalDays, newProjectsMap);
    }

    /**
     * Increments both finished today and all time.
     *
     * @param currentProject Name of the current active project. This will increment its counter too.
     * @return new Stats instance with the incremented counters.
     */
    protected Stats incrementCounter(String currentProject) {
      Map<String, Integer> newProjectsMap = mProjectMap;
      // If we have a current project we'll have to change it...
      if (currentProject != null && !currentProject.isEmpty()) {
        // We need to copy because some code might use the previous Stats objects which should be immutable
        newProjectsMap = new HashMap<String, Integer>(mProjectMap);
        newProjectsMap.put(currentProject,
                           newProjectsMap.containsKey(currentProject) ? newProjectsMap.get(currentProject) + 1 : 1);
      }
      return new Stats(finishedToday + 1, allTime + 1, totalDays, newProjectsMap);
    }

    /**
     * Increments the day counter.
     * This will reset the finishedToday value.
     *
     * @return new Stats instance with the information about the next day.
     */
    protected Stats nextDay() {
      return new Stats(0, allTime, totalDays + 1, mProjectMap);
    }

    @Override
    public String toString() {
      return String
          .format("PomodoroApi.Stats(finishedToday:%d, allTime:%d, totalDays:%d, totalProjects:%d)", finishedToday,
                  allTime, totalDays, mProjectMap.size());
    }

    public Map<String, Integer> getProjects() {
      return new HashMap<String, Integer>(mProjectMap);
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
                .putInt(context.getString(R.string.total_days_key), totalDays)
                .putStringSet(context.getString(R.string.projects), getProjectMapAsSet());
    }

    private Set<String> getProjectMapAsSet() {
      Set<String> projectSet = new HashSet<String>(mProjectMap.size());
      for (Map.Entry<String, Integer> projectInfo : mProjectMap.entrySet()) {
        projectSet.add(String.format("%s,%d", projectInfo.getKey(), projectInfo.getValue()));
      }
      return projectSet;
    }

    private static Map<String, Integer> parseProjectMap(Set<String> projectSet) {
      if (projectSet == null) {
        return new HashMap<String, Integer>();
      }

      HashMap<String, Integer> projectMap = new HashMap<String, Integer>(projectSet.size());
      for (String projectInfo : projectSet) {
        String[] projectValues = projectInfo.split(",");
        projectMap.put(projectValues[0], Integer.parseInt(projectValues[1]));
      }
      return projectMap;
    }
  }

  private static final String DEBUG_TAG            = "pomoapi";
  /**/
  public static final  int    POMODORO_DURATION    = 25 * 60;
  public static final  int    SHORT_BREAK_DURATION = 5 * 60;
  public static final  int    LONG_BREAK_DURATION  = 14 * 60;
  /*/
  // Test times (for debugging)
  public static final  int    POMODORO_DURATION    = 6;
  public static final  int    SHORT_BREAK_DURATION = 3;
  public static final  int    LONG_BREAK_DURATION  = 4;
  /**/

  private static PomodoroApi mInstance = new PomodoroApi();

  private PomodoroEventListener mListener = null;

  private final ScheduledExecutorService mExecutionService = Executors.newSingleThreadScheduledExecutor();

  private AtomicReference<ScheduledFuture> mCurrentPomodoro = new AtomicReference<ScheduledFuture>();

  private boolean mIsPaused  = false;
  private boolean mAutoStart = false;

  private       Stats                          mStats            = new Stats();
  private       DateTime                       mLastPomodoroDate = new DateTime(0).withTime(4, 0, 0, 0);
  private final AtomicReference<PomodoroState> mCurrentState     =
      new AtomicReference<PomodoroState>(PomodoroState.NONE);
  private final AtomicInteger                  mCurrentTime      = new AtomicInteger(POMODORO_DURATION);
  private final AtomicReference<String>        mCurrentProject   = new AtomicReference<String>("");

  public static PomodoroApi getInstance() {
    return mInstance;
  }

  private PomodoroApi() {}

  public Stats getStats() {
    return mStats;
  }

  public void save(Context context, SharedPreferences.Editor prefEditor) {
    DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    prefEditor.putBoolean(context.getString(R.string.auto_start_key), mAutoStart);
    prefEditor.putString(context.getString(R.string.last_pomodoro_key), formatter.print(mLastPomodoroDate));
    prefEditor.putString(context.getString(R.string.current_project), mCurrentProject.get());
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
    mAutoStart = preferences.getBoolean(context.getString(R.string.auto_start_key), false);
    mCurrentProject.set(preferences.getString(context.getString(R.string.current_project), ""));

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
   * @throws com.mindfulst.dneves.pomotivity.PomodoroApi.AlreadyRunningException if you call this
   *                                                                             method while a
   *                                                                             pomodoro is
   *                                                                             running.
   */
  public synchronized void start() throws AlreadyRunningException {
    // We don't care if it stops after this point, only that you called it while it was logically
    // running. Also, it is synchronized so, if it is null, there's no way it'll get another value
    // after this point ;)
    if (mCurrentPomodoro.get() != null) {
      throw new AlreadyRunningException();
    }

    final Runnable pomodoroTick = new Runnable() {
      private final long mStartTime = System.nanoTime();

      private void endPomodoro() {
        incrementStats();
        notifyListener(ListenerAction.END_POMODORO, 0, mCurrentState.get());

        // Force other threads to update
        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {
          Log.d(DEBUG_TAG, "Ooops, thread was interruped");
        }

        // Start the break
        if (mStats.finishedToday % 4 == 0) {
          mCurrentState.set(PomodoroState.LONG_BREAK);
          mCurrentTime.set(LONG_BREAK_DURATION);
        }
        else {
          mCurrentState.set(PomodoroState.SHORT_BREAK);
          mCurrentTime.set(SHORT_BREAK_DURATION);
        }
        notifyListener(ListenerAction.START_BREAK, mCurrentTime.get(), mCurrentState.get());
      }

      private void endBreak() {
        stop();
        if (mAutoStart) {
          try {
            // Force the UI to catch up and and give time to breath. We only need to do it here because of the
            // autostart, which will trigger another notification update immediately.
            Thread.sleep(50);
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

        if (mCurrentTime.decrementAndGet() <= 0) {
          if (mCurrentState.get() == PomodoroState.POMODORO) {
            Log.d(DEBUG_TAG, "Pomodoro ended after " + ((System.nanoTime() - mStartTime) * 1e-9));
            endPomodoro();
          }
          else { // LONG or SHORT break
            Log.d(DEBUG_TAG, "Pomodoro and break ended after " + ((System.nanoTime() - mStartTime) * 1e-9));
            endBreak();
          }
        }
        else {
          Log.d(DEBUG_TAG, "Timer: " + mCurrentTime.get());
          notifyListener(ListenerAction.TICK, mCurrentTime.get(), mCurrentState.get());
        }
      }
    };

    Log.i(DEBUG_TAG, "Pomodoro started");
    mIsPaused = false;
    mCurrentState.set(PomodoroState.POMODORO);
    mCurrentTime.set(POMODORO_DURATION);
    mCurrentPomodoro.set(mExecutionService.scheduleAtFixedRate(pomodoroTick, 1, 1, TimeUnit.SECONDS));
    notifyListener(ListenerAction.START, POMODORO_DURATION, mCurrentState.get());
  }

  private void notifyListener(ListenerAction action, int currentTime, PomodoroState state) {
    PomodoroEventListener listener = mListener;
    if (listener == null) {
      return;
    }

    try {
      // If we forced stop, we must override the value of the auto start, otherwise the client may think it is
      // going to start again
      boolean autoStart = currentTime == 0 && this.mAutoStart;
      PomodoroEvent event = new PomodoroEvent(this, currentTime, autoStart, state);
      // I used actions because creating and passing callables for something so static isn't convenient ;)
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
        case PAUSED:
          mListener.paused(event);
          break;
        case RESUMED:
          mListener.resumed(event);
          break;
      }
    }
    catch (Exception e) {
      Log.e(DEBUG_TAG, "Exception thrown while calling the listener: " + e.toString());
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
    mStats = mStats.incrementCounter(mCurrentProject.get());
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
      notifyListener(ListenerAction.FINISH, mCurrentTime.get(), mCurrentState.get());
      mCurrentState.set(PomodoroState.NONE);
    }
  }

  /**
   * Pauses the current timer or does nothing if no timer is running.
   */
  protected void pause() {
    if (mIsPaused == true) {
      return;
    }

    mIsPaused = true;
    Log.i(DEBUG_TAG, "Timer paused");
    ScheduledFuture pomodoro = mCurrentPomodoro.get();
    if (pomodoro != null) {
      notifyListener(ListenerAction.PAUSED, mCurrentTime.get(), mCurrentState.get());
    }
  }

  /**
   * Resumes the current timer or does nothing if no timer is running.
   */
  protected void resume() {
    if (mIsPaused == false) {
      return;
    }

    Log.i(DEBUG_TAG, "Timer resumed");
    // This means that we may have to wait almost a second before the next run, but it's a simple
    // mechanism ;)
    mIsPaused = false;
    ScheduledFuture pomodoro = mCurrentPomodoro.get();
    if (pomodoro != null) {
      notifyListener(ListenerAction.RESUMED, mCurrentTime.get(), mCurrentState.get());
    }
  }

  /**
   * Sets the auto start flag.
   * If set to true, when a pomodoro finishes, another one will start immediately.
   *
   * @param autoStart new value.
   */
  public void setAutoStart(boolean autoStart) {
    mAutoStart = autoStart;
  }

  /**
   * Gets the auto start flag value.
   *
   * @return the auto start value.
   */
  public boolean getAutoStart() {
    return mAutoStart;
  }

  /**
   * Sets the current project and adds it to the list of known projects if it doesn't exist yet.
   *
   * @param currentProject Name of the current project to set.
   */
  public void setCurrentProject(final String currentProject) {
    mCurrentProject.set(currentProject);
    mStats = mStats.addProject(currentProject);
  }

  public String getCurrentProject() {
    return mCurrentProject.get();
  }

  public Collection<String> getAllProjects() {
    HashSet<String> projectNames = new HashSet<String>(mStats.getProjects().keySet());
    String currentProject = mCurrentProject.get();
    if (currentProject != null && !currentProject.isEmpty()) {
      projectNames.add(mCurrentProject.get());
    }
    return projectNames;
  }

  public void setPomodoroListener(PomodoroEventListener listener) {
    mListener = listener;
  }
}
