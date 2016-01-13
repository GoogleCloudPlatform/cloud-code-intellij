/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.elysium;

import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * UI for the node that displays error information if an elysium call fails.
 */
class ProjectSelectorErrorItem extends JBLabel {

  public ProjectSelectorErrorItem(@NotNull Color errorForeground) {
    setBorder(BorderFactory.createEmptyBorder(2, 15, 2, 0));
    setOpaque(false);
    setHorizontalAlignment(SwingConstants.LEFT);
    setVerticalAlignment(SwingConstants.CENTER);
    setFont(new Font(getFont().getFontName(), Font.BOLD, getFont().getSize()));
    setForeground(errorForeground);
  }
}
