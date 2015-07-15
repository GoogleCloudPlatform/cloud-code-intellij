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
import com.google.api.services.debugger.model.StackFrame;
import com.google.api.services.debugger.model.Variable;
import com.google.gct.idea.util.GctBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CloudStackFrame represents a single frame in a {@link CloudExecutionStack}. It returns the set of variables and if
 * appropriate, the set of watch expressions at that location.
 */
public class CloudStackFrame extends XStackFrame {
  @Nullable private final List<Variable> myEvaluatedExpressions;
  private final StackFrame myFrame;
  private final List<Variable> myVariableTable;
  private final XSourcePosition myXSourcePosition;

  public CloudStackFrame(@NotNull Project project,
                         @NotNull StackFrame frame,
                         @NotNull List<Variable> variableTable,
                         @Nullable List<Variable> evaluatedExpressions) {
    myFrame = frame;
    myVariableTable = variableTable;
    myEvaluatedExpressions = evaluatedExpressions;
    String path = frame.getLocation().getPath();
    if (!Strings.isNullOrEmpty(path)) {
      JavaUtil.initializeLocations(project, false);
      VirtualFile file = JavaUtil.getFileFromCloudPath(project, frame.getLocation().getPath());
      myXSourcePosition = XDebuggerUtil.getInstance().createPosition(file, frame.getLocation().getLine() - 1);
    }
    else {
      myXSourcePosition = null;
    }
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    final XValueChildrenList list = new XValueChildrenList();
    List<Variable> arguments = myFrame.getArguments();
    if (arguments != null && arguments.size() > 0) {
      for (Variable variable : arguments) {
        if (!Strings.isNullOrEmpty(variable.getName())) {
          list.add(variable.getName(), new MyValue(variable, myVariableTable));
        }
      }
    }
    List<Variable> locals = myFrame.getLocals();
    if (locals != null && locals.size() > 0) {
      for (Variable variable : locals) {
        if (!Strings.isNullOrEmpty(variable.getName())) {
          list.add(variable.getName(), new MyValue(variable, myVariableTable));
        }
      }
    }

    if (myEvaluatedExpressions != null && myEvaluatedExpressions.size() > 0) {
      list.addTopGroup(new CustomWatchGroup());
    }
    node.addChildren(list, true);
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    component.setIcon(AllIcons.Debugger.StackFrame);
    String functionName = myFrame.getFunction();
    String className = "";
    String packageName = "";

    int lastDot = myFrame.getFunction().lastIndexOf('.');
    if (lastDot > 0 && lastDot < functionName.length() - 1) {
      functionName = functionName.substring(lastDot + 1);
      className = myFrame.getFunction().substring(0, lastDot);
      int classNameDot = className.lastIndexOf('.');
      if (classNameDot > 0 && classNameDot < className.length() - 1) {
        className = className.substring(classNameDot + 1);
        packageName = myFrame.getFunction().substring(0, classNameDot);
      }
    }
    component.append(functionName + "():" + myFrame.getLocation().getLine().toString() + ", " + className,
                     myXSourcePosition != null
                     ? SimpleTextAttributes.REGULAR_ATTRIBUTES
                     : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    component.append(" (" + packageName + ")", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);

  }

  @Override
  public Object getEqualityObject() {
    return CloudStackFrame.class;
  }

  @Override
  public XDebuggerEvaluator getEvaluator() {
    return null;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    return myXSourcePosition;
  }

  private static class MyValue extends XValue {
    private final List<Variable> myMembers;
    private final Variable myVariable;
    private final List<Variable> myVariableTable;

    public MyValue(@NotNull Variable variable, @NotNull List<Variable> variableTable) {
      //Note that we have to examine the variable table for some cases depending on how the
      // server compressed results.
      myVariableTable = variableTable;
      myVariable = variable.getVarIndex() != null ? variableTable.get(variable.getVarIndex().intValue()) : variable;
      myMembers = myVariable.getMembers();
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      final XValueChildrenList list = new XValueChildrenList();
      if (myMembers != null && myMembers.size() > 0) {
        for (Variable variable : myMembers) {
          if (!Strings.isNullOrEmpty(variable.getName())) {
            list.add(variable.getName(), new MyValue(variable, myVariableTable));
          }
        }
      }
      node.addChildren(list, true);
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
      String value = myVariable.getValue();
      node.setPresentation(null, myMembers != null && myMembers.size() > 0 ? "..." : null, value != null ? value : "",
                           myMembers != null && myMembers.size() > 0);
    }

    @Override
    public String getEvaluationExpression() {
      return myVariable.getName();
    }
  }

  private class CustomWatchGroup extends XValueGroup {
    protected CustomWatchGroup() {
      super(GctBundle.getString("clouddebug.watchexpressiongrouptitle"));
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      final XValueChildrenList list = new XValueChildrenList();

      if (myEvaluatedExpressions != null && myEvaluatedExpressions.size() > 0) {
        for (Variable variable : myEvaluatedExpressions) {
          if (!Strings.isNullOrEmpty(variable.getName())) {
            list.add(variable.getName(), new MyValue(variable, myVariableTable));
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
