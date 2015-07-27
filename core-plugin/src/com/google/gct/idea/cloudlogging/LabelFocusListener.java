package com.google.gct.idea.cloudlogging;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

/**
 * Focus Listener for the labels
 * Created by amulyau on 7/16/15.
 */
public class LabelFocusListener implements FocusListener {

  AppEngineLogToolWindowView view;
  JComboBox comboBox;

  /**
   * Constructor for focus listener for JBLabels
   * @param view View that holds all the components
   * @param comboBox Combo Box that has the labels that this focus listener has been applied to
   */
  public LabelFocusListener(AppEngineLogToolWindowView view, JComboBox comboBox) {

    this.view = view;
    this.comboBox = comboBox;
  }

  @Override
  public void focusGained(FocusEvent e) {

    System.out.println(comboBox.getSelectedIndex());
  }

  @Override
  public void focusLost(FocusEvent e) {

  }

}
