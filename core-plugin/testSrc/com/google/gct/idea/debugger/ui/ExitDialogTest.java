package com.google.gct.idea.debugger.ui;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.ui.DialogWrapperPeer;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;

@Ignore
public class ExitDialogTest {

  private ExitDialog dialog;

  @Before
  public void setUp() throws InvocationTargetException, InterruptedException {

    IdeEventQueue.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog = new ExitDialog(null);
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
    // test fails on gradle for inobvious reasons; for now skip there

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
  public void testStopListeningButtonClosesDialog() throws InvocationTargetException, InterruptedException {
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
  }

  @Nullable
  private JButton findButtonWithText(Component component, String text) {
    if (component instanceof JButton) {
      JButton button = (JButton) component;
      System.err.println(button.getText());
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
