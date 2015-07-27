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

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Versions Combo Box listener for AppEngineLogToolWindowView
 * Created by amulyau on 6/16/15.
 */
public class VersionsActionListener implements ActionListener {

  /**Controller for App Engine Logs*/
  private final AppEngineLogging controller;

  /**View for the App Engine Logs*/
  private final AppEngineLogToolWindowView view;

  /**Current application project (not app engine project)*/
  private final Project project;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs with the versions combo box
   * @param project Current application project (not app engine project)
   */
  public VersionsActionListener(AppEngineLogging controller, AppEngineLogToolWindowView view,
                                Project project) {

    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  /**
   * Action Listener class that verifies ver and gets modules list, versions list and logs
   * based on that information
   * @param e Even that occurs
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    //gets previous version selection to compare with current version selection
    String prevVersionSelection = view.getCurrVersion();
    view.setCurrVersion();

    String currVersion = view.getCurrVersion();

    if (currVersion == null) {//do not do anything if nothing is there
    } else if ((prevVersionSelection != null) && (currVersion.equals(prevVersionSelection))) {
      //same selection as previous selection so do nothing
    } else {
      if (view.getVersionALActive()) { //solve combobox touchiness problems
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

            while (view.getCurrPage() != -1) { //get to first page when we refresh logs
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
        logTask.queue();
      }
    }
  }

}
