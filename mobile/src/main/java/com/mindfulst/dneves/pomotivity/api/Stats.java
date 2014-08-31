package com.mindfulst.dneves.pomotivity.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.mindfulst.dneves.pomotivity.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable class responsible for keeping simple stats state.
 */
public final class Stats {
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
   * <p/>
   * This should be used internally only.
   * This class should only be constructed with default values, load constructor or by any of the methods.
   *
   * @param finishedToday Today's counter.
   * @param allTime       All time pomodoro counter.
   * @param totalDays     Total days with Pomodoros.
   * @param projectMap    Map of current projects with their individual pomodoro counters.
   */
  private Stats(int finishedToday, int allTime, int totalDays, Map<String, Integer> projectMap) {
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
    this.mProjectMap = parseProjectMap(preferences.getStringSet(context.getString(R.string.projects_key), null));
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
   * @return A new stats object with the new project or the same Stats object if the project already exists.
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
   * Increments finished today, all time and a project counters (if a project name is given).
   *
   * @param currentProject Name of a project or null. This will increment its counter too. You must call addProject
   *                       first.
   * @return new Stats instance with the incremented counters.
   */
  protected Stats incrementCounter(String currentProject) {
    Map<String, Integer> newProjectsMap = mProjectMap;
    // If we have a current project we'll have to change it...
    if (currentProject != null && !currentProject.isEmpty()) {
      // We need to copy because some code might use the previous Stats objects which should be immutable
      newProjectsMap = new HashMap<String, Integer>(mProjectMap);
      newProjectsMap.put(currentProject, newProjectsMap.get(currentProject) + 1);
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
              .putStringSet(context.getString(R.string.projects_key), getProjectMapAsSet());
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
