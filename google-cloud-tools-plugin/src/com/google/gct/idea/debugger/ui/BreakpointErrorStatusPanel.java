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
package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.debugger.CloudLineBreakpointProperties;
import com.google.gct.idea.debugger.CloudLineBreakpointType;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CaptionPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.intellij.xdebugger.impl.frame.XWatchesView;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchesRootNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * The breakpoint config panel is shown for both the config popup (right click on a breakpoint) and in the full
 * breakpoint manager dialog. Cloud snapshot locations can have a condition and custom watches. They do not support
 * "Suspend" options.
 */
public class BreakpointErrorStatusPanel
  extends XBreakpointCustomPropertiesPanel<XLineBreakpoint<CloudLineBreakpointProperties>>  {

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
    XBreakpointBase lineBreakpointImpl = breakpoint instanceof XBreakpointBase ? (XBreakpointBase)breakpoint : null;
    Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
    CloudLineBreakpointType.CloudLineBreakpoint cloudBreakpoint = null;
    if (javaBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
      cloudBreakpoint = (CloudLineBreakpointType.CloudLineBreakpoint)javaBreakpoint;
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
  public void saveTo(@NotNull final XLineBreakpoint<CloudLineBreakpointProperties> xIdebreakpoint) {
  }

  private void createUIComponents() {
  }
}
