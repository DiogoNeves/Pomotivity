package com.mindfulst.dneves.pomotivity;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

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
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mContext = getInstrumentation().getTargetContext();
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
