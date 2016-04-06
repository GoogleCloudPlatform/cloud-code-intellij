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
package com.google.cloud.tools.intellij.debugger.ui;

import static org.junit.Assert.assertEquals;

import com.google.api.services.clouddebugger.v2.model.Debuggee;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DebugTargetTest {

  private static final String MODULE = "module";
  private static final String VERSION = "version";

  private static final String DEFAULT_MODULE_NAME = "Default";

  private static final String MODULE_VERSION_FORMAT = "%s (Version:%s)";

  @Test
  public void testDefaultModuleTargetLabel() {
    String version = "1";
    DebugTarget target = new DebugTarget(createDebuggee(/* module */ null, version), "projectname");

    assertEquals(String.format(MODULE_VERSION_FORMAT, DEFAULT_MODULE_NAME, version),
        target.getDescription());
    assertEquals(DEFAULT_MODULE_NAME, target.getModule());
  }

  @Test
  public void testNonDefaultModuleTargetLabel() {
    String module = "myModule";
    String version = "1";
    DebugTarget target = new DebugTarget(createDebuggee(module, version), "projectname");

    assertEquals(String.format(MODULE_VERSION_FORMAT, module, version),
        target.getDescription());
    assertEquals(module, target.getModule());
  }

  private Debuggee createDebuggee(String module, String version) {
    Debuggee defaultDebuggee = new Debuggee();

    Map<String, String> labels = new HashMap<String, String>();
    if(module != null) {
      labels.put(MODULE, module);
    }
    labels.put(VERSION, version);

    defaultDebuggee.setLabels(labels);
    return defaultDebuggee;
  }
}
