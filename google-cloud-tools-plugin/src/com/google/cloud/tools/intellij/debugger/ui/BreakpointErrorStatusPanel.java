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

package com.google.cloud.tools.intellij.debugger.ui;

import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointProperties;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The breakpoint config panel is shown for both the config popup (right click on a breakpoint) and
 * in the full breakpoint manager dialog. Cloud snapshot locations can have a condition and custom
 * watches. They do not support "Suspend" options.
 */
public class BreakpointErrorStatusPanel
    extends XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>> {

  private static final Logger LOG = Logger.getInstance(BreakpointErrorStatusPanel.class);
  private JBLabel errorDescription;
  private JBLabel errorLabel;
  private JPanel errorPanel;
  private JPanel mainPanel;

  public BreakpointErrorStatusPanel() {
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return mainPanel;
  }

  @Override
  public void loadFrom(@NotNull XLineBreakpoint<CloudLineBreakpointProperties> breakpoint) {
    XBreakpointBase lineBreakpointImpl =
        breakpoint instanceof XBreakpointBase ? (XBreakpointBase) breakpoint : null;
    Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
    CloudLineBreakpoint cloudBreakpoint = null;
    if (javaBreakpoint instanceof CloudLineBreakpoint) {
      cloudBreakpoint = (CloudLineBreakpoint) javaBreakpoint;
    }

    if (cloudBreakpoint == null || lineBreakpointImpl == null) {
      return;
    }

    errorPanel.setVisible(cloudBreakpoint.hasError());
    if (cloudBreakpoint.hasError()) {
      errorLabel.setForeground(JBColor.RED);
      errorDescription.setText(cloudBreakpoint.getErrorMessage());
    }
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void saveTo(@NotNull final XLineBreakpoint<CloudLineBreakpointProperties> ideBreakpoint) {
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  private void createUIComponents() {
  }
}
