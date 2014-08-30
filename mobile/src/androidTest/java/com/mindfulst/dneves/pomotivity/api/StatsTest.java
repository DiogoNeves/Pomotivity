package com.mindfulst.dneves.pomotivity.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;

import com.mindfulst.dneves.pomotivity.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests the Stats class.
 */
public class StatsTest extends InstrumentationTestCase {
  private static class MockSharedPreferences implements SharedPreferences {
    private Map<String, ?> mDataMap;

    protected MockSharedPreferences(final Map<String, ?> dataMap) {
      mDataMap = dataMap;
    }

    protected static MockSharedPreferences createEmpty() {
      return new MockSharedPreferences(new HashMap<String, Object>(1));
    }

    protected static MockSharedPreferences createWithTestData(final Context context, final String projectName) {
      Map<String, Object> dataMap = new HashMap<String, Object>();
      dataMap.put(context.getString(R.string.finished_today_key), 3);
      dataMap.put(context.getString(R.string.all_time_key), 4);
      dataMap.put(context.getString(R.string.total_days_key), 2);

      if (projectName != null && !projectName.isEmpty()) {
        Set<String> projectSet = new HashSet<String>(1);
        projectSet.add(String.format("%s,1", projectName));
        dataMap.put(context.getString(R.string.projects_key), projectSet);
      }

      return new MockSharedPreferences(dataMap);
    }

    @Override
    public Map<String, ?> getAll() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getString(String key, String defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      if (mDataMap.containsKey(key)) {
        return (Set<String>) mDataMap.get(key);
      }
      else {
        return defValues;
      }
    }

    @Override
    public int getInt(String key, int defValue) {
      if (mDataMap.containsKey(key)) {
        return (Integer) mDataMap.get(key);
      }
      else {
        return defValue;
      }
    }

    @Override
    public long getLong(String key, long defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(String key, float defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor edit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Tests if the default constructor creates stats with the right value.
   */
  public void testDefaultConstructor() {
    Stats stats = new Stats();
    assertDefaultValues(stats);
  }

  /**
   * Tests if the constructor loads the default values if preferences are empty.
   */
  public void testLoadConstructorWithEmptyData() {
    SharedPreferences emptyPrefs = MockSharedPreferences.createEmpty();
    Stats stats = new Stats(getInstrumentation().getTargetContext(), emptyPrefs);
    assertDefaultValues(stats);
  }

  /**
   * Tests if the constructor loads the values from the shared preferences.
   */
  public void testLoadConstructorWithData() {
    final Context context = getInstrumentation().getTargetContext();
    final String projectName = "Test Project";
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);

    Stats stats = new Stats(context, prefs);
    assertEquals(3, stats.finishedToday);
    assertEquals(4, stats.allTime);
    assertEquals(2, stats.totalDays);
    assertProject(stats, projectName, 1);
  }

  /**
   * Tests if resetting stats with default values returns the same values.
   */
  public void testResetTodayFromDefaultValues() {
    Stats stats = new Stats();
    stats.resetToday();
    assertDefaultValues(stats);
  }

  /**
   * Tests if resetting stats with some data only resets the today value.
   */
  public void testResetTodayWithData() {
    final Context context = getInstrumentation().getTargetContext();
    final String projectName = "Test Today";
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);

    Stats stats = new Stats(context, prefs).resetToday();
    assertEquals(0, stats.finishedToday);
    assertEquals(4, stats.allTime);
    assertEquals(2, stats.totalDays);
    assertProject(stats, projectName, 1);
  }

  /**
   * Tests adding a new project actually adds it and returns the same values for the other attributes.
   */
  public void testAddNewProject() {
    // From default values
    final String projectName = "New Project";
    Stats stats = new Stats().addProject(projectName);
    assertEquals(0, stats.finishedToday);
    assertEquals(0, stats.allTime);
    assertEquals(0, stats.totalDays);
    assertProject(stats, projectName, 0);

    // From data
    final Context context = getInstrumentation().getTargetContext();
    // Don't create with a project otherwise we wouldn't be testing a new project
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, null);
    stats = new Stats(context, prefs).addProject(projectName);
    assertEquals(3, stats.finishedToday);
    assertEquals(4, stats.allTime);
    assertEquals(2, stats.totalDays);
    assertProject(stats, projectName, 0);
  }

