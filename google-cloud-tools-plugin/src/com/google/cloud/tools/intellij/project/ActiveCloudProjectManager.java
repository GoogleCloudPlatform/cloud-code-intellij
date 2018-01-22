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

package com.google.cloud.tools.intellij.project;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds active cloud project, one per IDE project. Active cloud project is the last cloud project
 * selected by a user in most of the GCP related UI reported by {@link ProjectSelector}.
 *
 * <p>Must only be used on Swing/IDE EDT.
 */
public class ActiveCloudProjectManager {
  private static final String PROJECT_ACCOUNT_KEY = "ACTIVE_CLOUD_PROJECT_ACCOUNT";
  private static final String PROJECT_NAME_KEY = "ACTIVE_CLOUD_PROJECT_NAME";
  private static final String PROJECT_ID_KEY = "ACTIVE_CLOUD_PROJECT_ID";
  private static final String PROJECT_NUMBER_KEY = "ACTIVE_CLOUD_PROJECT_NUMBER";

  private static ActiveCloudProjectManager instance = new ActiveCloudProjectManager();

  static ActiveCloudProjectManager getInstance() {
    return instance;
  }

  @VisibleForTesting
  static void setInstance(ActiveCloudProjectManager instance) {
    ActiveCloudProjectManager.instance = instance;
  }

  void setActiveCloudProject(
      @NotNull CloudProject activeCloudProject, @NotNull Project ideProject) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(ideProject);

    propertiesComponent.setValue(PROJECT_ACCOUNT_KEY, activeCloudProject.googleUsername());
    propertiesComponent.setValue(PROJECT_NAME_KEY, activeCloudProject.projectName());
    propertiesComponent.setValue(PROJECT_ID_KEY, activeCloudProject.projectId());
    if (activeCloudProject.projectNumber() != null) {
      propertiesComponent.setValue(
          PROJECT_NUMBER_KEY, activeCloudProject.projectNumber().toString());
    }
  }

  @Nullable
  CloudProject getActiveCloudProject(@NotNull Project ideProject) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(ideProject);

    String projectAccount = propertiesComponent.getValue(PROJECT_ACCOUNT_KEY);
    String projectName = propertiesComponent.getValue(PROJECT_NAME_KEY);
    String projectId = propertiesComponent.getValue(PROJECT_ID_KEY);
    String projectNumberString = propertiesComponent.getValue(PROJECT_NUMBER_KEY);

    // we only need ID and account to show project in case it was not saved completely.
    if (!Strings.isNullOrEmpty(projectAccount) && !Strings.isNullOrEmpty(projectId)) {
      // null project number is acceptable
      Long projectNumber = Longs.tryParse(Strings.nullToEmpty(projectNumberString));

      return CloudProject.create(projectName, projectId, projectNumber, projectAccount);
    }

    return null;
  }
}
