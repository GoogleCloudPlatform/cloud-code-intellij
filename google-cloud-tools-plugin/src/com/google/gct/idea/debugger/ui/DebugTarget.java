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
 * The class models out the details for a target dubuggable module.
 */
class DebugTarget {
  private static final Logger LOG = Logger.getInstance(DebugTarget.class);
  private static final String MODULE = "module";

  private final Debuggee myDebuggee;
  private String myDescription;
  private long myMinorVersion = 0;
  private String myModule;
  private String myVersion;

  public DebugTarget(@NotNull Debuggee debuggee, @NotNull String projectName) {
    myDebuggee = debuggee;
    if (myDebuggee.getLabels() != null) {
      myDescription = "";
      myModule = "";
      myVersion = "";
      String minorVersion = "";

      //Get the module name, major version and minor version strings.
      for (Map.Entry<String, String> entry : myDebuggee.getLabels().entrySet()) {
        if (entry.getKey().equalsIgnoreCase(MODULE)) {
          myModule = entry.getValue();
        }
        else if (entry.getKey().equalsIgnoreCase("minorversion")) {
          minorVersion = entry.getValue();
        }
        else if (entry.getKey().equalsIgnoreCase("version")) {
          myVersion = entry.getValue();
        }
        else {
          //This is fallback logic where we dump the labels verbatim if they
          //change from underneath us.
          myDescription += String.format("%s:%s", entry.getKey(), entry.getValue());
        }
      }

      //Build a description from the strings.
      if (!Strings.isNullOrEmpty(myModule)) {
        myDescription = GctBundle.getString("clouddebug.version.with.module.format",
            myModule, myVersion);
      }
      else if (!Strings.isNullOrEmpty(myVersion)) {
        myDescription = GctBundle.getString("clouddebug.versionformat", myVersion);
      }

      //Record the minor version.  We only show the latest minor version.
      try {
        if (!Strings.isNullOrEmpty(minorVersion)) {
          myMinorVersion = Long.parseLong(minorVersion);
        }
      }
      catch(NumberFormatException ex) {
        LOG.warn("unable to parse minor version: " + minorVersion);
      }
    }

    //Finally if nothing worked (maybe labels aren't enabled?), we fall
    //back to the old logic of using description with the project name stripped out.
    if (Strings.isNullOrEmpty(myDescription)) {
      myDescription = myDebuggee.getDescription();
      if (myDescription != null &&
          !Strings.isNullOrEmpty(projectName) &&
          myDescription.startsWith(projectName + "-")) {
        myDescription = myDescription.substring(projectName.length() + 1);
      }
    }
  }

  public String getId() {
    return myDebuggee.getId();
  }

  @Override
  public String toString() {
    return myDescription;
  }

  public long getMinorVersion() {
    return myMinorVersion;
  }

  public String getModule() {
    return myModule;
  }

  public String getVersion() {
    return myVersion;
  }
}
