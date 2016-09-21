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

package com.google.cloud.tools.intellij.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.StatusMessage;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.collect.ImmutableList;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * Utility functions for cloud debug data.
 */
public class BreakpointUtil {

  private static final Logger LOG = Logger.getInstance(BreakpointUtil.class);

  // 2015-07-23T16:37:33.000Z
  public static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  // TODO(joaomartins): Check with API team on when the rollout to the NO_MS format is done,
  // so we can remove the ISO_8601_FORMAT check.
  // 2015-07-23T16:37:33Z
  public static final String ISO_8601_FORMAT_NO_MS = "yyyy-MM-dd'T'HH:mm:ssZ";
  public static final Collection<String> FORMATS =
      ImmutableList.of(ISO_8601_FORMAT_NO_MS, ISO_8601_FORMAT);

  /**
   * This is a helper routine that converts a server {@link StatusMessage} to descriptive text.
   */
  @Nullable
  public static String getUserErrorMessage(@Nullable StatusMessage statusMessage) {
    if (statusMessage == null || !Boolean.TRUE.equals(statusMessage.getIsError())) {
      return null;
    }

    String errorDescription = getUserMessage(statusMessage);
    return !Strings.isNullOrEmpty(errorDescription) ? errorDescription
        : GctBundle.getString("clouddebug.fallbackerrormessage");
  }

  /**
   * Formats and returns the user message.
   */
  @Nullable
  public static String getUserMessage(@Nullable StatusMessage statusMessage) {
    if (statusMessage != null && statusMessage.getDescription() != null) {
      String formatString = statusMessage.getDescription().getFormat();
      Integer idx = 0;
      // Parameters in the server version are encoded script style with '$'.
      String argString = "$" + idx.toString();
      while (formatString.contains(argString)) {
        formatString = formatString.replace(argString, "%s");
        idx++;
        argString = "$" + idx.toString();
      }
      return String.format(formatString, statusMessage.getDescription().getParameters());
    }
    return null;
  }

  /**
   * Parses a date time string to a {@link java.util.Date}.
   */
  @Nullable
  public static Date parseDateTime(@Nullable String dateString) {
    if (dateString == null) {
      return null;
    }

    dateString = dateString.replaceAll("Z$", "-0000");

    SimpleDateFormat dateFormat;

    for (String format : FORMATS) {
      try {
        dateFormat = new SimpleDateFormat(format);
        return dateFormat.parse(dateString);
      } catch (ParseException pe) {
        LOG.warn("error parsing datetime " + dateString + " with format " + format);
      }
    }
    LOG.error("datetime " + dateString + "couldn't be parsed by any formats.");

    return null;
  }
}
