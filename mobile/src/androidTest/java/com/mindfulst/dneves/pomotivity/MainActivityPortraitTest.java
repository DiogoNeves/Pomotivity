package com.mindfulst.dneves.pomotivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers;
import com.mindfulst.dneves.pomotivity.api.PomodoroApi;

import java.util.Arrays;
import java.util.HashSet;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeTextIntoFocusedView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasFocus;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;

/**
 * Tests the MainActivity.
 */
public class MainActivityPortraitTest extends ActivityInstrumentationTestCase2<MainActivity> {
  private static final int    INITIAL_PROJECT_ADAPTER_COUNT = 1;
  private static final String TEST_PROJECT_NAME             = "Test Project 1";
  private static final String TEST_ADD_PROJECT_NAME         = "Test Project 2";

  private Activity    mActivity;
  private Context     mContext;
  private PomodoroApi mApi;

  private Spinner mProjectSpinner;

  /**
   * Creates the default test.
   */
  public MainActivityPortraitTest() {
    super(MainActivity.class);
  }

  /**
   * Sets the test up.
   *
   * @throws Exception
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
   * Tears down the test.
   * Stops any running pomodoro.
   */
  @Override
  protected void tearDown() throws Exception {
    mApi.stop();
    super.tearDown();
  }

  /**
   * Tests if the Activity was properly setup.
   */
  public void testPreConditions() {
    assertEquals(mApi, MainActivity.PomodoroApiWrapper.getOrCreate());
    assertNotNull(mApi.getPomodoroListener());
    assertFalse(mApi.isRunning());
    assertEquals(Configuration.ORIENTATION_PORTRAIT, mActivity.getResources().getConfiguration().orientation);

    onView(withId(R.id.start_button)).check(matches(isDisplayed()));
    onView(withId(R.id.pause_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.stop_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.resume_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
  }

  /**
   * Tests if the Project Spinner was properly setup.
   */
  public void testProjectSpinnerPreConditions() {
    assertNotNull(mProjectSpinner.getOnItemSelectedListener());
    assertProjects(new String[]{TEST_PROJECT_NAME});
  }

  /**
   * Tests selecting the test project sets it as current.
   */
  public void testProjectSelectionSetsCurrent() {
    onView(withId(R.id.current_project)).perform(click());
    onData(hasToString(equalTo(TEST_PROJECT_NAME))).perform(click());
    assertEquals(TEST_PROJECT_NAME, mApi.getCurrentProject());
  }

  /**
   * Tests pressing start actually starts the pomodoro and shows the right buttons.
   */
  public void testPressingStart() {
    onView(withId(R.id.start_button)).perform(click());
    onView(withId(R.id.start_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.pause_button)).check(matches(isDisplayed()));
    onView(withId(R.id.stop_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.resume_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    assertTrue(mApi.isRunning());
  }

  /**
   * Tests pressing pause pauses it.
   */
  public void testPressingPause() {
    onView(withId(R.id.start_button)).perform(click());
    onView(withId(R.id.pause_button)).perform(click());
    onView(withId(R.id.start_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.pause_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.stop_button)).check(matches(isDisplayed()));
    onView(withId(R.id.resume_button)).check(matches(isDisplayed()));
    assertTrue(mApi.isPaused());
  }

  /**
   * Tests if pressing + Project shows the dialog.
   */
  public void testAddProjectDialog() {
    selectAddProject();
    onView(withText(R.string.project_dialog_title)).check(matches(isDisplayed()));
    // Cancel
    onView(withId(android.R.id.button2)).perform(click());
    onView(withText(R.string.project_dialog_title)).check(doesNotExist());
  }

  /**
   * Tests if actually adding something to the dialog sets it as current project.
   */
  public void testAddProjectDialogWithValue() {
    selectAddProject();
    onView(withId(R.id.project_name_box)).perform(typeText(TEST_ADD_PROJECT_NAME));
    // OK
    onView(withId(android.R.id.button1)).perform(click());
    assertProjects(new String[]{TEST_ADD_PROJECT_NAME, TEST_PROJECT_NAME});
  }

  /**
   * Tests if adding a project that already exists doesn't change the spinner contents.
   */
  public void testAddExistingProjectDoesntChange() {
    selectAddProject();
    onView(withId(R.id.project_name_box)).perform(typeText(TEST_PROJECT_NAME));
    // OK
    onView(withId(android.R.id.button1)).perform(click());
    assertProjects(new String[]{TEST_PROJECT_NAME});
  }

  /**
   * Tests if rotating while showing the add project dialog doesn't crash and shows the dialog again.
   */
  public void testRotatingWhileShowingDialogue() {
    selectAddProject();
    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    getInstrumentation().waitForIdleSync();
    onView(withText(R.string.project_dialog_title)).check(matches(isDisplayed()));
    // Cancel
    // TODO: Broken
    onView(withId(android.R.id.button2)).perform(click());
    onView(withText(R.string.project_dialog_title)).check(doesNotExist());
  }

  /**
   * Tests if typeing a project name and rotating keeps the project name there.
   */
  public void testTypingProjectAndRotate() {
  }

  /**
   * Selects the + Project option in the project spinner.
   */
  private void selectAddProject() {
    onView(withId(R.id.current_project)).perform(click());
    onData(hasToString(equalTo(mContext.getString(R.string.project_add)))).perform(click());
  }

  /**
   * Asserts the spinner contains the same projects as the given array (in order).
   *
   * @param projects Projects to assert.
   */
  private void assertProjects(final String[] projects) {
    SpinnerAdapter projectAdapter = mProjectSpinner.getAdapter();
    assertEquals(projects.length, mApi.getAllProjects().size());
    assertEquals(mApi.getAllProjects().size() + INITIAL_PROJECT_ADAPTER_COUNT, projectAdapter.getCount());
    for (int i = 0; i < projects.length; ++i) {
      assertEquals(projects[i], projectAdapter.getItem(i));
    }
    assertEquals(mContext.getString(R.string.project_add), projectAdapter.getItem(projectAdapter.getCount() - 1));
  }
}
