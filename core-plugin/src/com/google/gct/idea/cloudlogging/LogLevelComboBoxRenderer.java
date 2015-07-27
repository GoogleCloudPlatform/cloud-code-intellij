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

import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by amulyau on 7/1/15.
 */
public class LogLevelComboBoxRenderer extends JBLabel implements ListCellRenderer{


  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    //value passed in is jb label
    setText(((JBLabel)value).getText());
    setIcon(((JBLabel)value).getIcon());


    return this;
  }
}
