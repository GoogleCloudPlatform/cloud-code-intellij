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
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * To animate the tree style logs in App Engine logs
 * Created by amulyau on 6/9/15.
 */
public class LogsTreeCellRenderer extends DefaultTreeCellRenderer{

  Color backgroundNonSelectionColor = getBackgroundNonSelectionColor();//get defaults from intellij
  Color  backgroundSelectionColor = getBackgroundSelectionColor();

  Color textNonSelectColor = getTextNonSelectionColor();
  Color textSelectColor = getTextSelectionColor();


  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value,boolean selected,
                                                boolean expanded,boolean leaf, int row,
                                                boolean hasFocus){

    TextAreaNode treeNode = (TextAreaNode)value;
    //TextAreaNode node = (TextAreaNode)treeNode.getUserObject(); //put panel node in
    String text = treeNode.getText();
    PanelExtend panel= new PanelExtend();
    JBLabel iconLabel = panel.getLableIcon();
    JTextArea textArea = panel.getLogText();

    textArea.setText(text);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);


    if(treeNode.isLeaf() && !treeNode.isRoot() && (!((TextAreaNode) treeNode.getParent()).isRoot())) {
      // setIcon(AppEngineIcons.TREE_LEAF_ICON);
      textArea.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));
    }else {
      textArea.setBorder(BorderFactory.createEmptyBorder());
    }

    if(selected){
      setForeground(textSelectColor);
      setBackground(backgroundSelectionColor);
      textArea.setBackground(textSelectColor);
      textArea.setBackground(backgroundSelectionColor);

    }else{
      setForeground(textNonSelectColor);
      setBackground(backgroundNonSelectionColor);
      textArea.setBackground(textNonSelectColor);
      textArea.setBackground(backgroundNonSelectionColor);

    }

    iconLabel.setIcon(treeNode.icon);
    panel.setLableIcon(iconLabel);
    panel.setTextArea(textArea);

    return panel;

  }


}

