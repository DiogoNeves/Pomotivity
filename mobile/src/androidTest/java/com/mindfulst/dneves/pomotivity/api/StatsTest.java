package com.mindfulst.dneves.pomotivity.api;

import junit.framework.TestCase;

/**
 * Tests the Stats class.
 */
public class StatsTest extends TestCase {
  public void testDefaultConstructor() {
    Stats stats = new Stats();
    assertEquals(0, stats.finishedToday);
    assertEquals(0, stats.allTime);
    assertEquals(0, stats.totalDays);
    assertTrue(stats.getProjects().isEmpty());
  }
}
