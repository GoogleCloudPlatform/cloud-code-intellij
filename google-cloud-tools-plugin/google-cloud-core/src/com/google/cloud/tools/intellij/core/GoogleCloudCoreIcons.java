/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.util.IconLoader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * Icons that are used commonly across the Cloud Tools plugin feature-set.
 *
 * TODO: Look into auto-generation: http://youtrack.jetbrains.com/issue/IDEA-103558
 */
public class GoogleCloudCoreIcons {

  public static final Icon REFRESH = load("/icons/refresh.png");
  private static final int STEPS_COUNT = 12;
  public static final ImmutableList<Icon> STEP_ICONS = findStepIcons("/icons/step_");
  public static final Icon LOADING = loadGif("/icons/loading.gif");

  private GoogleCloudCoreIcons() {
    // Not for instantiation.
  }

  private static Icon load(String path) {
    return IconLoader.getIcon(path, GoogleCloudCoreIcons.class);
  }

  private static Icon loadGif(String path) {
    return new ImageIcon(GoogleCloudCoreIcons.class.getResource(path));
  }

  private static ImmutableList<Icon> findStepIcons(String prefix) {
    List<Icon> icons = new ArrayList<>(STEPS_COUNT);
    for (int i = 0; i < STEPS_COUNT; i++) {
      icons.add(IconLoader.getIcon(prefix + (i + 1) + ".png"));
    }
    return new ImmutableList.Builder<Icon>().addAll(icons).build();
  }
}
