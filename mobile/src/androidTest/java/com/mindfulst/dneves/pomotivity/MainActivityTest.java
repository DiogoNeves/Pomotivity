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

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

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
    assertEquals(mApi.getAllProjects().size() + INITIAL_PROJECT_ADAPTER_COUNT, projectAdapter.getCount());
    assertEquals(TEST_PROJECT_NAME, projectAdapter.getItem(0));
    assertEquals(mContext.getString(R.string.project_add), projectAdapter.getItem(projectAdapter.getCount() - 1));
  }

  /**
   * Tests selecting the test project sets it as current.
   */
  public void testProjectSelectionSetsCurrent() {
    onView(withId(R.id.current_project)).perform(click());
    onData(allOf(is(instanceOf(String.class)), is(TEST_PROJECT_NAME))).perform(click());
    assertEquals(TEST_PROJECT_NAME, mApi.getCurrentProject());
  }
}
