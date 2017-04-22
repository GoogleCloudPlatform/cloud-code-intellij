/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.flags.FlagReader;
import com.google.cloud.tools.intellij.util.IntelliJPlatform;
import com.google.common.collect.ImmutableSet;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Tests for {@link BasePluginInfoService}. */
@RunWith(MockitoJUnitRunner.class)
public class BasePluginInfoServiceTest {

  private static final String TEST_VERSION = "1.0";
  private static final String USER_AGENT_NAME = "testUserAgentName";
  @Mock private IdeaPluginDescriptor mockPlugin;
  @Mock private FlagReader flagReader;
  private BasePluginInfoService underTest;

  @Before
  public void setup() {
    when(mockPlugin.getVersion()).thenReturn(TEST_VERSION);
    underTest =
        new BasePluginInfoService(USER_AGENT_NAME, mockPlugin, flagReader) {
          @NotNull
          @Override
          IntelliJPlatform getCurrentPlatform() {
            return IntelliJPlatform.IDEA;
          }

          @NotNull
          @Override
          String getCurrentPlatformVersion() {
            return "15.03";
          }
        };
  }

  @Test
  public void testGetUserAgent() throws Exception {
    assertEquals(
        "testUserAgentName/1.0 (IntelliJ IDEA Ultimate Edition/15.03)", underTest.getUserAgent());
  }

  @Test
  public void testGetPluginVersion() throws Exception {
    assertEquals(TEST_VERSION, underTest.getPluginVersion());
  }

  @Test
  public void testIsEnabled_platformSupported() throws Exception {
    Feature platformEnabledFeature = mock(Feature.class);
    when(platformEnabledFeature.getSupportedPlatforms())
        .thenReturn(ImmutableSet.of(IntelliJPlatform.IDEA));
    assertTrue(underTest.shouldEnable(platformEnabledFeature));
  }

  @Test
  public void testIsEnabled_platformNotSupported() throws Exception {
    Feature platformEnabledFeature = mock(Feature.class);
    when(platformEnabledFeature.getSupportedPlatforms())
        .thenReturn(ImmutableSet.of(IntelliJPlatform.ANDROID_STUDIO));
    assertFalse(underTest.shouldEnable(platformEnabledFeature));
  }

  @Test
  public void testIsEnabled_noPlatformsSupported() throws Exception {
    Feature platformEnabledFeature = mock(Feature.class);
    when(platformEnabledFeature.getSupportedPlatforms())
        .thenReturn(ImmutableSet.<IntelliJPlatform>of());
    assertFalse(underTest.shouldEnable(platformEnabledFeature));
  }

  @Test
  public void testIsEnabled_configIsNull() throws Exception {
    Feature platformEnabledFeature = mock(Feature.class);
    when(platformEnabledFeature.getSupportedPlatforms()).thenReturn(null);
    when(platformEnabledFeature.getResourceFlagName()).thenReturn(null);
    when(platformEnabledFeature.getSystemFlagName()).thenReturn(null);
    assertFalse(underTest.shouldEnable(platformEnabledFeature));
  }

  @Test
  public void testIsEnabled_flagEnabledByConfig() throws Exception {
    Feature resourceFlagEnabledFeature = mock(Feature.class);
    String flagString = "TEST_FLAG";
    when(flagReader.getFlagString(flagString)).thenReturn("true");
    when(resourceFlagEnabledFeature.getResourceFlagName()).thenReturn(flagString);
    assertTrue(underTest.shouldEnable(resourceFlagEnabledFeature));
  }

  @Test
  public void testIsEnabled_resourceFlagIsMissing() throws Exception {
    Feature resourceFlagEnabledFeature = mock(Feature.class);
    String flagString = "TEST_FLAG";
    when(resourceFlagEnabledFeature.getResourceFlagName()).thenReturn(flagString);
    when(flagReader.getFlagString(flagString)).thenReturn(null);
    assertFalse(underTest.shouldEnable(resourceFlagEnabledFeature));
  }

  @Test
  public void testIsEnabled_flagEnabledBySystemProperty() throws Exception {
    Feature systemFlagEnabledFeature = mock(Feature.class);
    String flagString = "TEST_FLAG";
    try {
      when(flagReader.getFlagString(flagString)).thenReturn("false");
      when(systemFlagEnabledFeature.getSystemFlagName()).thenReturn(flagString);
      System.setProperty(flagString, "true");
      assertTrue(underTest.shouldEnable(systemFlagEnabledFeature));
    } finally {
      System.clearProperty(flagString);
    }
  }

  @Test
  public void testIsEnabled_SystemPropertyIsMissing() throws Exception {
    Feature systemFlagEnabledFeature = mock(Feature.class);
    String flagString = "TEST_FLAG";
    when(flagReader.getFlagString(flagString)).thenReturn("false");
    when(systemFlagEnabledFeature.getSystemFlagName()).thenReturn(flagString);
    assertFalse(underTest.shouldEnable(systemFlagEnabledFeature));
  }
}
