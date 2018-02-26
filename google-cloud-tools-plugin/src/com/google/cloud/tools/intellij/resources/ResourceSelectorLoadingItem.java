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

package com.google.cloud.tools.intellij.resources;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.intellij.ui.components.JBLabel;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.NotNull;

/** Displays UI similar to "loading..." when an resource manager call is in progress. */
public class ResourceSelectorLoadingItem extends JPanel {

  private JLabel progressIcon;

  public ResourceSelectorLoadingItem(
      @NotNull Color backgroundNonSelectionColor, @NotNull Color textNonSelectionColor) {
    this.setLayout(new FlowLayout());
    this.setOpaque(false);

    setBorder(BorderFactory.createEmptyBorder(2, 15, 2, 0));

    JLabel loadText = new JBLabel();
    loadText.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
    loadText.setHorizontalAlignment(SwingConstants.LEFT);
    loadText.setVerticalAlignment(SwingConstants.CENTER);
    loadText.setOpaque(false);
    loadText.setBackground(backgroundNonSelectionColor);
    loadText.setForeground(textNonSelectionColor);
    loadText.setText("Loading...");

    progressIcon = new JBLabel();
    progressIcon.setOpaque(false);
    this.add(progressIcon);
    this.add(loadText);
  }

  // This is called to animate the spinner.  It snaps a frame of the spinner based on current
  // timer ticks.
  public void snap() {
    long currentMilliseconds = System.nanoTime() / 1000000;
    int frame = (int) (currentMilliseconds / 100) % GoogleCloudCoreIcons.STEP_ICONS.size();
    progressIcon.setIcon(GoogleCloudCoreIcons.STEP_ICONS.get(frame));
  }
}
