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

package com.google.cloud.tools.intellij.gcs;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/** Factory class for the Google Cloud Storage tool window panel. */
public final class GcsToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    GcsToolWindowPanel gcsToolWindowPanel = new GcsToolWindowPanel();
    ContentManager contentManager = toolWindow.getContentManager();
    Content content =
        contentManager
            .getFactory()
            .createContent(gcsToolWindowPanel, null /*displayName*/, false /*isLockable*/);
    contentManager.addContent(content);
  }

  @Override
  public void init(ToolWindow window) {
    window.setIcon(GoogleCloudToolsIcons.CLOUD);
  }
}
