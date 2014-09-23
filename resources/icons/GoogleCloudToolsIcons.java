/*
 * Copyright (C) 2013 The Android Open Source Project
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
package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Note: This file should be auto generated once build/scripts/icons.gant is part of CE.
 * http://youtrack.jetbrains.com/issue/IDEA-103558
 */
public class GoogleCloudToolsIcons {
  private static final int STEPS_COUNT = 12;

  public static final Icon AppEngine = load("/icons/appEngine.png"); // 16x16
  public static final Icon Cloud = load("/icons/cloudPlatform.png"); // 16x16
  public static final Icon GoogleTransparent = load("/icons/google.png");
  public static final Icon Refresh = load("/icons/refresh.png");
  public static final Icon SampleImport = load("/icons/sampleImport.png");
  public static final Icon[] StepIcons = findStepIcons("/icons/step_");

  private static Icon load(String path) {
    return IconLoader.getIcon(path, GoogleCloudToolsIcons.class);
  }

  private static Icon[] findStepIcons(String prefix) {
    Icon[] icons = new Icon[STEPS_COUNT];
    for (int i = 0; i <= STEPS_COUNT - 1; i++) {
      icons[i] = IconLoader.getIcon(prefix + (i + 1) + ".png");
    }
    return icons;
  }
}
