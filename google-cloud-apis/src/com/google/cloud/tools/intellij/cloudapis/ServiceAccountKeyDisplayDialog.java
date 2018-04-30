/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming the download of the service account JSON key with information on how to set the
 * credential environment variables for local run.
 */
public class ServiceAccountKeyDisplayDialog extends DialogWrapper {
  private final CloudProject cloudProject;
  private final String downloadPath;
  private JPanel panel;
  private ServiceAccountKeyDownloadedPanel commonPanel;

  ServiceAccountKeyDisplayDialog(
      @Nullable Project project, CloudProject cloudProject, String downloadPath) {
    super(project);
    this.cloudProject = cloudProject;
    this.downloadPath = downloadPath;
    init();
    setTitle(
        GoogleCloudApisMessageBundle.message("cloud.apis.service.account.key.downloaded.title"));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(cloudProject.projectId(), downloadPath);
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction()};
  }
}
