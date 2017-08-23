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

package com.google.cloud.tools.intellij.gcs;

import com.google.cloud.storage.Bucket;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/** Custom cell renderer for rendering Google Cloud Storage bucket line items for display. */
final class GcsBucketCellRenderer extends JLabel implements ListCellRenderer<Bucket> {

  GcsBucketCellRenderer() {
    setOpaque(true);
  }

  @Override
  public Component getListCellRendererComponent(
      JList<? extends Bucket> list,
      Bucket bucket,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    setIcon(GoogleCloudToolsIcons.CLOUD);
    setText(bucket.getName());

    if (isSelected) {
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    } else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    return this;
  }
}
