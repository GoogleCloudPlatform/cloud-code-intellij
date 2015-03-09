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
package com.google.gct.idea.debugger;

import com.google.api.services.debugger.model.StackFrame;
import com.google.api.services.debugger.model.Variable;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CloudExecutionStack represents an entire stack for a {@link com.google.api.services.debugger .model.Breakpoint} It
 * stores the individual frames, and also the variables and custom watch expressions.
 */
public class CloudExecutionStack extends XExecutionStack {
  private final List<XStackFrame> myFrames = new ArrayList<XStackFrame>();

  public CloudExecutionStack(@NotNull Project project,
                             @NotNull String name,
                             @NotNull List<StackFrame> frames,
                             @NotNull List<Variable> variableTable,
                             @Nullable List<Variable> evaluatedExpressions) {
    super(name);
    for (StackFrame nativeFrame : frames) {
      myFrames.add(new CloudStackFrame(project, nativeFrame, variableTable, evaluatedExpressions));
      // We only show custom watches on the top frame.
      evaluatedExpressions = null;
    }
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    if (firstFrameIndex <= myFrames.size()) {
      container.addStackFrames(myFrames.subList(firstFrameIndex, myFrames.size()), true);
    }
    else {
      container.addStackFrames(Collections.<XStackFrame>emptyList(), true);
    }
  }

  @Override
  public XStackFrame getTopFrame() {
    return myFrames.size() > 0 ? myFrames.get(0) : null;
  }
}
