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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * {@link BreakpointUtil} unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class BreakpointUtilTest {
  @Test
  public void testParseDateTime_iso8601() {
    DateTime date = new DateTime(2016, 9, 21, 16, 39, 0, DateTimeZone.UTC);
    assertEquals(date.toDate(), BreakpointUtil.parseDateTime("2016-09-21T16:39:00.000Z"));
  }

  @Test
  public void testParseDateTime_iso8601NoMs() {
    DateTime date = new DateTime(2016, 9, 21, 16, 39, 0, DateTimeZone.UTC);
    assertEquals(date.toDate(), BreakpointUtil.parseDateTime("2016-09-21T16:39:00Z"));
  }

  @Test(expected = AssertionError.class)
  public void testParseDateTime_unknownFormat() {
    BreakpointUtil.parseDateTime("this is not a date");
  }
}
