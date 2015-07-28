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
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * Project Selector listener to select project when project is clicked in Project Selector component
 * Created by amulyau on 6/24/15.
 */
public class ProjectSelectorListener extends DocumentAdapter {

  private final AppEngineLogging controller;
  private final AppEngineLogToolWindowView view;
  private final Project project;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   * @param project Current application project (not app engine project)
   */
  public ProjectSelectorListener(AppEngineLogging controller, AppEngineLogToolWindowView view,
                                 Project project) {
    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  @Override
  protected void textChanged(DocumentEvent e) {
    view.setCurrProject(); //get the project ID of the app engine project
    String currProject = view.getCurrProject();

    if ((currProject != null)  && (!currProject.equals(view.getPrevProject()))) { //gcp project
      view.setTreeRootVisible();
      view.clearComboBoxes();//remove previous filters
      view.setPrevProject(); //sets new project as prev project

      view.setEnabledModuleComboBox();
      view.setEnabledVersionComboBox();

      Task.Backgroundable logTask = new Task.Backgroundable(project, "Getting Modules, " +
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

          controller.createConnection(); //make sure of proper user credentials and make connection
          view.setCurrentAppID();

          if (controller.isNotConnected()) { //no current app set means error with create connection
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                view.resetComponents();
                view.setRootText(view.ERROR_PROJECT_DID_NOT_CONNECT);
              }
            });

          } else {

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

                while (view.getCurrPage() != -1) { //get to first page when we refresh logs
                  view.decreasePage();
                }
                progressIndicator.setFraction(0.66);
                progressIndicator.setText("33% to finish");
                ListLogEntriesResponse logResp = controller.getLogs();

                progressIndicator.setFraction(0.90);
                progressIndicator.setText("10% to finish");
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
        }
      };
      logTask.queue();
    } else if ((currProject != null) && (currProject.equals(view.getPrevProject()))) {
      //same as previous selection
    } else if ((currProject == null) && (view.getPrevProject() == null)){
      //don't do anything because they had previously errored
    } else if (currProject == null) {
      //invalid current project
    } else { //not valid project name => error
      view.setPrevProject();
      view.setRootText(view.ERROR_PROJECT_NOT_EXIST);
      view.setModuleALActive(false);
      view.setVersionALActive(false);
      view.resetComponents();
      view.setModuleALActive(true);
      view.setVersionALActive(true);
    }
  }

}
