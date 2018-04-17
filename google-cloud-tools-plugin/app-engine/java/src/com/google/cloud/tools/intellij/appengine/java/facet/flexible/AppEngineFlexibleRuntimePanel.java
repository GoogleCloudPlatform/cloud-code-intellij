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

package com.google.cloud.tools.intellij.appengine.java.facet.flexible;

import com.google.common.annotations.VisibleForTesting;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** UI panel containing App Engine flexible runtime information. */
public final class AppEngineFlexibleRuntimePanel {

  private JPanel runtimePanel;
  private JLabel runtimeLabel;

  /** Sets the visibility of the entire panel. */
  public void setVisible(boolean visibile) {
    runtimePanel.setVisible(visibile);
  }

  /** Sets the text of the runtime label ('custom', 'java', etc.). */
  public void setLabelText(String text) {
    runtimeLabel.setText(text);
  }

  @VisibleForTesting
  public boolean isVisible() {
    return runtimePanel.isVisible();
  }

  @VisibleForTesting
  public String getLabelText() {
    return runtimeLabel.getText();
  }
}
