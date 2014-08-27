package com.mindfulst.dneves.pomotivity.api;

import junit.framework.TestCase;

/**
 * Tests the PomodoroApi class.
 */
public class PomodoroApiTest extends TestCase {
  /**
   * Tests that getting the current project gets the project we just set.
   */
  public void testGetCurrentProjectGetsCurrentlySet() {
    PomodoroApi api = new PomodoroApi();
    api.setCurrentProject("test project");
    assertEquals("test project", api.getCurrentProject().toLowerCase());
    api.setCurrentProject("test project 2");
    assertEquals("test project 2", api.getCurrentProject().toLowerCase());
  }

  /**
   * Tests that setting the current project adds it to the project list.
   */
  public void testSetCurrentProjectAddsToProjectList() {
    PomodoroApi api = new PomodoroApi();
    api.setCurrentProject("boom");
    assertTrue(api.getAllProjects().contains("boom"));
  }
}
