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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Each Node in the JTree is a Text Area Node
 * Created by amulyau on 6/23/15.
 */
public class TextAreaNode extends DefaultMutableTreeNode {

  private final String text;
  private final Icon icon;

  /**
   * Constructor
   * @param text Text to go in the tree node
   * @param bool boolean value on whether it allows children or not
   * @param severityIcon icon that represents the log level
   */
  public TextAreaNode(String text, boolean bool, Icon severityIcon) {
    this.setAllowsChildren(bool);
    this.text = text;
    this.icon = severityIcon;
  }

  /**
   * Gets the Text of the Tree node
   * @return String text
   */
  public String getText() {
    return text;
  }

  /**
   * Gets the Icon of the Tree Node
   * @return Icon that represents the log level of the tree node.
   */
  public Icon getIcon() {
    return icon;
  }

}
