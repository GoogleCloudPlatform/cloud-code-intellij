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

package com.google.gct.idea.appengine.cloud;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates and parses individual module listing line items. Used to collect output from the
 * command line and to query the results for the most recently deployed application (module+version
 * combination).
 *
 */
public class AppEngineModuleListOutputParser {
  private static final Logger logger = Logger.getInstance(AppEngineModuleListOutputParser.class);

  private List<AppEngineModuleListItem> moduleDeployTimes = new ArrayList<AppEngineModuleListItem>();

  public void addLineItem(String lineItem) {
    try {
      moduleDeployTimes.add(parseLineItem(lineItem));
    } catch (RuntimeException e) {
      logger.warn(String.format("Unexpected module listing line item format: %s", lineItem), e);
    }
  }

  @Nullable
  public AppEngineModuleListItem getLatestDeployedModule() {
    Collections.sort(moduleDeployTimes);
    return moduleDeployTimes.isEmpty() ? null : moduleDeployTimes.get(moduleDeployTimes.size() - 1);
  }

  /**
   * Given a line from the output of a module listing cli command, parse it into its individual
   * components.
   *
   * Example line: "default  20160325t180009  1.0"
   *
   * @param line from module listing cli command
   * @return @see {@link AppEngineModuleListItem}
   * @throws RuntimeException
   */
  private AppEngineModuleListItem parseLineItem(String line) {
    String[] items = line.split("\\s+");
    String moduleName = items[0];
    String version = items[1];
    String trafficSplit = items[2];

    String[] dateTimeComponents = version.split("t");
    String versionDate = dateTimeComponents[0];
    String versionTime = dateTimeComponents[1];

    return new AppEngineModuleListItem(
        moduleName,
        version,
        versionDate,
        versionTime,
        trafficSplit
    );
  }
}