  /**
   * Tests adding a project that already exists won't change a thing.
   * Doesn't test other attributes because we can infer the result from testAddNewProject.
   */
  public void testAddExistingProject() {
    // From default values
    final String projectName = "Existing Project";
    Stats stats = new Stats().addProject(projectName).addProject(projectName);
    assertProject(stats, projectName, 0);

    // From data
    final Context context = getInstrumentation().getTargetContext();
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);
    // Only need to add once because it was already loaded with the preferences
    stats = new Stats(context, prefs).addProject(projectName);
    assertProject(stats, projectName, 1);
  }

  /**
   * Tests if adding multiple different projects adds all of them.
   */
  public void testAddMultiProjects() {
    final String projectOne = "Project One";
    final String projectTwo = "Project Two";
    Stats stats = new Stats().addProject(projectOne).addProject(projectTwo);

    assertTrue(stats.getProjects().size() == 2);
    MoreAsserts.assertEquals(stats.getProjects().keySet(), new HashSet<String>(Arrays.asList(projectOne, projectTwo)));
    assertEquals(0, (int) stats.getProjects().get(projectOne));
    assertEquals(0, (int) stats.getProjects().get(projectTwo));

    // Add project one again, the result should be the same
    stats = stats.addProject(projectOne);
    assertTrue(stats.getProjects().size() == 2);
    assertEquals(0, (int) stats.getProjects().get(projectOne));
  }

  /**
   * Tests if incrementing really increments all counters (no project).
   */
  public void testIncrementCountersOnly() {
    Stats stats = new Stats().incrementCounter(null);
    assertEquals(1, stats.finishedToday);
    assertEquals(1, stats.allTime);
    assertEquals(0, stats.totalDays);
    MoreAsserts.assertEmpty(stats.getProjects());

    // From data
    final Context context = getInstrumentation().getTargetContext();
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, null);
    stats = new Stats(context, prefs).incrementCounter(null);
    assertEquals(4, stats.finishedToday);
    assertEquals(5, stats.allTime);
    assertEquals(2, stats.totalDays);
    MoreAsserts.assertEmpty(stats.getProjects());
  }

  /**
   * Tests if incrementing with a project that wasn't yet added actually throws.
   */
  public void testIncrementWithInvalidProjectThrows() {
    try {
      new Stats().incrementCounter("Invalid");
      fail("Incrementing the counter should've thrown an exception");
    }
    catch (NullPointerException ex) {
    }
  }

  /**
   * Tests if incrementing really increments the project counter.
   */
  public void testIncrementProjectCounter() {
    final String projectName = "Incrementor";
    Stats stats = new Stats().addProject(projectName).incrementCounter(projectName);
    assertEquals(1, stats.finishedToday);
    assertEquals(1, stats.allTime);
    assertEquals(0, stats.totalDays);
    assertProject(stats, projectName, 1);
    stats = stats.incrementCounter(projectName);
    assertProject(stats, projectName, 2);

    // From data
    final Context context = getInstrumentation().getTargetContext();
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);
    stats = new Stats(context, prefs).incrementCounter(projectName);
    assertEquals(4, stats.finishedToday);
    assertEquals(5, stats.allTime);
    assertEquals(2, stats.totalDays);
    assertProject(stats, projectName, 2);
  }

  /**
   * Tests if incrementing really increments only one project in multiple projects.
   */
  public void testIncrementMultiProjectCounters() {
    final String projectOne = "Incrementor 1";
    final String projectTwo = "Incrementor 2";
    Stats stats = new Stats().addProject(projectOne).addProject(projectTwo).incrementCounter(projectOne);
    assertEquals(1, stats.finishedToday);
    assertEquals(1, stats.allTime);
    assertEquals(0, stats.totalDays);
    assertEquals(1, (int) stats.getProjects().get(projectOne));
    assertEquals(0, (int) stats.getProjects().get(projectTwo));
    stats = stats.incrementCounter(projectTwo);
    assertEquals(1, (int) stats.getProjects().get(projectOne));
    assertEquals(1, (int) stats.getProjects().get(projectTwo));
  }

  /**
   * Tests against stats default values.
   *
   * @param stats Stats object to test.
   */
  private void assertDefaultValues(final Stats stats) {
    assertEquals(0, stats.finishedToday);
    assertEquals(0, stats.allTime);
    assertEquals(0, stats.totalDays);
    MoreAsserts.assertEmpty(stats.getProjects());
  }

  private void assertProject(final Stats stats, final String projectName, final int value) {
    assertTrue(stats.getProjects().size() == 1);
    MoreAsserts.assertContentsInAnyOrder(stats.getProjects().keySet(), projectName);
    assertEquals(value, (int) stats.getProjects().get(projectName));
  }
}
