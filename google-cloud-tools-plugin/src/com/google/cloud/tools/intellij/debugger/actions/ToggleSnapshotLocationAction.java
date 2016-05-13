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

package com.google.cloud.tools.intellij.debugger.actions;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

import com.google.cloud.tools.intellij.debugger.CloudDebugRunConfiguration;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebuggerUtil;

import java.awt.event.MouseEvent;

/**
 * This action sets a cloud breakpoint (snapshot location) at the line of code the user right
 * clicked on. Right now, we store that line of code in user data (SnapshotTargetLine).
 */
public class ToggleSnapshotLocationAction extends AnAction {

  public static final Key<Integer> POPUP_LINE = Key.create("SnapshotTargetLine");
  private static final Logger LOG = Logger.getInstance(ToggleSnapshotLocationAction.class);

  /**
   * Initialize the action.
   */
  public ToggleSnapshotLocationAction() {
    super(GctBundle.getString("clouddebug.snapshot.location"),
        GctBundle.getString("clouddebug.adds.snapshot.location"),
        GoogleCloudToolsIcons.CLOUD);
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void actionPerformed(AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    Editor editor = EDITOR.getData(dataContext);
    if (editor instanceof EditorEx
        && event.getInputEvent() instanceof MouseEvent
        && editor.getUserData(POPUP_LINE) != null) {
      EditorEx exEditor = (EditorEx) editor;
      if (exEditor.getProject() == null) {
        LOG.error("could not add a snapshot location as the target editor was unexpectedly null.");
        return;
      }
      if (editor.getUserData(POPUP_LINE) == null) {
        LOG.error("could not add a snapshot location as the target line was unexpectedly null.");
        return;
      }
      XDebuggerUtil.getInstance()
          .toggleLineBreakpoint(exEditor.getProject(), CloudLineBreakpointType.getInstance(),
              exEditor.getVirtualFile(),
              editor.getUserData(POPUP_LINE));
    }
  }

  @Override
  public void update(AnActionEvent event) {
    Editor editor = EDITOR.getData(event.getDataContext());
    if (editor == null) {
      return;
    }
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(editor.getProject());
    for (RunConfiguration runConfiguration : runManager.getAllConfigurations()) {
      if (runConfiguration instanceof CloudDebugRunConfiguration) {
        event.getPresentation().setVisible(true);
        return;
      }
    }

    event.getPresentation().setVisible(false);
  }
}
