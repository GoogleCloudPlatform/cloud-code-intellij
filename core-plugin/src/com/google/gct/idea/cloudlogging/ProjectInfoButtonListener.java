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

import com.intellij.openapi.ui.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by amulyau on 6/16/15.
 */
public class ProjectInfoButtonListener implements ActionListener {

  /**Controller for App Engine Logs*/
  AppEngineLogging controller;

  /**View for the App Engine Logs*/
  AppEngineLogToolWindowView view;

  private String ERROR_PROJECT_NOT_EXIST = "Error: Project entered does not exist, " +
                                           "please try again";
  private String ERROR_DIALOG_TITLE = "Error";

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs with the Project Info Button
   */
  public ProjectInfoButtonListener(AppEngineLogging controller, AppEngineLogToolWindowView view){

    this.controller=controller;
    this.view = view;
  }

  /**
   * Action Listener class that verifies project and gets modules list, versions list and logs
   * based on that information
   * @param e Even that occurs
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    //get the project ID of the app engine project
    view.setCurrProject();
    String currProject = view.getCurrProject();

    //proper project (in app engine) selected and not same as previous selection
    if((currProject!=null)  && (!currProject.equals(view.getPrevProject()))){

      //remove previous filters
      view.clearComboBoxes();

      view.setPrevProject();

      view.setEnabledModuleComboBox(true);
      view.setEnabledVersionComboBox(true);

      //make sure of proper user credentials and make connection
      controller.createConnection();
      view.setCurrentAppID();

      view.setModulesList(controller.getModulesList());
      view.setVersionsList(controller.getVersionsList());
      view.setLogs(controller.getLogs());

    }else if((currProject!=null) && (currProject.equals(view.getPrevProject()))){

      //same as previous selection
    }else if(view.getCurrModule()==null){ //modules does not exist

      Messages.showErrorDialog(ERROR_PROJECT_NOT_EXIST, ERROR_DIALOG_TITLE);
    }else{ //not valid project name => error

      Messages.showErrorDialog(ERROR_PROJECT_NOT_EXIST, ERROR_DIALOG_TITLE);
      view.setModuleALActive(false);
      view.setVersionALActive(false);
      view.resetComponents();
      view.setModuleALActive(true);
      view.setVersionALActive(true);
    }
  }

}
