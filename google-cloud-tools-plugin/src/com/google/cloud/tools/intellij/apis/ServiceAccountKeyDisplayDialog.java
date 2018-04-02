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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.util.GctBundle;
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
  final private Project project;
  final private String gcpProjectId;
  final private String downloadPath;
  private JPanel panel;
  private ServiceAccountKeyDownloadedPanel keyDownloadedPanel;

  ServiceAccountKeyDisplayDialog(@Nullable Project project, String gcpProjectId, String downloadPath) {
    super(project);
    this.project = project;
    this.gcpProjectId = gcpProjectId;
    this.downloadPath = downloadPath;
    init();
    setTitle(GctBundle.message("cloud.apis.service.account.key.downloaded.title"));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  private void createUIComponents() {
    keyDownloadedPanel = new ServiceAccountKeyDownloadedPanel(project, gcpProjectId, downloadPath);
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction()};
  }
}
