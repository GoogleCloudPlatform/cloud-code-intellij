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

package com.google.cloud.tools.intellij.ui;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

/**
 * Note: This file should be auto generated once build/scripts/icons.gant is part of CE.
 * http://youtrack.jetbrains.com/issue/IDEA-103558
 */
public final class GoogleCloudToolsIcons {

  public static final Icon APP_ENGINE = load("/icons/appEngine.png"); // 16x16
  public static final Icon CLOUD = load("/icons/cloudPlatform.png");
  public static final Icon CLOUD_STORAGE = load("/icons/cloudStorage.png");
  public static final Icon CLOUD_STORAGE_BUCKET = load("/icons/cloudStorageBucket.png");
  public static final Icon CLOUD_BREAKPOINT_FINAL = load("/icons/cloudsnapshotfinal.png");
  public static final Icon CLOUD_BREAKPOINT = load("/icons/cloudbreakpoint.png");
  public static final Icon CLOUD_BREAKPOINT_CHECKED = load("/icons/cloudbreakpointchecked.png");
  public static final Icon CLOUD_BREAKPOINT_ERROR = load("/icons/cloudbreakpointerror.png");
  public static final Icon CLOUD_BREAKPOINT_DISABLED = load("/icons/cloudbreakpointdisabled.png");
  public static final Icon CLOUD_DEBUG_SAVE_EXIT = load("/icons/debugsaveexit.png");
  public static final Icon CLOUD_DEBUG_REACTIVATE_BREAKPOINT =
      load("/icons/debugreactivatebreakpoint.png");
  public static final Icon CLOUD_DEBUG_DELETE_ALL_BREAKPOINTS = load("/icons/debugdeleteall.png");
  public static final Icon STACKDRIVER_DEBUGGER = load("/icons/stackdriverdebugger.png");
  public static final Icon FEEDBACK = load("/icons/cloudtoolsfeedback.png");

  private static Icon load(String path) {
    return IconLoader.getIcon(path, GoogleCloudToolsIcons.class);
  }

  private GoogleCloudToolsIcons() {
    // This utility class should not be instantiated.
  }
}
