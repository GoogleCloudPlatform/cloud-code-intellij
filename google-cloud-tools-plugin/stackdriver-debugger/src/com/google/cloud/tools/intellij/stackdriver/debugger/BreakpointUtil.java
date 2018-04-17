/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.StatusMessage;
import org.jetbrains.annotations.Nullable;

/** Utility functions for cloud debug data. */
public class BreakpointUtil {

  /** This is a helper routine that converts a server {@link StatusMessage} to descriptive text. */
  @Nullable
  public static String getUserErrorMessage(@Nullable StatusMessage statusMessage) {
    if (statusMessage == null || !Boolean.TRUE.equals(statusMessage.getIsError())) {
      return null;
    }

    String errorDescription = getUserMessage(statusMessage);
    return !Strings.isNullOrEmpty(errorDescription)
        ? errorDescription
        : StackdriverDebuggerBundle.getString("clouddebug.fallbackerrormessage");
  }

  /** Formats and returns the user message. */
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
}
