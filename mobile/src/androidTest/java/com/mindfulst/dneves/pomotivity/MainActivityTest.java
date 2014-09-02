package com.mindfulst.dneves.pomotivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests the MainActivity.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

  private static final int INITIAL_PROJECT_ADAPTER_COUNT = 1;

  private Activity mActivity;
  private Context  mContext;

  /**
   * Creates the default test.
   */
  public MainActivityTest() {
    super(MainActivity.class);
  }

  /**
   * Sets the test up.
   */
  @SuppressLint("CommitPrefEdits")
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mContext = getInstrumentation().getTargetContext();
    SharedPreferences prefs = mContext.getSharedPreferences(MainActivity.class.getName(), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit().clear();
    editor.putStringSet(mContext.getString(R.string.projects_key),
                        new HashSet<String>(Arrays.asList("Test Project 1,1")));
    editor.commit();

    mActivity = getActivity();
  }

  /**
   * Tests if the Activity was properly setup.
   */
  public void testPreConditions() {
    assertNotNull(MainActivity.PomodoroApiWrapper.getOrCreate().getPomodoroListener());
  }

  /**
   * Tests if the Project Spinner was properly setup.
   */
  public void testProjectSpinnerPreConditions() {
    Spinner projectsChooser = (Spinner) mActivity.findViewById(R.id.current_project);
    assertNotNull(projectsChooser.getOnItemSelectedListener());

    SpinnerAdapter projectAdapter = projectsChooser.getAdapter();
    assertEquals(MainActivity.PomodoroApiWrapper.getOrCreate().getAllProjects().size() + INITIAL_PROJECT_ADAPTER_COUNT,
                 projectAdapter.getCount());
    assertEquals(mContext.getString(R.string.project_add), projectAdapter.getItem(projectAdapter.getCount() - 1));
  }
}
