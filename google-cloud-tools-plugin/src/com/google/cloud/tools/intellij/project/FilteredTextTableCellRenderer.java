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

package com.google.cloud.tools.intellij.project;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** Renders text in table cells marking filter text as bold. */
public class FilteredTextTableCellRenderer extends DefaultTableCellRenderer {
  private String filterText;

  void setFilterText(String filterText) {
    this.filterText = filterText;
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    JLabel label =
        (JLabel)
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    label.setText(highlightFilterText(filterText, label.getText()));
    return label;
  }

  @VisibleForTesting
  String highlightFilterText(String filterText, String text) {
    if (!Strings.isNullOrEmpty(filterText) && !Strings.isNullOrEmpty(text)) {
      return "<html>" + text.replace(filterText, "<b>" + filterText + "</b>");
    }
    return text;
  }
}
