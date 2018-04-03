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

package com.google.cloud.tools.intellij.apis;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Created by nbashirbello on 4/2/18. */
public class ServiceAccountKeyUltimateDisplayDialog extends DialogWrapper {
  private final Project project;
  private final String gcpProjectId;
  private final String downloadPath;
  private JPanel panel1;
  private JTable serverTable;
  private ServiceAccountKeyDownloadedPanel commonPanel;

  public ServiceAccountKeyUltimateDisplayDialog(
      @Nullable Project project, @NotNull String gcpProjectId, @NotNull String downloadPath) {
    super(project);
    this.project = project;
    this.gcpProjectId = gcpProjectId;
    this.downloadPath = downloadPath;
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel1;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction()};
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(project, gcpProjectId, downloadPath);
  }
}
