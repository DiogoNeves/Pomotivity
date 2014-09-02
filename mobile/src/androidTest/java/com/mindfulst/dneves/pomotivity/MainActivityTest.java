package com.mindfulst.dneves.pomotivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.mindfulst.dneves.pomotivity.api.PomodoroApi;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests the MainActivity.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

  private static final int    INITIAL_PROJECT_ADAPTER_COUNT = 1;
  private static final String TEST_PROJECT_NAME             = "Test Project 1";

  private Activity    mActivity;
  private Context     mContext;
  private PomodoroApi mApi;

  private Spinner mProjectSpinner;

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
    SharedPreferences prefs = mContext.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit().clear();
    editor.putStringSet(mContext.getString(R.string.projects_key),
                        new HashSet<String>(Arrays.asList(TEST_PROJECT_NAME + ",1")));
    editor.commit();

    mActivity = getActivity();
    mApi = MainActivity.PomodoroApiWrapper.getOrCreate();

    mProjectSpinner = (Spinner) mActivity.findViewById(R.id.current_project);
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
    assertNotNull(mProjectSpinner.getOnItemSelectedListener());

    SpinnerAdapter projectAdapter = mProjectSpinner.getAdapter();
    assertEquals(MainActivity.PomodoroApiWrapper.getOrCreate().getAllProjects().size() + INITIAL_PROJECT_ADAPTER_COUNT,
                 projectAdapter.getCount());
    assertEquals(TEST_PROJECT_NAME, projectAdapter.getItem(0));
    assertEquals(mContext.getString(R.string.project_add), projectAdapter.getItem(projectAdapter.getCount() - 1));
  }

  /**
   * Tests selecting the test project sets it as current.
   */
  public void testProjectSelectionSetsCurrent() throws InterruptedException {
    getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        assertTrue(mProjectSpinner.requestFocus());
        mProjectSpinner.setSelection(0, false);
      }
    });
    getInstrumentation().waitForIdleSync();
    assertEquals(TEST_PROJECT_NAME, mApi.getCurrentProject());
  }
}
