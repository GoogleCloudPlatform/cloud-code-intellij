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
package com.google.gct.idea.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.SourceLocation;

import java.util.Comparator;
import java.util.Date;

/**
 * BreakpointComparer is a comparer used to sort breakpoints in the historical snapshot list.
 */
public class BreakpointComparer implements Comparator<Breakpoint> {
  private static final BreakpointComparer DEFAULT_INSTANCE = new BreakpointComparer();
  private static final Date MINIMUM_DATE = new Date(Long.MIN_VALUE);
  private BreakpointComparer() {
  }

  public static BreakpointComparer getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public int compare(Breakpoint o1, Breakpoint o2) {
    if (o2.getFinalTime() == null && o1.getFinalTime() != null) {
      return 1;
    }
    if (o2.getFinalTime() != null && o1.getFinalTime() == null) {
      return -1;
    }
    if (o2.getFinalTime() == null && o1.getFinalTime() == null) {
      //compare file and line
      SourceLocation s1 = o1.getLocation();
      SourceLocation s2 = o2.getLocation();
      boolean s1Valid = isSourceLocationValid(s1);
      boolean s2Valid = isSourceLocationValid(s2);
      if (!s1Valid && !s2Valid) {
        return 0;
      }
      if (s1Valid && !s2Valid) {
        return -1;
      }
      if (!s1Valid && s2Valid) {
        return 1;
      }
      if (s1.getPath().equals(s2.getPath())) {
        long s1Line = toLongValue(s1.getLine().longValue());
        long s2Line = toLongValue(s2.getLine().longValue());
        if (s1Line > s2Line) {
          return 1;
        }
        if (s1Line < s2Line) {
          return -1;
        }
        return 0;
      }
      return s1.getPath().compareTo(s2.getPath());
    }
    Date d1 = BreakpointUtil.parseDateTime(o1.getFinalTime());
    Date d2 = BreakpointUtil.parseDateTime(o2.getFinalTime());
    if (d1 == null) {
      d1 = MINIMUM_DATE;
    }
    if (d2 == null) {
      d2 = MINIMUM_DATE;
    }
    return d2.compareTo(d1);
  }

  private static boolean isSourceLocationValid(SourceLocation sourceLocation) {
    if (sourceLocation == null) {
      return false;
    }
    if (Strings.isNullOrEmpty(sourceLocation.getPath())) {
      return false;
    }
    if (sourceLocation.getLine() == null) {
      return false;
    }
    return true;
  }

  private static int toIntValue(Integer integer) {
    return integer != null ? integer.intValue() : 0;
  }

  private static long toLongValue(Long longValue) {
    return longValue != null ? longValue.longValue() : 0;
  }
}
