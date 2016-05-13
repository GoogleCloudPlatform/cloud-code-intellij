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

package com.google.cloud.tools.intellij.debugger.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Opens the Cloud Debugger help page.
 */
public class CloudDebugHelpAction extends AnAction {

  private static final Icon icon = IconLoader.getIcon("/actions/help.png");
  private String url;

  public CloudDebugHelpAction(String url) {
    this.url = url;
  }

  public void actionPerformed(AnActionEvent event) {
    openUrl();
  }

  /**
   * Opens the URL in a browser with BrowserUtil.
   */
  protected void openUrl() {
    BrowserUtil.browse(url);
  }

  /**
   * Sets the help button's icon and label text.
   */
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    presentation.setIcon(icon);
    presentation.setText(CommonBundle.getHelpButtonText());
  }
}
