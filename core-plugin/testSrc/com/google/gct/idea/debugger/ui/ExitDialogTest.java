package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.stats.UsageTrackerService.UsageTracker;
import com.intellij.openapi.ui.DialogWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

@RunWith(MockitoJUnitRunner.class)
public class ExitDialogTest {

  private ExitDialog dialog;

  @Mock
  private UsageTracker tracker;

  @Before
  public void setUp() throws InvocationTargetException, InterruptedException {
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog = new ExitDialog(null, tracker);

        dialog.show();
      }
    });
  }

  @After
  public void tearDown() throws InvocationTargetException, InterruptedException {
    SwingUtilities.invokeAndWait(new Runnable() {
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
  public void testDialogNonModal() {
    Assert.assertFalse(dialog.isModal());
  }

  @Test
  public void testDoCancelAction() throws InvocationTargetException, InterruptedException {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          dialog.show();
          Assert.assertTrue(dialog.getPeer().isVisible());
        }
      });
    } catch (InvocationTargetException ex) {
    }

    SwingUtilities.invokeAndWait(new Runnable() {
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
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          dialog.show();
          Assert.assertTrue(dialog.getPeer().isVisible());
        }
      });
    } catch (InvocationTargetException ex) {
    }

    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        dialog.doOKAction();
        Assert.assertFalse(dialog.getPeer().isVisible());
      }
    });

    Mockito.verify(tracker)
        .trackEvent(GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.continuelistening", null);
  }

}
