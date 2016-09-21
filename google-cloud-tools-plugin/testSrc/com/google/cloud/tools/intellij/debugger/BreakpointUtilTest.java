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

package com.google.cloud.tools.intellij.debugger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * {@link BreakpointUtil} unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class BreakpointUtilTest {
  @Test
  public void testParseDateTime_iso8601() {
    Calendar gregorian = new GregorianCalendar(2016, 8, 21, 16, 39, 0);
    gregorian.setTimeZone(TimeZone.getTimeZone("America/New York"));
    assertEquals(gregorian.getTime(), BreakpointUtil.parseDateTime("2016-09-21T16:39:00.000Z"));
  }

  @Test
  public void testParseDateTime_iso8601NoMs() {
    Calendar gregorian = new GregorianCalendar(2016, 8, 21, 16, 39, 0);
    gregorian.setTimeZone(TimeZone.getTimeZone("America/New York"));
    assertEquals(gregorian.getTime(), BreakpointUtil.parseDateTime("2016-09-21T16:39:00Z"));
  }

  @Test
  public void testParseDateTime_unknownFormat() {
    assertNull(BreakpointUtil.parseDateTime("this is not a date"));
  }
}
