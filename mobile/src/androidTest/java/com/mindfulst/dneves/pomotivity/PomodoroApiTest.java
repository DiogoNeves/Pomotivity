package com.mindfulst.dneves.pomotivity;

import junit.framework.TestCase;

/**
 * Tests the PomodoroApi code.
 */
public class PomodoroApiTest extends TestCase {
  /**
   * Tests that getting the current project gets the project we just set.
   */
  public void testGetCurrentProjectGetsCurrentlySet() {
    PomodoroApi api = PomodoroApi.getInstance();
    api.setCurrentProject("test project");
    assertEquals(api.getCurrentProject().toLowerCase(), "test project");
    api.setCurrentProject("test project 2");
    assertEquals(api.getCurrentProject().toLowerCase(), "test project 2");
  }

  /**
   * Tests that setting the current project adds it to the project list.
   */
  public void testSetCurrentProjectAddsToProjectList() {
    PomodoroApi api = PomodoroApi.getInstance();
    api.setCurrentProject("boom");
    assertTrue(api.getAllProjects().contains("boom"));
  }
}
