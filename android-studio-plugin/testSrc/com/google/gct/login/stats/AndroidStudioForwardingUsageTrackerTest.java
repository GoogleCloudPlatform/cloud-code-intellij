/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.login.stats;

import com.google.gct.idea.stats.AndroidStudioForwardingUsageTracker;
import com.google.gct.login.stats.UsageTrackerService.UsageTracker;

import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.LightPlatformTestCase;

/**
 * Test to make sure we're loading the correct usage tracker when in Android Studio
 */
public class AndroidStudioForwardingUsageTrackerTest extends LightPlatformTestCase {

  public AndroidStudioForwardingUsageTrackerTest() {
    IdeaTestCase.autodetectPlatformPrefix();
  }

  public void testGetStudioTracker() {
    UsageTracker tracker = UsageTrackerService.getInstance("AndroidStudio");

    assertInstanceOf(tracker, AndroidStudioForwardingUsageTracker.class);
  }
}
