package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;

import com.mindfulst.dneves.pomotivity.api.PomodoroApi;

/**
 * Tests the (@link MainActivity) in Landscape mode.
 */
public class MainActivityLandscapeTest extends ActivityInstrumentationTestCase2<MainActivity> {
  private Activity    mActivity;
  private PomodoroApi mApi;

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
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    Instrumentation.ActivityMonitor monitor =
        new Instrumentation.ActivityMonitor(MainActivity.class.getName(), null, false);
    getInstrumentation().addMonitor(monitor);
    getInstrumentation().waitForIdleSync();
    mActivity = getInstrumentation().waitForMonitor(monitor);

    mApi = MainActivity.PomodoroApiWrapper.getOrCreate();
  }

  /**
   * Tests if the Activity was properly setup.
   */
  public void testPreConditions() {
    assertNotNull(MainActivity.PomodoroApiWrapper.getOrCreate().getPomodoroListener());
    assertEquals(Configuration.ORIENTATION_LANDSCAPE, mActivity.getResources().getConfiguration().orientation);
  }
}
