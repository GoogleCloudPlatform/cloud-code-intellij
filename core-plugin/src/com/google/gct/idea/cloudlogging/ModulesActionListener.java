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
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Modules Combo Box Listener for AppEngineLogToolWindowView
 * Created by amulyau on 6/16/15.
 */
public class ModulesActionListener implements ActionListener {

  /**Controller for App Engine Logs*/
  private final AppEngineLogging controller;

  /**View for the App Engine Logs*/
  private final AppEngineLogToolWindowView view;

  /**Current application project (not app engine project)*/
  private final Project project;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs with the Modules Combo Box
   * @param project Current application project (not app engine project)
   */
  public ModulesActionListener(AppEngineLogging controller, AppEngineLogToolWindowView view, Project project) {

    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  /**
   * Action Listener class that verifies modules and get the versions list and logs
   * based on that information
   * @param e Even that occurs
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    String prevModuleSelection = view.getCurrModule();
    view.setCurrModule();
    String currModule = view.getCurrModule();

    if (currModule == null) { //do not do anything if nothing is there/selected
    } else if ((prevModuleSelection != null) && (currModule.equals(prevModuleSelection))) {
      //same selection as previous selection
    } else {
      if (view.getModuleALActive()) { //solve combobox touchiness problems
        Task.Backgroundable logTask = new Task.Backgroundable(project, "Getting Versions " +
            "and Logs List", false, new PerformInBackgroundOption() {

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
            final ArrayList<String> versionsList = view.processVersionsList(controller
                .getVersionsList());

            progressIndicator.setFraction(0.33);
            progressIndicator.setText("66% to finish");
            if (versionsList != null) {
              progressIndicator.setFraction(0.45);
              progressIndicator.setText("55% to finish");

              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  view.setVersionsList(versionsList);
                }
              });

              view.clearPageTokens();
              while (view.getCurrPage() != -1) { //get to first page when we get new logs
                view.decreasePage();
              }
              progressIndicator.setFraction(0.66);
              progressIndicator.setText("33% to finish");
              ListLogEntriesResponse logResp = controller.getLogs();

              view.threadProcessAndSetLogs(logResp);
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
          }
        };
        logTask.run(new ProgressWindow(false, project));

      }
    }
  }

}
