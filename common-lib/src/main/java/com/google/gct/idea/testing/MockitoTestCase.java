package com.google.gct.idea.testing;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

/**
 * Base class for tests that want @Mock based auto injection of mockito mocks.
 */
public class MockitoTestCase {

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
