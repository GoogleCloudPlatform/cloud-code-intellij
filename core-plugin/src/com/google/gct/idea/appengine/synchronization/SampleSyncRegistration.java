/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.synchronization;

import com.intellij.openapi.components.ApplicationComponent;

import org.jetbrains.annotations.NotNull;

/**
 * Initializes the {@link SampleSyncScheduler} to officiate sample templates synchronization.
 * Registered in the <application-components> section of the plugin.xml file,
 * to be called on start-up.
 */
public class SampleSyncRegistration  implements ApplicationComponent {
  @Override
  public void initComponent() {
    SampleSyncScheduler.getInstance().startScheduleTask();
  }

  @Override
  public void disposeComponent() {
    // do nothing
  }

  @NotNull
  @Override
  public String getComponentName() {
    return  "Sample Template Synchronization";
  }
}
