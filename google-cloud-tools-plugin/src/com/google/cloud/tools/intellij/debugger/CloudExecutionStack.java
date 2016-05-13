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

package com.google.cloud.tools.intellij.debugger;

import com.google.api.services.clouddebugger.v2.model.StackFrame;
import com.google.api.services.clouddebugger.v2.model.Variable;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XExecutionStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CloudExecutionStack represents an entire stack for a
 * {@link com.google.api.services.clouddebugger.v2.model.Breakpoint}. It stores the individual
 * frames, and also the variables and custom watch expressions.
 */
public class CloudExecutionStack extends XExecutionStack {

  private final List<CloudStackFrame> frames = new ArrayList<CloudStackFrame>();

  /**
   * Initialize the execution stack.
   */
  public CloudExecutionStack(
      @NotNull Project project,
      @NotNull String name,
      @Nullable List<StackFrame> frames,
      @Nullable List<Variable> variableTable,
      @Nullable List<Variable> evaluatedExpressions) {
    super(name);

    if (frames != null) {
      if (variableTable == null) {
        variableTable = Collections.emptyList();
      }
      for (StackFrame nativeFrame : frames) {
        this.frames
            .add(new CloudStackFrame(project, nativeFrame, variableTable, evaluatedExpressions,
                new ServerToIdeFileResolver()));
        // We only show custom watches on the top frame.
        evaluatedExpressions = null;
      }
    }
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    if (firstFrameIndex <= frames.size()) {
      container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
    } else {
      container.addStackFrames(Collections.<CloudStackFrame>emptyList(), true);
    }
  }

  @Override
  public CloudStackFrame getTopFrame() {
    return frames.size() > 0 ? frames.get(0) : null;
  }
}
