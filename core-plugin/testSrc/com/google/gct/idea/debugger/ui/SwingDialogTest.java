package com.google.gct.idea.debugger.ui;

import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;

public class SwingDialogTest {

  @Test
  public void testShowDialog() throws InvocationTargetException, InterruptedException {
    final JDialog dialog = new JDialog();
    Container pane = dialog.getContentPane();
    pane.add(new JButton("foo"));
        SwingUtilities.invokeAndWait(new Runnable() {

          @Override
          public void run() {
            dialog.setVisible(true);
          }
        });

    Assert.assertNotNull(findButtonWithText(pane, "foo"));
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
