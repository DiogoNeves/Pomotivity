package com.mindfulst.dneves.pomotivity.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.mindfulst.dneves.pomotivity.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mocks some of the behaviour of SharedPreferences.
 */
public class MockSharedPreferences implements SharedPreferences {
  public class MockEditor implements Editor {
    private final Map<String, Object> mDataDestination;

    private MockEditor(Map<String, Object> dataMap) {
      mDataDestination = dataMap;
    }

    @Override
    public Editor putString(String key, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor putStringSet(String key, Set<String> values) {
      mDataDestination.put(key, values);
      return this;
    }

    @Override
    public Editor putInt(String key, int value) {
      mDataDestination.put(key, value);
      return this;
    }

    @Override
    public Editor putLong(String key, long value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor putFloat(String key, float value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor putBoolean(String key, boolean value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor remove(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Editor clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean commit() {
      return true;
    }

    @Override
    public void apply() {
    }
  }

  private Map<String, Object> mDataMap;

  protected MockSharedPreferences(final Map<String, Object> dataMap) {
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
  @SuppressWarnings("unchecked")
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
    return new MockEditor(mDataMap);
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
