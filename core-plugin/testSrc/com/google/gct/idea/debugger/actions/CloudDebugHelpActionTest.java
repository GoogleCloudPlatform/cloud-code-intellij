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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.Override;

public class CloudDebugHelpActionTest {
  protected static final String URL = "http://helps!";
  @Mock protected AnActionEvent event;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testActionPerformed() {
    CloudDebugHelpAction action = Mockito.spy(new CloudDebugHelpAction(URL));
    // Don't actually open a browser window when we're testing.
    Mockito.doNothing().when(action).openUrl();
    action.actionPerformed(event);

    verify(action, times(1)).openUrl();
  }

  @Test
  public void testUpdate() {
    Presentation presentation = new Presentation();
    when(event.getPresentation()).thenReturn(presentation);

    CloudDebugHelpAction action = new CloudDebugHelpAction(URL);
    action.update(event);

    assertEquals(presentation.getIcon(), IconLoader.getIcon("/actions/help.png"));
    assertEquals(presentation.getText(), CommonBundle.getHelpButtonText());
  }
}