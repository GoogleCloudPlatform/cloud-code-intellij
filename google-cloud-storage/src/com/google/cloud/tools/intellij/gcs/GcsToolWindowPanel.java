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

package com.google.cloud.tools.intellij.gcs;

import com.intellij.icons.AllIcons.Actions;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.util.ui.JBUI;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/** Defines a Google Cloud Storage tool panel. */
final class GcsToolWindowPanel extends SimpleToolWindowPanel {

  private static final String GCS_PANEL_TOOLBAR_ACTION = "GcsPanelToolbar";
  private final GcsBucketPanel bucketPanel;

  GcsToolWindowPanel(@NotNull Project project) {
    super(true /*vertical*/, true /*borderless*/);

    bucketPanel = new GcsBucketPanel(project);

    setToolbar(createToolbar());
    setContent(bucketPanel.getComponent());
  }

  private JPanel createToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new RefreshAction());

    ActionToolbar actionToolBar =
        ActionManager.getInstance()
            .createActionToolbar(GCS_PANEL_TOOLBAR_ACTION, group, true /*horizontal*/);

    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  private final class RefreshAction extends DumbAwareAction {
    RefreshAction() {
      super(
          GoogleCloudStorageMessageBundle.message("gcs.panel.toolbar.refresh.hover.text"),
          GoogleCloudStorageMessageBundle.message("gcs.panel.toolbar.refresh.hover.description"),
          Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      bucketPanel.refresh();
    }
  }
}
