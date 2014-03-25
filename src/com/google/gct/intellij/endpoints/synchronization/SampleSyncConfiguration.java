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
package com.google.gct.intellij.endpoints.synchronization;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Sets and returns the setting for using built-in samples
 */
public class SampleSyncConfiguration {
  public static boolean usingBuiltInSamples() {
    return PropertiesComponent.getInstance().getBoolean("built.in.samples.templates", false);
  }

  public static void setUseBuiltInSamples(boolean useBuiltInSamples) {
    PropertiesComponent.getInstance().setValue("built.in.samples.templates", String.valueOf(useBuiltInSamples));
  }
}
