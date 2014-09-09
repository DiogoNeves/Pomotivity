package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers;
import com.mindfulst.dneves.pomotivity.api.PomodoroApi;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;

/**
 * Tests the (@link MainActivity) in Landscape mode.
 */
public class MainActivityLandscapeTest extends ActivityInstrumentationTestCase2<MainActivity> {
  private Activity mActivity;

  private PomodoroApi                       mApi;
  private PomodoroApi.PomodoroEventListener mOriginalListener;

  /**
   * Default Constructor.
   */
  public MainActivityLandscapeTest() {
    super(MainActivity.class);
  }

  /**
   * Sets the test up.
   *
   * @throws Exception
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Activity activity = getActivity();
    mApi = MainActivity.PomodoroApiWrapper.getOrCreate();
    mOriginalListener = mApi.getPomodoroListener();

    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor(MainActivity.class.getName(), null, false);
    getInstrumentation().addMonitor(monitor);
    getInstrumentation().waitForIdleSync();
    mActivity = getInstrumentation().waitForMonitor(monitor);
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
    assertNotSame(mOriginalListener, mApi.getPomodoroListener());
    assertFalse(mApi.isRunning());
    assertEquals(Configuration.ORIENTATION_LANDSCAPE, mActivity.getResources().getConfiguration().orientation);

    onView(withId(R.id.start_button)).check(matches(isDisplayed()));
    onView(withId(R.id.pause_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.stop_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    onView(withId(R.id.resume_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
  }
}
