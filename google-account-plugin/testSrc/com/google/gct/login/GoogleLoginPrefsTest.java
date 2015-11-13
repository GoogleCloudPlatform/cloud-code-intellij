/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.login;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link GoogleLoginPrefs}.
 */
public class GoogleLoginPrefsTest {

  private String oldPreferencesPath;

  @Before
  public void setUp() {
    oldPreferencesPath = GoogleLoginPrefs.getPreferencesPath();
    GoogleLoginPrefs.setPreferencesPath("/com/google/gct/login/test");
  }

  @After
  public void tearDown() {
    GoogleLoginPrefs.setPreferencesPath(oldPreferencesPath);
  }

  @Test
  public void testActiveUser() {
    GoogleLoginPrefs.saveActiveUser("cheese");
    Assert.assertEquals("cheese", GoogleLoginPrefs.getActiveUser());
    GoogleLoginPrefs.removeActiveUser();
    Assert.assertNull(GoogleLoginPrefs.getActiveUser());
  }
}
