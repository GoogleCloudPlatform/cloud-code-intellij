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
import com.google.api.services.debugger.model.StatusMessage;
import com.google.gct.idea.util.GctBundle;
import org.jetbrains.annotations.Nullable;

/**
 * Utility functions for cloud debug data.
 */
public class BreakpointUtil {
  /**
   * This is a helper routine that converts a server {@link StatusMessage} to descriptive text.
   */
  @Nullable
  public static String getUserErrorMessage(@Nullable StatusMessage statusMessage) {
    if (statusMessage == null || statusMessage.getIsError() != Boolean.TRUE) {
      return null;
    }

    String errorDescription = getUserMessage(statusMessage);
    return !Strings.isNullOrEmpty(errorDescription) ? errorDescription
        : GctBundle.getString("clouddebug.fallbackerrormessage");
  }

  @Nullable
  public static String getUserMessage(@Nullable StatusMessage statusMessage) {
    if (statusMessage != null && statusMessage.getDescription() != null) {
      String formatString = statusMessage.getDescription().getFormat();
      Integer i = 0;
      // Parameters in the server version are encoded script style with '$'.
      String argString = "$" + i.toString();
      while (formatString.contains(argString)) {
        formatString = formatString.replace(argString, "%s");
        i++;
        argString = "$" + i.toString();
      }
      return String.format(formatString, statusMessage.getDescription().getParameters());
    }
    return null;
  }
}
