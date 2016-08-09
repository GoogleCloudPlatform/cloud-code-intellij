/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A Dialog that prompts a user to disable a plugin.
 */
public class DisablePluginWarningDialog extends DialogWrapper {
  private JLabel promptLabel;
  private JLabel restartLabel;
  private JPanel contentPane;

  private static final int DISABLE_EXIT_CODE = OK_EXIT_CODE;
  private static final int DISABLE_AND_RESTART_EXIT_CODE = NEXT_USER_EXIT_CODE;
  private final boolean myRestartCapable;

  private DisablePluginWarningDialog(@NotNull Component parent, String pluginName,
      boolean restartCapable) {
    super(parent, false);
    myRestartCapable = restartCapable;
    promptLabel.setText(
        GctBundle.message("error.dialog.disable.plugin.prompt", pluginName));
    restartLabel
        .setText(GctBundle.message(restartCapable
            ? "error.dialog.disable.plugin.restart" : "error.dialog.disable.plugin.norestart",
            ApplicationNamesInfo.getInstance().getFullProductName()));

    setTitle(GctBundle.message("error.dialog.disable.plugin.title"));
    init();
  }

  public static void disablePlugin(@NotNull PluginId pluginId, @NotNull Component parentComponent) {
    IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);

    Application app = ApplicationManager.getApplication();
    DisablePluginWarningDialog dialog = new DisablePluginWarningDialog(parentComponent,
        plugin.getName(), app.isRestartCapable());
    dialog.show();
    switch (dialog.getExitCode()) {
      case CANCEL_EXIT_CODE:
        return;
      case DISABLE_EXIT_CODE:
        PluginManagerCore.disablePlugin(pluginId.getIdString());
        break;
      case DISABLE_AND_RESTART_EXIT_CODE:
        PluginManagerCore.disablePlugin(pluginId.getIdString());
        app.restart();
        break;
      default:
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    if (SystemInfo.isMac) {
      if (myRestartCapable) {
        return new Action[]{getCancelAction(), new DisableAction(), new DisableAndRestartAction()};
      } else {
        return new Action[]{getCancelAction(), new DisableAction()};
      }
    } else {
      if (myRestartCapable) {
        return new Action[]{new DisableAction(), new DisableAndRestartAction(), getCancelAction()};
      } else {
        return new Action[]{new DisableAction(), getCancelAction()};
      }
    }
  }

  private class DisableAction extends DialogWrapperAction {
    protected DisableAction() {
      super(GctBundle.message("error.dialog.disable.plugin.action.disable"));
    }

    @Override
    protected void doAction(ActionEvent event) {
      close(DISABLE_EXIT_CODE);
    }
  }

  private class DisableAndRestartAction extends DialogWrapperAction {
    protected DisableAndRestartAction() {
      super(GctBundle.message("error.dialog.disable.plugin.action.disableAndRestart"));
    }

    @Override
    protected void doAction(ActionEvent event) {
      close(DISABLE_AND_RESTART_EXIT_CODE);
    }
  }
}
