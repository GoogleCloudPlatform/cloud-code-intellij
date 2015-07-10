package com.google.gct.idea.cloudlogging;/*
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

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;

/**
 * Created by amulyau on 7/1/15.
 */
public class LogLevelComboBoxEditor extends BasicComboBoxEditor {
  JBPanel panel = new JBPanel();
  JBLabel label = new JBLabel();

  public LogLevelComboBoxEditor(){
    panel.add(label);
  }

  @Override
  public Component getEditorComponent(){
    return this.panel;
  }

}
