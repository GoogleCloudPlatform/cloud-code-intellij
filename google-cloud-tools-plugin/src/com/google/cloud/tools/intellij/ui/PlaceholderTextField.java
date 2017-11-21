/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.ui;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTextField;

/** Extends JTextField to add placeholder text. */
public class PlaceholderTextField extends JTextField {

  private String placeholderText;
  private Color defaultColor;

  public Color getDefaultColor() {
    return defaultColor;
  }

  /**
   * Sets the placeholder text on the input and attaches a listener to update the placeholder text
   * when the input's editability property changes.
   */
  public void setPlaceholderText(String placeholderText) {
    this.placeholderText = placeholderText;
    this.defaultColor = this.getForeground();

    updatePlaceholderText();

    this.addPropertyChangeListener(
        "editable",
        new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            updatePlaceholderText();
          }
        });
  }

  private void updatePlaceholderText() {
    if (isEditable()) {
      setText("");
      setForeground(getDefaultColor());
    } else {
      setText(this.placeholderText);
      setForeground(getDisabledTextColor());
    }
  }
}
