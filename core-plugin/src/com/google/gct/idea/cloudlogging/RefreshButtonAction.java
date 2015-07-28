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
package com.google.gct.idea.cloudlogging;

import com.google.api.services.logging.model.ListLogEntriesResponse;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import javax.swing.*;

/**
 * Refresh Button to refresh the logs
 * Created by amulyau on 5/29/15.
 */
public class RefreshButtonAction extends AnAction {

  private AppEngineLogging controller;
  private AppEngineLogToolWindowView view;
  private Project project;

  /**
   * Empty Constructor for adding as an action to plugin.xml
   */
  public RefreshButtonAction() {}

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   * @param project Current application project (not app engine project)
   */
  public RefreshButtonAction(AppEngineLogging controller, AppEngineLogToolWindowView view,
                             Project project) {
    super("Refresh", "Refresh the App Engine Logs", AllIcons.Actions.Refresh);
    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  /**
   * Refreshes the logs as long as a project is selected (implies module and version are also set)
   * @param e Click event that occurs
   */
  @Override
  public void actionPerformed(AnActionEvent e) {
    if (view.getCurrProject() != null) {
      final String currModule = view.getCurrModule();
      final String currVersion = view.getCurrVersion();

      Task.Backgroundable logTask = new Task.Backgroundable(project, "Refreshing Modules, " +
          "Versions and Logs List", false, new PerformInBackgroundOption() {
        @Override
        public boolean shouldStartInBackground() {
          return true;
        }
        @Override
        public void processSentToBackground() {}
      }) {

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
          progressIndicator.setFraction(0.10);
          progressIndicator.setText("90% to finish");
          final ArrayList<String> modulesList = view.processModulesList(controller
              .getModulesList());

          if (modulesList != null) {
            progressIndicator.setFraction(0.20);
            progressIndicator.setText("80% to finish");
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                view.setModulesList(modulesList);
              }
            });

            progressIndicator.setFraction(0.33);
            progressIndicator.setText("66% to finish");
            final ArrayList<String> versionsList = view.processVersionsList(controller
                .getVersionsList());

            if (versionsList != null) {
              progressIndicator.setFraction(0.45);
              progressIndicator.setText("55% to finish");
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  view.setVersionsList(versionsList);
                }
              });

              progressIndicator.setFraction(0.66);
              progressIndicator.setText("33% to finish");
              ListLogEntriesResponse logResp = controller.getLogs();
              view.clearPageTokens();

              while (view.getCurrPage() != -1) { //get to first page when we refresh logs
                view.decreasePage();
              }
              if ((logResp != null) && (logResp.getEntries() != null)) {
                view.processLogs(logResp);

                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    view.setLogs();
                    view.setCurrModuleToModuleComboBox(currModule);
                    view.setCurrVersionToVersionComboBox(currVersion);
                  }
                });

                progressIndicator.setFraction(0.90);
                progressIndicator.setText("10% to finish");
              } else {
                progressIndicator.setFraction(0.90);
                progressIndicator.setText("10% to finish");
                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    view.setRootText(view.NO_LOGS_LIST_STRING);
                  }
                });
              }
            } else {
              progressIndicator.setFraction(0.90);
              progressIndicator.setText("10% to finish");
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  view.setRootText(view.NO_VERSIONS_LIST_STRING);
                }
              });
            }
          } else {
            progressIndicator.setFraction(0.90);
            progressIndicator.setText("10% to finish");
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                view.setRootText(view.NO_MODULES_LIST_STRING);
              }
            });

          }
        }
      };
      logTask.queue();
    }
  }

  /**
   * Makes sure the button is available to click
   * @param e Action event
   */
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(true);
  }

}
