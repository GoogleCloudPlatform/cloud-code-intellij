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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import icons.GoogleCloudToolsIcons;

/**
 * Action on the left Side tool bar of the tool window that controls the time order of logs
 * Created by amulyau on 7/10/15.
 */
public class TimeOrderAction extends ToggleAction {

  /**View for the App Engine Logs*/
  private AppEngineLogToolWindowView view;
  /**Controller for App Engine Logs*/
  private AppEngineLogging controller;
  /**Current application project (not app engine project)*/
  private Project project;

  /**Empty Constructor*/
  public TimeOrderAction() {}

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   * @param project Current application project (not app engine project)
   */
  public TimeOrderAction(AppEngineLogging controller, AppEngineLogToolWindowView view,
                         Project project) {

    super("Ascending", "Ascending Logs", GoogleCloudToolsIcons.ASCENDING_LOGS);
    this.view = view;
    this.controller = controller;
    this.project = project;
  }

  @Override
  public boolean isSelected(AnActionEvent e) {

    return view.getTimeOrder();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {

    if (state) { //if selected, time asc
      view.changeTimeOrder(true);
    } else {
      view.changeTimeOrder(false);
    }

    Task.Backgroundable logTask = new Task.Backgroundable(project, "Getting Logs List",
        false, new PerformInBackgroundOption() {

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
        view.clearPageTokens();

        while(view.getCurrPage() > -1) {
          view.decreasePage();
        }
        progressIndicator.setFraction(0.33);
        progressIndicator.setText("66% to finish");

        ListLogEntriesResponse logResp = controller.getLogs();
        progressIndicator.setFraction(0.66);
        progressIndicator.setText("33% to finish");

        view.threadProcessAndSetLogs(logResp);
      }
    };
    logTask.run(new ProgressWindow(false, project));
  }

}
