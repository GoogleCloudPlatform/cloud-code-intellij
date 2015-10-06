package com.google.gct.idea.feedback;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Mundane test to make sure the resource loading is working correctly.
 */
public class ErrorReporterBundleTest {

  @Test
  public void testMessage() throws Exception {
    final String errorString = ErrorReporterBundle.message("error.googlefeedback.message");
    assertNotNull(errorString);
  }
}