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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Refresh Button to refresh the logs
 * Created by amulyau on 5/29/15.
 */
public class RefreshButtonAction extends AnAction {

  /**Controller for App Engine Logs*/
  AppEngineLogging controller;

  /**View for the App Engine Logs*/
  AppEngineLogToolWindowView view;

  /**
   * Constructor that creates the button with icon and sets project
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   */
  public RefreshButtonAction(AppEngineLogging controller, AppEngineLogToolWindowView view) {

    super("Refresh", "Refresh the App Engine Logs", AppEngineLogging.REFRESH_BUTTON_ICON);
    this.controller = controller;
    this.view = view;

  }


  /**
   * Refreshes the logs as long as a project is selected (implies module and version are also set)
   * @param e Click event that occurs
   */
  @Override
  public void actionPerformed(AnActionEvent e){

    if(view.getCurrProject()!=null) {

      view.setLogs(controller.getLogs());
    }
  }


  /**
   * Makes sure the button is available to click
   * @param e Action event
   */
  @Override
  public void update(AnActionEvent e){

    e.getPresentation().setEnabled(true);
  }

}
