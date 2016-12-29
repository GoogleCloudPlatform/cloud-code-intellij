/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.application;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.google.api.services.appengine.v1.model.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

/**
 * Tests for {@link AppEngineLocationSelectorItem}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineLocationSelectorItemTest {

  private Location location;

  @Before
  public void setup() {
    location = new Location();
    location.setMetadata(new HashMap<>());
  }

  @Test
  public void testIsStandardSupported_true() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.STANDARD_ENV_AVAILABLE_KEY, Boolean.TRUE);
    assertTrue(new AppEngineLocationSelectorItem(location).isStandardSupported());
  }

  @Test
  public void testIsStandardSupported_false() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.STANDARD_ENV_AVAILABLE_KEY, Boolean.FALSE);
    assertFalse(new AppEngineLocationSelectorItem(location).isStandardSupported());
  }

  @Test
  public void testIsStandardSupported_null() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.STANDARD_ENV_AVAILABLE_KEY, null);
    assertFalse(new AppEngineLocationSelectorItem(location).isStandardSupported());
  }

  @Test
  public void testIsStandardSupported_wrongType() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.STANDARD_ENV_AVAILABLE_KEY, Integer.valueOf(1));
    assertFalse(new AppEngineLocationSelectorItem(location).isStandardSupported());
  }

  @Test
  public void testIsFlexSupported_true() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.FLEXIBLE_ENV_AVAILABLE_KEY, Boolean.TRUE);
    assertTrue(new AppEngineLocationSelectorItem(location).isFlexSupported());
  }

  @Test
  public void testIsFlexSupported_false() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.FLEXIBLE_ENV_AVAILABLE_KEY, Boolean.FALSE);
    assertFalse(new AppEngineLocationSelectorItem(location).isFlexSupported());
  }

  @Test
  public void testIsFlexSupported_null() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.FLEXIBLE_ENV_AVAILABLE_KEY, null);
    assertFalse(new AppEngineLocationSelectorItem(location).isFlexSupported());
  }

  @Test
  public void testIsFlexSupported_wrongType() {
    location.getMetadata()
        .put(AppEngineLocationSelectorItem.FLEXIBLE_ENV_AVAILABLE_KEY, Integer.valueOf(1));
    assertFalse(new AppEngineLocationSelectorItem(location).isFlexSupported());
  }
}
