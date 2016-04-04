/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.stats;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.login.PluginFlags;

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
