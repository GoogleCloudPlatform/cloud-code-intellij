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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

/**
 * Left Side tool bar toggle button action that controls expanding/collapsing all logs.
 * Created by amulyau on 6/30/15.
 */
public class logsExpandAction extends ToggleAction {

  private AppEngineLogToolWindowView view;

  /**
   * Constructor used to add as action to plugin.xml
   */
  public logsExpandAction() {}

  /**
   * Constructor for the logs expansion action
   * @param view View that holds all components
   */
  public logsExpandAction(AppEngineLogToolWindowView view) {
    super("Expand","Expand Logs", AllIcons.Actions.Expandall);
    this.view = view;
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return view.getLogsExpanded();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    if (state) { //if selected, expand logs
      view.changeLogsExpandState(true);
      view.expandLogs();
    } else {
      view.changeLogsExpandState(false);
      view.collapseLogs();
    }
  }

}