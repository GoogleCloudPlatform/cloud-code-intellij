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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.StackFrame;
import com.google.api.services.clouddebugger.v2.model.Variable;
import com.google.gct.idea.util.GctBundle;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueGroup;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CloudStackFrame represents a single frame in a {@link CloudExecutionStack}. It returns the set of
 * variables and if appropriate, the set of watch expressions at that location.
 */
public class CloudStackFrame extends XStackFrame {
  @Nullable private final List<Variable> evaluatedExpressions;
  private final StackFrame frame;
  private final List<Variable> variableTable;
  private final XSourcePosition xSourcePosition;

  public CloudStackFrame(@NotNull Project project,
      @NotNull StackFrame frame,
      @NotNull List<Variable> variableTable,
      @Nullable List<Variable> evaluatedExpressions,
      @NotNull ServerToIDEFileResolver fileResolver) {
    this.frame = frame;
    this.variableTable = variableTable;
    this.evaluatedExpressions = evaluatedExpressions;
    String path = frame.getLocation().getPath();
    if (!Strings.isNullOrEmpty(path)) {
      xSourcePosition = XDebuggerUtil.getInstance().createPosition(
          fileResolver.getFileFromPath(project, path),
          frame.getLocation().getLine() - 1);
    }
    else {
      xSourcePosition = null;
    }
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    final XValueChildrenList list = new XValueChildrenList();
    List<Variable> arguments = frame.getArguments();
    if (arguments != null && arguments.size() > 0) {
      for (Variable variable : arguments) {
        if (!Strings.isNullOrEmpty(variable.getName())) {
          list.add(variable.getName(), new MyValue(variable, variableTable));
        }
      }
    }
    List<Variable> locals = frame.getLocals();
    if (locals != null && locals.size() > 0) {
      for (Variable variable : locals) {
        if (!Strings.isNullOrEmpty(variable.getName())) {
          list.add(variable.getName(), new MyValue(variable, variableTable));
        }
      }
    }

    if (evaluatedExpressions != null && evaluatedExpressions.size() > 0) {
      list.addTopGroup(new CustomWatchGroup());
    }
    node.addChildren(list, true);
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    component.setIcon(AllIcons.Debugger.StackFrame);
    String functionName = frame.getFunction();
    String className = "";
    String packageName = "";

    int lastDot = frame.getFunction().lastIndexOf('.');
    if (lastDot > 0 && lastDot < functionName.length() - 1) {
      functionName = functionName.substring(lastDot + 1);
      className = frame.getFunction().substring(0, lastDot);
      int classNameDot = className.lastIndexOf('.');
      if (classNameDot > 0 && classNameDot < className.length() - 1) {
        className = className.substring(classNameDot + 1);
        packageName = frame.getFunction().substring(0, classNameDot);
      }
    }
    component.append(functionName + "():" + frame.getLocation().getLine().toString() + ", " + className,
                     xSourcePosition != null
                     ? SimpleTextAttributes.REGULAR_ATTRIBUTES
                     : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    component.append(" (" + packageName + ")", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);

  }

  @Override
  public Object getEqualityObject() {
    return CloudStackFrame.class;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    return xSourcePosition;
  }

  private static class MyValue extends XValue {
    private final List<Variable> members;
    private final Variable variable;
    private final List<Variable> variableTable;

    public MyValue(@NotNull Variable variable, @NotNull List<Variable> variableTable) {
      //Note that we have to examine the variable table for some cases depending on how the
      // server compressed results.
      this.variableTable = variableTable;
      this.variable = variable.getVarTableIndex() != null ? variableTable.get(variable.getVarTableIndex().intValue())
          : variable;
      members = variable.getMembers();
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      final XValueChildrenList list = new XValueChildrenList();
      if (members != null && members.size() > 0) {
        for (Variable variable : members) {
          if (!Strings.isNullOrEmpty(variable.getName())) {
            list.add(variable.getName(), new MyValue(variable, variableTable));
          }
        }
      }
      node.addChildren(list, true);
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
      String status = BreakpointUtil.getUserMessage(variable.getStatus());
      String value = !Strings.isNullOrEmpty(status) ?
          String.format("%s (%s)", variable.getValue(), status) : variable.getValue();
      node.setPresentation(null, members != null && members.size() > 0 ? "..." : null, value != null ? value : "",
          members != null && members.size() > 0);
    }

    @Override
    public String getEvaluationExpression() {
      return variable.getName();
    }
  }

  private class CustomWatchGroup extends XValueGroup {
    protected CustomWatchGroup() {
      super(GctBundle.getString("clouddebug.watchexpressiongrouptitle"));
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      final XValueChildrenList list = new XValueChildrenList();

      if (evaluatedExpressions != null && evaluatedExpressions.size() > 0) {
        for (Variable variable : evaluatedExpressions) {
          if (!Strings.isNullOrEmpty(variable.getName())) {
            list.add(variable.getName(), new MyValue(variable, variableTable));
          }
        }
      }
      node.addChildren(list, true);
    }

    @Override
    public boolean isAutoExpand() {
      return true;
    }
  }
}
