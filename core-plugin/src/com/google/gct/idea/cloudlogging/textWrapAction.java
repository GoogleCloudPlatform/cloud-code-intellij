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
 * Created by amulyau on 6/30/15.
 */
public class textWrapAction extends ToggleAction {

  AppEngineLogToolWindowView view;
  public textWrapAction(AppEngineLogToolWindowView view){
    super("Wrap","Text Wrap",AppEngineIcons.WRAP_TOGGLE_ICON);
    this.view = view;

  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return view.getTextWrap();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    if (state==true) { //selected => text wrap
      view.changeTextWrapState(true);
      view.registerUI();
    }
    else {
      view.changeTextWrapState(false);
      view.registerUI();
    }


  }


}
