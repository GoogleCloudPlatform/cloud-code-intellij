/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.ui;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;

/**
 * This is a combobox control whose {@link com.intellij.openapi.ui.popup.JBPopup} is defined externally.
 * It gives the look and feel of a standard combobox without defining anything that appears in the popup.
 * The popup returns the currently selected text as well as an event when the selection change.
 */
public abstract class CustomizableComboBox extends JPanel {
  private JBTextField textField;
  private JComboBox themedCombo = new ComboBox();
  private boolean popupVisible;

  public CustomizableComboBox() {
    super(new BorderLayout());

    themedCombo.setEditable(true);

    PopupMouseListener listener = new PopupMouseListener();
    // GTK always draws a border on the textbox.  It cannot be removed,
    // so to compensate, we remove our own border so we don't have a double border.
    if (UIUtil.isUnderGTKLookAndFeel()) {
      this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }
    else {
      this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2),
                                                        BorderFactory.createLineBorder(getBorderColor(), 1)));
    }

    // Try to turn off the border on the JTextField.
    textField = new JBTextField() {
      @Override
      public void setBorder(Border border) {
        super.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
      }
    };
    textField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
    textField.addMouseListener(listener);
    textField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        textField.selectAll();
      }

      @Override
      public void focusLost(FocusEvent e) {
        // no-op
      }
    });

    JButton popupButton = createArrowButton();
    popupButton.addMouseListener(listener);

    this.add(popupButton, BorderLayout.EAST);
    this.add(textField, BorderLayout.CENTER);
  }

  @Override
  public Dimension getPreferredSize() {
    return themedCombo.getPreferredSize();
  }

  class PopupMouseListener implements MouseListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      // no-op
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (popupVisible) {
        if (getPopup() != null && getPopup().isPopupVisible()) {
          getPopup().hidePopup();
        }
        popupVisible = false;
      }
      else {
        textField.grabFocus();
        showPopup();
        popupVisible = true;
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // no-op
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      popupVisible = getPopup() != null && getPopup().isPopupVisible();
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // no-op
    }
  }

  protected abstract CustomizableComboBoxPopup getPopup();

  protected abstract int getPreferredPopupHeight();

  protected JBTextField getTextField() {
    return textField;
  }

  public Document getDocument() {
    return getTextField().getDocument();
  }

  public void setText(@Nullable String text) {
    textField.setText(text);
  }

  public String getText() {
    return textField.getText();
  }

  private void showPopup() {
    if (!getPopup().isPopupVisible()) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          boolean showOnTop = false;
          GraphicsConfiguration gc = CustomizableComboBox.this.getGraphicsConfiguration();
          if (gc != null) {

            // We will test to see if we can pop down without going past the screen edge.
            Rectangle bounds = gc.getBounds();
            // Insets account for a taskbar.
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int effectiveScreenAreaHeight = bounds.height - screenInsets.top - screenInsets.bottom;

            Point comboLocation = CustomizableComboBox.this.getLocationOnScreen();
            if (comboLocation.getY() + CustomizableComboBox.this.getHeight()
                + getPreferredPopupHeight() > effectiveScreenAreaHeight) {
              showOnTop = true;
            }
          }
          if (showOnTop) {
            getPopup().showPopup(new RelativePoint(CustomizableComboBox.this,
                                                   new Point(0, -getPreferredPopupHeight())));
          }
          else {
            getPopup().showPopup(new RelativePoint(CustomizableComboBox.this,
                                                   new Point(0, CustomizableComboBox.this.getHeight() - 1)));
          }
        }
      });
    }
  }

  private static boolean isUsingDarculaUIFlavor() {
    return UIUtil.isUnderDarcula() || UIUtil.isUnderIntelliJLaF();
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if ((textField.isFocusOwner() || (getPopup() != null && getPopup().isPopupVisible()))) {
      if (isUsingDarculaUIFlavor()) { // NOPMD
        DarculaUIUtil.paintFocusRing(g, 3, 3, getWidth() - 4, getHeight() - 4);
      }
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();

    textField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        CustomizableComboBox.this.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        CustomizableComboBox.this.repaint();
      }
    });
  }

  private static Color getButtonBackgroundColor() {
    Color color;

    if (isUsingDarculaUIFlavor()) {
      color = UIManager.getColor("ComboBox.darcula.arrowFillColor");
    }
    else {
      color = UIManager.getColor("ComboBox.buttonBackground");
    }

    return color == null ? UIUtil.getControlColor() : color;
  }

  private Color getArrowColor() {
    Color color = null;
    if (isUsingDarculaUIFlavor()) {
      color = isEnabled() ? new JBColor(Gray._255, getForeground()) : new JBColor(Gray._255, getForeground().darker());
    }
    if (color == null) {
      color = getForeground();
    }

    return color;
  }

  private static Color getBorderColor() {
    return new JBColor(Gray._150, Gray._100);
  }

  /*
  * We do custom rendering of the arrow button because there are too many
  * hacks in each theme's combobox UI.
  * We also cannot forward paint of the arrow button to a stock combobox
  * because each LAF and theme may render different parts of the
  * arrow button with different sizes.  For example, in Mac, the arrow button is
  * drawn with a border (the responsibility of the border
  * near the arrow button belongs to the button).  However in IntelliJ and Darcula,
  * the arrow button is drawn without a border and it's the responsibility of the
  * outer control to draw a border.  In addition, darcula renders part of the arrow
  * button outside the button.  That is, the arrow button looks like it's about 20x20,
  * but actually, it's only 16x16, with part of the rendering done via paint on the combobox itself.
  * So while this creates some small inconsistencies,
  * it reduces the chance of a major UI issue such as a completely poorly drawn button with a double border.
  */
  private JButton createArrowButton() {
    final Color bg = getBackground();
    final Color fg = getForeground();
    JButton button = new BasicArrowButton(SwingConstants.SOUTH, bg, fg, fg, fg) {

      @Override
      public void paint(Graphics g2) {
        final Graphics2D g = (Graphics2D)g2;
        final GraphicsConfig config = new GraphicsConfig(g);

        final int w = getWidth();
        final int h = getHeight();
        g.setColor(getButtonBackgroundColor());
        g.fillRect(0, 0, w, h);
        g.setColor(getArrowColor());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        final int midx = (int) Math.ceil((w - 1) / 2.0) + 1;
        final int midy = (int) Math.ceil(h / 2.0);
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(midx - 4, midy - 2);
        path.lineTo(midx + 4, midy - 2);
        path.lineTo(midx, midy + 4);
        path.lineTo(midx - 4, midy - 2);
        path.closePath();
        g.fill(path);
        g.setColor(getBorderColor());
        if (UIUtil.isUnderGTKLookAndFeel()) {
          g.drawLine(0, 1, 0, h - 2);
          g.drawLine(0, 1, w - 2, 1);
          g.drawLine(0, h - 2, w - 2, h - 2);
          g.drawLine(w - 2, 1, w - 2, h - 2);
        }
        else {
          g.drawLine(0, 0, 0, h);
        }
        config.restore();
      }

      @Override
      public Dimension getPreferredSize() {
        int newSize = CustomizableComboBox.this.getHeight() - (CustomizableComboBox.this.getInsets().bottom + CustomizableComboBox.this.getInsets().top);
        return new Dimension(newSize, newSize);
      }
    };
    button.setOpaque(false);
    button.setFocusable(false);
    button.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 1));

    return button;
  }
}
