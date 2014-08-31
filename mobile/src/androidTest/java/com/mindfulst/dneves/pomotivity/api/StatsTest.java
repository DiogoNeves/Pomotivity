package com.mindfulst.dneves.pomotivity.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;

/**
 * Tests the Stats class.
 */
public class StatsTest extends InstrumentationTestCase {
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
    Stats stats = new Stats().resetToday();
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
    MoreAsserts.assertContentsInAnyOrder(stats.getProjects().keySet(), projectOne, projectTwo);
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
   * Tests if incrementing the day with default values returns the same + 1 day.
   */
  public void testNextDayFromDefaultValues() {
    Stats stats = new Stats().nextDay();
    assertEquals(0, stats.finishedToday);
    assertEquals(0, stats.allTime);
    assertEquals(1, stats.totalDays);
  }

  /**
   * Tests if incrementing the day with data returns the same + 1 day.
   */
  public void testNextDayWithData() {
    final Context context = getInstrumentation().getTargetContext();
    final String projectName = "Test Next Day";
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);

    Stats stats = new Stats(context, prefs).nextDay();
    assertEquals(0, stats.finishedToday);
    assertEquals(4, stats.allTime);
    assertEquals(3, stats.totalDays);
    assertProject(stats, projectName, 1);
  }

  /**
   * Tests the output of toString.
   * <p/>
   * This is not an extensive test, used more as a warning that the output changed, not a contract enforcement ;)
   */
  public void testToString() {
    final Context context = getInstrumentation().getTargetContext();
    final String projectName = "Test Next Day";
    SharedPreferences prefs = MockSharedPreferences.createWithTestData(context, projectName);

    Stats stats = new Stats(context, prefs);
    String expected = "PomodoroApi.Stats(finishedToday:3, allTime:4, totalDays:2, totalProjects:1)";
    assertEquals(expected, stats.toString());
  }

  public void testGetProjects() {
    final String projectOne = "One";
    final String projectTwo = "Two";
    final String projectThree = "Three";

    Stats stats = new Stats();
    MoreAsserts.assertEmpty(stats.getProjects());
    stats = stats.addProject(projectOne);
    assertTrue(stats.getProjects().size() == 1);
    stats = stats.addProject(projectTwo);
    assertTrue(stats.getProjects().size() == 2);
    stats = stats.addProject(projectThree);
    assertTrue(stats.getProjects().size() == 3);
    MoreAsserts.assertContentsInAnyOrder(stats.getProjects().keySet(), projectOne, projectTwo, projectThree);
  }

  /**
   * Tests if saving saves all values and projects.
   */
  public void testSave() {
    final String projectOne = "One";
    final String projectTwo = "Two";
    Stats stats = new Stats();
    // Our goal is to have 1 today, 3 total, 3 days, 2 projects with 1 and 2 counter respectively
    stats = stats.addProject(projectOne).addProject(projectTwo);
    stats = stats.incrementCounter(projectOne).incrementCounter(projectTwo);
    stats = stats.nextDay().nextDay().nextDay();
    stats = stats.incrementCounter(projectTwo);

    final Context context = getInstrumentation().getTargetContext();
    SharedPreferences prefs = MockSharedPreferences.createEmpty();
    MockSharedPreferences.MockEditor editor = (MockSharedPreferences.MockEditor) prefs.edit();
    stats.save(context, editor);
    editor.apply();

    Stats loaded = new Stats(context, prefs);
    assertEquals(1, loaded.finishedToday);
    assertEquals(3, loaded.allTime);
    assertEquals(3, loaded.totalDays);
    assertTrue(loaded.getProjects().size() == 2);
    MoreAsserts.assertEquals(stats.getProjects().entrySet(), loaded.getProjects().entrySet());
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
