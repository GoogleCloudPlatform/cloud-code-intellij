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
package com.google.gct.idea.appengine.gradle.notification;

import com.android.tools.idea.gradle.invoker.GradleInvoker;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.gradle.util.Projects;
import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;

/**
 * Notification for invalid SDK paths. Displays error message when user's Facet configuration contains
 * a reference to a non-existent App Engine SDK path.  It offers two corrective measures
 * 1. download via gradle
 * 2. open build.gradle file so the user may configure it correctly
 */
public class AppEngineStatusNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  private static final Key<EditorNotificationPanel> KEY = Key.create("appengine.sdk.status");

  @NotNull private final Project myProject;

  public AppEngineStatusNotificationProvider(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (!Projects.isBuildWithGradle(myProject)) {
      return null;
    }
    Module module = ModuleUtilCore.findModuleForFile(file, myProject);
    if (module == null) {
      return null;
    }
    AppEngineGradleFacet facet = AppEngineGradleFacet.getInstance(module);
    if(facet != null) {
      AppEngineConfigurationProperties state = facet.getConfiguration().getState();
      if (state != null && !(new File(state.APPENGINE_SDKROOT).exists())) {
        return new DownloadSdkNotification(module);
      }
    }
    return null;
  }

  private class DownloadSdkNotification extends EditorNotificationPanel {

    DownloadSdkNotification(@NotNull Module module) {
      setText(GctBundle.message("appengine.sdk.invalid",module.getName()));

      createActionLabel(GctBundle.message("appengine.sdk.gradle.install"), new Runnable() {
        @Override
        public void run() {
          GradleInvoker.getInstance(myProject).executeTasks(Collections.singletonList("appengineDownloadSdk"));
        }
      });

      final VirtualFile buildFile = GradleUtil.getGradleBuildFile(module);
      if (buildFile != null && buildFile.exists()) {
        createActionLabel(GctBundle.message("appengine.sdk.open.gradle.build"), new Runnable() {
          @Override
          public void run() {
            FileEditorManager.getInstance(myProject).openFile(buildFile, true);
          }
        });
      }
    }
  }
}
