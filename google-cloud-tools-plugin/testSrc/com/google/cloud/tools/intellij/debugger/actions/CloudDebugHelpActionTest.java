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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.IconLoader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudDebugHelpActionTest {
  @Mock private AnActionEvent event;

  @Test
  public void testActionPerformed() {
    CloudDebugHelpAction action = Mockito.spy(new CloudDebugHelpAction("http://www.example.com"));
    // Don't actually open a browser window when we're testing.
    Mockito.doNothing().when(action).openUrl();
    action.actionPerformed(event);

    verify(action, times(1)).openUrl();
  }

  @Test
  public void testUpdate() {
    Presentation presentation = new Presentation();
    when(event.getPresentation()).thenReturn(presentation);

    CloudDebugHelpAction action = new CloudDebugHelpAction("http://www.example.com");
    action.update(event);

    assertEquals(IconLoader.getIcon("/actions/help.png"), presentation.getIcon());
    assertEquals(CommonBundle.getHelpButtonText(), presentation.getText());
  }
}
