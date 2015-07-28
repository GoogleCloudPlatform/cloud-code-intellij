package com.google.gct.idea.cloudlogging;

import com.google.gct.idea.ui.CustomizableComboBox;
import com.google.gct.idea.ui.CustomizableComboBoxPopup;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.UIUtil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Custom Combo Box object to show the list for the combo box.
 * Created by amulyau on 7/26/15.
 */
public class LogLevelCustomCombo extends CustomizableComboBox implements CustomizableComboBoxPopup {
  private final AppEngineLogToolWindowView view;
  private JBPopup popup;
  private JBList list;
  private DefaultListModel listModel;
  private JBLabel label;
  private boolean enabled;

  public LogLevelCustomCombo(final AppEngineLogToolWindowView view) {
    this.remove(1); //remove default text field and add label instead
    this.view = view;
    this.listModel = new DefaultListModel();
    populateListModel();
    label.setOpaque(true);
    label.setBackground(UIUtil.getListBackground(false));
    label.setForeground(UIUtil.getListForeground(false));
    this.add(label);
    this.setPreferredSize(new Dimension(view.getMinComboboxWidth(), getPreferredPopupHeight()));
    this.list = new JBList(listModel);
    int height = this.getHeight();
    this.setSize(view.getMinComboboxWidth(), height);

    list.setBackground(UIUtil.getListBackground());
    list.setForeground(UIUtil.getListForeground());

    this.list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    this.list.setCellRenderer(new LogLevelComboBoxRenderer(view));
    enabled = false;
  }

  /**
   * Add mouse motion listeners to list
   * @param listener Log level Combo Listener mouse listener
   */
  public void addMouseListeners(LogLevelComboListener listener) {
    list.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {
        int index = list.getUI().locationToIndex(list,e.getPoint());
        list.setSelectionInterval(0, index);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        int index = list.getUI().locationToIndex(list,e.getPoint());
        list.setSelectionInterval(0, index);
      }
    });

    list.addMouseListener(listener);
  }

  /**
   * What to do when mouse clicks the combo box list
   * @param p Point the mouse clicked
   */
  public void mouseClicked(Point p) {
    int index = list.locationToIndex(p);
    list.setSelectionInterval(0,index);
    label.setText(((JBLabel) listModel.get(index)).getText());
    label.setIcon(((JBLabel) listModel.get(index)).getIcon());
    if ((popup!=null) && (popup.isVisible()) && (!popup.isDisposed())) {
      popup.dispose();
    }
    view.setCurrLogLevelSelected(index);
  }

  /**
   * Based on mouse point, sets the selected interval on log levels
   * @param p Point p form mouse event
   */
  public void setInterval(Point p) {
    int index = list.locationToIndex(p);
    list.setSelectionInterval(0, index);
  }

  /**
   * What to do when mouse exits the combo box list
   * @param p Point the mouse exited
   */
  public void mouseExited(Point p) {
    int index = view.getCurrLogLevelSelected();
    list.setSelectionInterval(0, index);
    label.setText(((JBLabel) listModel.get(index)).getText());
    label.setIcon(((JBLabel) listModel.get(index)).getIcon());
  }

  /**
   * Sets the label look as enabled to click
   * @param bool boolean value, if true then label is enabled, else false.
   */
  public void setEnabledView(boolean bool) {
    this.enabled = bool;
    this.label.setEnabled(bool);
  }

  /**
   * Populates the list and sets the label properly
   */
  private void populateListModel() {
    ArrayList<JBLabel> logLevelList = view.createLogLevelList();
    for (JBLabel label : logLevelList) {
      listModel.addElement(label);
    }
    JBLabel tempLabel = logLevelList.get(logLevelList.size()-1);
    label = new JBLabel(tempLabel.getText(), tempLabel.getIcon(), JBLabel.LEFT);
  }

  @Override
  protected CustomizableComboBoxPopup getPopup() {
    return this;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return 0;
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if(enabled) {
      if (((popup == null) || (popup.isDisposed())) && (enabled)) {
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(list, null).createPopup();
      }
      if (!popup.isVisible()) {
        popup.show(showTarget);
      }
      int logLevel = view.getCurrLogLevelSelected();
      list.setSelectionInterval(0, logLevel);
    }
  }

  @Override
  public void hidePopup() {
    if(popup.isVisible()) {
      popup.dispose();
    }
  }

  @Override
  public boolean isPopupVisible() {
    return popup !=null && !popup.isDisposed() && popup.isVisible();
  }

}
