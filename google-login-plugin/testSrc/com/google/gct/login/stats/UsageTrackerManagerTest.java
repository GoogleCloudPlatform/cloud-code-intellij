package com.google.gct.login.stats;

import com.intellij.ide.util.PropertiesComponent;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link UsageTrackerManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UsageTrackerManagerTest extends TestCase {
    @Mock
    private PropertiesComponent mockComponent;

    @Test
    public void testHasUserRecordedTrackingPreference_noPrefSet() {
        UsageTrackerManager usageTrackerManager = new UsageTrackerManager(mockComponent);
        assertFalse(usageTrackerManager.hasUserRecordedTrackingPreference());
    }

    @Test
    public void testHasUserRecordedTrackingPreference_prefSetToTrue() {
        when(mockComponent.getValue(UsageTrackerManager.USAGE_TRACKER_KEY)).thenReturn("true");

        UsageTrackerManager usageTrackerManager = new UsageTrackerManager(mockComponent);
        assertTrue(usageTrackerManager.hasUserRecordedTrackingPreference());
    }

    @Test
    public void testHasUserRecordedTrackingPreference_prefSetToFalse() {
        when(mockComponent.getValue(UsageTrackerManager.USAGE_TRACKER_KEY)).thenReturn("false");

        UsageTrackerManager usageTrackerManager = new UsageTrackerManager(mockComponent);
        assertTrue(usageTrackerManager.hasUserRecordedTrackingPreference());
    }

    @Test
    public void testGetAnalyticsProperty() {
        UsageTrackerManager usageTrackerManager = new UsageTrackerManager(mockComponent);
        assertNotNull(usageTrackerManager.getAnalyticsProperty());
    }
}
