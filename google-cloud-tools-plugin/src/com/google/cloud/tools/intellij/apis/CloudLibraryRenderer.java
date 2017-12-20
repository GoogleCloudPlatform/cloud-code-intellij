/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.json.CloudLibrary;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom {@link javax.swing.table.TableCellRenderer TableCellRenderer} for {@link CloudLibrary}
 * objects.
 */
final class CloudLibraryRenderer extends DefaultTableCellRenderer {

  private static final Border NO_FOCUS_BORDER = new EmptyBorder(5, 5, 5, 5);

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component component =
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setBorder(NO_FOCUS_BORDER);
    return component;
  }

  @Override
  public void setValue(Object value) {
    if (value instanceof CloudLibrary) {
      CloudLibrary library = (CloudLibrary) value;
      setText(library.getName());
    } else {
      setText("");
    }
  }
}
