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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

/**
 * Action on the left Side tool bar of the tool window that controls the time order of logs
 * Created by amulyau on 7/10/15.
 */
public class TimeOrderAction extends ToggleAction {

  private final AppEngineLogToolWindowView view;

  public TimeOrderAction(AppEngineLogToolWindowView view) {

    super("Ascending","Ascending Logs", AppEngineIconsAndStrings.ASC_ORDER_ICON);
    this.view = view;

  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return view.getTimeOrder();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    if(state){ //if selected, time asc
      view.changeTimeOrder(true);
      System.out.println("true");
      // view.refreshTimeOrderLogs();
    }else{
      view.changeTimeOrder(false);
      System.out.println("false");
      //view.refreshTimeOrderLogs();
    }

  }

}
