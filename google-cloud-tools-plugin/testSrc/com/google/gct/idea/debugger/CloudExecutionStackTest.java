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

package com.google.gct.idea.debugger;

import com.google.api.services.clouddebugger.v2.model.SourceLocation;
import com.google.api.services.clouddebugger.v2.model.StackFrame;
import com.google.api.services.clouddebugger.v2.model.Variable;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.frame.XValueChildrenList;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class CloudExecutionStackTest {

  @Mock
  private Project project;

  @Test
  public void testGetTopFrame_nullFramesReturnsNull() {
    CloudExecutionStack stack = new CloudExecutionStack(project, "name", null, null, null);
    Assert.assertNull(stack.getTopFrame());
  }

  @Test
  public void testGetTopFrame_nonNull() {
    List<StackFrame> frames = new ArrayList<StackFrame>();
    StackFrame frame1 = new StackFrame();
    SourceLocation location1 = new SourceLocation();
    location1.setLine(1);
    frame1.setLocation(location1);
    List<Variable> variables = new ArrayList<Variable>();
    Variable variable = new Variable();
    variable.setName("foo");
    variables.add(variable);
    frame1.setLocals(variables);

    StackFrame frame2 = new StackFrame();
    SourceLocation location2 = new SourceLocation();
    location2.setLine(2);
    frame2.setLocation(location2);

    frames.add(frame1);
    frames.add(frame2);

    CloudExecutionStack stack = new CloudExecutionStack(project, "name", frames, null, null);
    CloudStackFrame localFrame = stack.getTopFrame();
    Assert.assertNotNull(localFrame);
    SpyNode node = new SpyNode();
    localFrame.computeChildren(node);
    Assert.assertEquals(1, node.seenChildren.size());
    Assert.assertEquals("foo", node.seenChildren.get(0));
  }

  private static class SpyNode implements XCompositeNode {

    List<String> seenChildren = new ArrayList<String>();

    @Override
    public void addChildren(XValueChildrenList children, boolean last) {
      for (int i = 0; i < children.size(); i++) {
        seenChildren.add(children.getName(i));
      }
    }

    @Override
    public void tooManyChildren(int remaining) {
    }

    @Override
    public void setAlreadySorted(boolean alreadySorted) {
    }

    @Override
    public void setErrorMessage(String errorMessage) {
    }

    @Override
    public void setErrorMessage(String errorMessage, XDebuggerTreeNodeHyperlink link) {
    }

    @Override
    public void setMessage(String message, Icon icon,
        SimpleTextAttributes attributes, XDebuggerTreeNodeHyperlink link) {
    }

    @Override
    public boolean isObsolete() {
      return false;
    }
  }
}
