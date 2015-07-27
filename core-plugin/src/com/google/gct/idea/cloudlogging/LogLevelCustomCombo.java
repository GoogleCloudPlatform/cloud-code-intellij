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
import java.awt.event.MouseAdapter;
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

    this.list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    this.list.setCellRenderer(new LogLevelComboBoxRenderer(view));

    this.list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        view.setCurrLogLevelSelected(index);
        list.setSelectionInterval(0, index);
        label.setText(((JBLabel) listModel.get(index)).getText());
        label.setIcon(((JBLabel) listModel.get(index)).getIcon());
        popup.dispose();
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        list.setSelectionInterval(0, index);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        int index = view.getCurrLogLevelSelected();
        list.setSelectionInterval(0, index);
        label.setText(((JBLabel) listModel.get(index)).getText());
        label.setIcon(((JBLabel) listModel.get(index)).getIcon());
      }
    });
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
    if ((popup == null) || (popup.isDisposed())) {
      popup = JBPopupFactory.getInstance().createComponentPopupBuilder(list, null).createPopup();
    }
    if(!popup.isVisible()){
      popup.show(showTarget);
    }
    int logLevel = view.getCurrLogLevelSelected();
    list.setSelectionInterval(0,logLevel);
  }

  @Override
  public void hidePopup() {

  }

  @Override
  public boolean isPopupVisible() {
    return false;
  }

}
