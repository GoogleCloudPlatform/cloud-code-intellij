package com.google.gct.idea.debugger.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gct.idea.debugger.actions.CloudDebugHelpAction;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.IconLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.Override;

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
