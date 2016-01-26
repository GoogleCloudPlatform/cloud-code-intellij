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
package com.google.gct.idea.debugger.ui;

import com.google.api.services.clouddebugger.model.Debuggee;
import com.google.common.base.Strings;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The class models out the details for a target debuggable module.
 */
class DebugTarget {
  private static final Logger LOG = Logger.getInstance(DebugTarget.class);
  private static final String MODULE = "module";

  private final Debuggee debuggee;
  private String description;
  private long minorVersion = 0;
  private String module;
  private String version;

  public DebugTarget(@NotNull Debuggee debuggee, @NotNull String projectName) {
    this.debuggee = debuggee;
    if (this.debuggee.getLabels() != null) {
      description = "";
      module = "";
      version = "";
      String minorVersion = "";

      //Get the module name, major version and minor version strings.
      for (Map.Entry<String, String> entry : this.debuggee.getLabels().entrySet()) {
        if (entry.getKey().equalsIgnoreCase(MODULE)) {
          module = entry.getValue();
        }
        else if (entry.getKey().equalsIgnoreCase("minorversion")) {
          minorVersion = entry.getValue();
        }
        else if (entry.getKey().equalsIgnoreCase("version")) {
          version = entry.getValue();
        }
        else {
          //This is fallback logic where we dump the labels verbatim if they
          //change from underneath us.
          description += String.format("%s:%s", entry.getKey(), entry.getValue());
        }
      }

      //Build a description from the strings.
      if (!Strings.isNullOrEmpty(module)) {
        description = GctBundle.getString("clouddebug.version.with.module.format",
            module, version);
      }
      else if (!Strings.isNullOrEmpty(version)) {
        description = GctBundle.getString("clouddebug.versionformat", version);
      }

      //Record the minor version.  We only show the latest minor version.
      try {
        if (!Strings.isNullOrEmpty(minorVersion)) {
          this.minorVersion = Long.parseLong(minorVersion);
        }
      }
      catch(NumberFormatException ex) {
        LOG.warn("unable to parse minor version: " + minorVersion);
      }
    }

    //Finally if nothing worked (maybe labels aren't enabled?), we fall
    //back to the old logic of using description with the project name stripped out.
    if (Strings.isNullOrEmpty(description)) {
      description = this.debuggee.getDescription();
      if (description != null &&
          !Strings.isNullOrEmpty(projectName) &&
          description.startsWith(projectName + "-")) {
        description = description.substring(projectName.length() + 1);
      }
    }
  }

  public String getId() {
    return debuggee.getId();
  }

  @Override
  public String toString() {
    return description;
  }

  public long getMinorVersion() {
    return minorVersion;
  }

  public String getModule() {
    return module;
  }

  public String getVersion() {
    return version;
  }
}
