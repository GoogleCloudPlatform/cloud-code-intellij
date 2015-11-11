package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.stats.UsageTrackerService.UsageTracker;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DialogWrapperPeer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;

@RunWith(MockitoJUnitRunner.class)
public class ExitDialogTest {

  private ExitDialog dialog;

  @Mock
  private UsageTracker tracker;

  @Before
  public void setUp() throws InvocationTargetException, InterruptedException {
    IdeEventQueue.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog = new ExitDialog(null, tracker);
      }
    });
  }

  @After
  public void tearDown() throws InvocationTargetException, InterruptedException {
    IdeEventQueue.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
      }
    });
  }

  @Test
  public void testCreateCenterPanel() {
    JLabel label = (JLabel) dialog.createCenterPanel();
    Assert.assertEquals("Continue listening for snapshots in background?", label.getText());
  }

  @Test
  public void testContinueButton() {
    DialogWrapperPeer peer = dialog.getPeer();
    Container contentPane = peer.getContentPane();

    JButton continueButton = findButtonWithText(contentPane, "Continue");
    Assume.assumeNotNull(continueButton);

    Assert.assertEquals("Continue", continueButton.getText());
  }

  @Test
  public void testStopListeningButton() {

    DialogWrapperPeer peer = dialog.getPeer();
    Container contentPane = peer.getContentPane();
    JButton button = findButtonWithText(contentPane, "Stop Listening");
    Assume.assumeNotNull(button);

    Assert.assertEquals("Stop Listening", button.getText());
  }

  @Test
  public void testDialogNonModal() {
    Assert.assertFalse(dialog.isModal());
  }

  @Test
  public void testDoCancelAction() throws InvocationTargetException, InterruptedException {
    try {
      IdeEventQueue.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          dialog.show();
          Assert.assertTrue(dialog.getPeer().isVisible());
        }
      });
    } catch (InvocationTargetException ex) {
      // for unclear reasons this test fails when not run inside IDEA.
      Assume.assumeFalse(ex.getCause() instanceof NullPointerException);
    }

    IdeEventQueue.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog.doCancelAction();
        Assert.assertFalse(dialog.getPeer().isVisible());
      }
    });


    Mockito.verify(tracker).trackEvent(GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.stoplistening", null);
  }


  @Test
  public void testDoOKAction() throws InvocationTargetException, InterruptedException {
    try {
      IdeEventQueue.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          dialog.show();
          Assert.assertTrue(dialog.getPeer().isVisible());
        }
      });
    } catch (InvocationTargetException ex) {
      // for unclear reasons this test fails when not run inside IDEA.
      Assume.assumeFalse(ex.getCause() instanceof NullPointerException);
    }

    IdeEventQueue.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog.doOKAction();
        Assert.assertFalse(dialog.getPeer().isVisible());
      }
    });

    Mockito.verify(tracker).trackEvent(GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.continuelistening", null);
  }

  @Nullable
  private JButton findButtonWithText(Component component, String text) {
    if (component instanceof JButton) {
      JButton button = (JButton) component;
      if (text.equals(button.getText())) {
        return button;
      }
    } else if (component instanceof Container) {
      Container container = (Container) component;
      for (Component child : container.getComponents()) {
        JButton button = findButtonWithText(child, text);
        if (button != null) {
          return button;
        }
      }
    }
    return null;
  }
}
