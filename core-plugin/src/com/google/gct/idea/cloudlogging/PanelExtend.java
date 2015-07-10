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
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by amulyau on 6/24/15.
 */
public class PanelExtend extends JBPanel {
  GridBagConstraints gb;
  JTextArea log = null;
  JBLabel labelIcon;


    public PanelExtend(){
        gb = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        log = new JTextArea();
        labelIcon = new JBLabel();
        this.setBackground(Color.white);

    }

    public JTextArea getLogText(){
        return log;
    }

    public JBLabel getLableIcon(){
        return labelIcon;
    }

    public void setLableIcon(JBLabel label){

        this.labelIcon = label;

        gb.gridx=0;
        gb.gridy = 0;
        gb.weightx = 0.0;
        gb.weighty=0.0;
        gb.gridheight=1; //icon takes up 2 rows.
        gb.fill=gb.BOTH;
        gb.gridwidth = 1;
        gb.anchor=gb.FIRST_LINE_START; //top left corner always
        this.add(labelIcon,gb);
    }

    public void setTextArea(JTextArea log){

        this.log= log;

        gb.gridx=1;
        gb.gridy = 0;
        gb.weightx = 1.0;
        gb.weighty=1.0;
        gb.gridheight=2; //icon takes up 2 rows.
        gb.fill=gb.BOTH;
        gb.gridwidth = GridBagConstraints.REMAINDER;
        gb.anchor=gb.NORTH; //top left corner always
        this.add(log,gb);
    }
}
