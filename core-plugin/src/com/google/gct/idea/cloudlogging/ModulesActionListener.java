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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Modules Combo Box Listener for AppEngineLogToolWindowView
 * Created by amulyau on 6/16/15.
 */
public class ModulesActionListener implements ActionListener {

  /**Controller for App Engine Logs*/
  AppEngineLogging controller;

  /**View for the App Engine Logs*/
  AppEngineLogToolWindowView view;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs with the Modules Combo Box
   */
  public ModulesActionListener(AppEngineLogging controller, AppEngineLogToolWindowView view){

    this.controller=controller;
    this.view = view;
  }

  /**
   * Action Listener class that verifies modules and get the versions list and logs
   * based on that information
   * @param e Even that occurs
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    //gets previous module selection to compare with current module selection
    String prevModuleSelection = view.getCurrModule();
    view.setCurrModule();

    String currModule = view.getCurrModule();

    if(currModule==null){ //do not do anything if nothing is there/selected

    }else if((prevModuleSelection!=null) && (currModule.equals(prevModuleSelection))) {
      //same selection as previous selection
    }else{

      if(view.getModuleALActive()) { //solve combobox touchiness problems

        view.setVersionsList(controller.getVersionsList());
        view.setLogs(controller.getLogs());
      }
    }
  }

}
