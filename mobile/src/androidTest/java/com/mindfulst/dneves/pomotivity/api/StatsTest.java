package com.mindfulst.dneves.pomotivity.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;

import com.mindfulst.dneves.pomotivity.R;

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

      Set<String> projectSet = new HashSet<String>(1);
      projectSet.add(String.format("%s,1", projectName));
      dataMap.put(context.getString(R.string.projects_key), projectSet);

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

  public void testLoadConstructorWithData() {
    final Context context = getInstrumentation().getTargetContext();
    final String projectName = "Test Project";
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);

    Stats stats = new Stats(context, prefs);
    assertEquals(3, stats.finishedToday);
    assertEquals(4, stats.allTime);
    assertEquals(2, stats.totalDays);
    assertTrue(stats.getProjects().size() == 1);
    MoreAsserts.assertContentsInAnyOrder(stats.getProjects().keySet(), projectName);
    assertEquals(1, (int) stats.getProjects().get(projectName));
  }

  /**
   * Tests against stats default values.
   * @param stats Stats object to test.
   */
  private void assertDefaultValues(Stats stats) {
    assertEquals(0, stats.finishedToday);
    assertEquals(0, stats.allTime);
    assertEquals(0, stats.totalDays);
    MoreAsserts.assertEmpty(stats.getProjects());
  }
}
