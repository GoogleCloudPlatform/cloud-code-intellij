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

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Conditionally registers our {@link CloudDebugRunConfiguration} if our flag is on.
 */
public class DebuggerInitializationComponent implements ApplicationComponent {
  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return "DebuggerInitializationComponent";
  }

  @Override
  public void initComponent() {
    if ("AndroidStudio".equals(PlatformUtils.getPlatformPrefix())) {
      if (CloudDebugConfigType.isFeatureEnabled()) {
        enableCloudDebugger();
      }
    } else {
      enableCloudDebugger();
    }
  }

  private void enableCloudDebugger() {
    Extensions.getRootArea().getExtensionPoint(ConfigurationType.CONFIGURATION_TYPE_EP)
        .registerExtension(new CloudDebugConfigType());
  }
}
