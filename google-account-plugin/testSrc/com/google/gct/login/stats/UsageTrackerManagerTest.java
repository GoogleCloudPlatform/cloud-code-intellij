package com.google.gct.login.stats;

import static org.mockito.Mockito.when;

import com.google.gct.login.PluginFlags;

import com.intellij.ide.util.PropertiesComponent;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link UsageTrackerManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UsageTrackerManagerTest extends TestCase {

  @Mock
  private PropertiesComponent mockComponent;
  @Mock
  private PluginFlags mockFlags;

  private UsageTrackerManager manager;

  @Before
  public void setUp() {
    manager = new UsageTrackerManager(mockComponent, mockFlags);
  }

  @Test
  public void testHasUserRecordedTrackingPreference_noPrefSet() {
    assertFalse(manager.hasUserRecordedTrackingPreference());
  }

  @Test
  public void testHasUserRecordedTrackingPreference_prefSetToTrue() {
    when(mockComponent.getValue(UsageTrackerManager.USAGE_TRACKER_KEY)).thenReturn("true");
    assertTrue(manager.hasUserRecordedTrackingPreference());
  }

  @Test
  public void testHasUserRecordedTrackingPreference_prefSetToFalse() {
    when(mockComponent.getValue(UsageTrackerManager.USAGE_TRACKER_KEY)).thenReturn("false");

    assertTrue(manager.hasUserRecordedTrackingPreference());
  }

  @Test
  public void testGetAnalyticsProperty() {
    when(mockFlags.getAnalyticsId()).thenReturn("test");
    assertEquals("test", manager.getAnalyticsProperty());
  }

  @Test
  public void testGetAnalyticsProperty_placeHolderShouldResultInNull() {
    when(mockFlags.getAnalyticsId())
        .thenReturn(UsageTrackerManager.USAGE_TRACKER_PROPERTY_PLACEHOLDER);
    assertNull(manager.getAnalyticsProperty());
  }
}
