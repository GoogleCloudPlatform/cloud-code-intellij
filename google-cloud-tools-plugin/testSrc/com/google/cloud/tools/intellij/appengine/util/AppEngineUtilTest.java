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

package com.google.cloud.tools.intellij.appengine.util;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;

/**
 * Tests App Engine Utilities
 *
 */
public class AppEngineUtilTest {

  @Test
  public void testDateTimeToVersion() {
    DateTime dateTime = new DateTime(2016, 12, 28, 23, 58, 59); // 12/28/2016 23:58:59
    DateTimeUtils.setCurrentMillisFixed(dateTime.getMillis());

    assertEquals("20161228t235859", AppEngineUtil.generateVersion());
  }

  @Test
  public void testLeadingZeroDateTimeToVersion() {
    DateTime dateTime = new DateTime(2016, 1, 2, 1, 2 ,3); // 01/02/2016 01:02:03
    DateTimeUtils.setCurrentMillisFixed(dateTime.getMillis());

    assertEquals("20160102t010203", AppEngineUtil.generateVersion());
  }

  @After
  public void tearDown() {
    DateTimeUtils.setCurrentMillisSystem();
  }
}
