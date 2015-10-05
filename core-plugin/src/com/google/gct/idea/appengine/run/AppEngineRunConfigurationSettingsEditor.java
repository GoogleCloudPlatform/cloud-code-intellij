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
package com.google.gct.idea.appengine.run;

import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * GUI for configuring App Engine Run Configurations
 */
public class AppEngineRunConfigurationSettingsEditor extends SettingsEditor<AppEngineRunConfiguration> {

  private JTextField myServerPortField;
  private JPanel myMainPanel;
  private JComboBox myModuleComboBox;
  private JTextField myVmArgsField;
  private TextFieldWithBrowseButton myWarPathField;
  private TextFieldWithBrowseButton myAppEngineSdkField;
  private JTextField myServerAddressField;
  private JCheckBox mySynchronizeWithBuildGradleCheckBox;
  private JCheckBox myUpdateCheckCheckBox;
  private final Project myProject;
  private final ConfigurationModuleSelector myModuleSelector;

  public AppEngineRunConfigurationSettingsEditor(Project project) {
    myProject = project;
    myModuleSelector = new ConfigurationModuleSelector(project, myModuleComboBox);
    myModuleComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateSync();
      }
    });
    mySynchronizeWithBuildGradleCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateSync();
      }
    });
    myAppEngineSdkField.addBrowseFolderListener("Select App Engine Sdk Root", null, null,
        FileChooserDescriptorFactory.createSingleFolderDescriptor());
    myWarPathField.addBrowseFolderListener("Select Exploded War Root", null, myProject,
        FileChooserDescriptorFactory.createSingleFolderDescriptor());
  }

  protected void updateSync() {
    boolean isSync = mySynchronizeWithBuildGradleCheckBox.isSelected();
    if (isSync) {
      syncWithBuildFileViaFacet();
    }
    myWarPathField.setEditable(!isSync);
    myServerAddressField.setEditable(!isSync);
    myServerPortField.setEditable(!isSync);
    myAppEngineSdkField.setEditable(!isSync);
    myVmArgsField.setEditable(!isSync);
    myUpdateCheckCheckBox.setEnabled(!isSync);
  }

  // Syncs a run configuration with information from build.gradle via the App Engine Gradle facet
  // This is also a duplicate of the sync in AppEngineRunConfiguration#syncWithBuildFileViaFacet,
  // but this is required here to update the UI correctly when "sync" is checked and turned on,
  // if we didn't reflect the configuration in the UI, we wouldn't need this.
  protected void syncWithBuildFileViaFacet() {
    myServerPortField.setText("");
    myServerAddressField.setText("");
    myAppEngineSdkField.setText("");
    myWarPathField.setText("");
    myVmArgsField.setText("");
    myUpdateCheckCheckBox.setSelected(false);

    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(myModuleSelector.getModule());
    if (facet != null) {
      AppEngineConfigurationProperties model = facet.getConfiguration().getState();
      if (model != null) {
        myServerPortField.setText(model.HTTP_PORT.toString());
        myServerAddressField.setText(model.HTTP_ADDRESS);
        myAppEngineSdkField.setText(model.APPENGINE_SDKROOT);
        myWarPathField.setText(model.WAR_DIR);
        myVmArgsField.setText(model.getJvmFlags());
        myUpdateCheckCheckBox.setSelected(model.DISABLE_UPDATE_CHECK);
      }
    }
  }

  @Override
  protected void resetEditorFrom(AppEngineRunConfiguration configuration) {
    if (configuration.getSdkPath() != null) {
      myAppEngineSdkField.setText(configuration.getSdkPath());
    }
    if (configuration.getWarPath() != null) {
      myWarPathField.setText(configuration.getWarPath());
    }
    myServerPortField.setText(configuration.getServerPort());
    myServerAddressField.setText(configuration.getServerAddress());
    myVmArgsField.setText(configuration.getVmArgs());
    myModuleSelector.reset(configuration);
    mySynchronizeWithBuildGradleCheckBox.setSelected(configuration.isSyncWithGradle());
    myUpdateCheckCheckBox.setSelected(configuration.isDisableUpdateCheck());
    updateSync();
  }

  @Override
  protected void applyEditorTo(AppEngineRunConfiguration configuration) throws ConfigurationException {
    myModuleSelector.applyTo(configuration);
    configuration.setSdkPath(myAppEngineSdkField.getText());
    configuration.setServerAddress(myServerAddressField.getText());
    configuration.setServerPort(myServerPortField.getText());
    configuration.setVmArgs(myVmArgsField.getText());
    configuration.setWarPath(myWarPathField.getText());
    configuration.setSyncWithGradle(mySynchronizeWithBuildGradleCheckBox.isSelected());
    configuration.setDisableUpdateCheck(myUpdateCheckCheckBox.isSelected());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }

  @Override
  protected void disposeEditor() {
    // nothing here
  }

  {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    myMainPanel = new JPanel();
    myMainPanel.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 0, 0), -1, -1));
    final JLabel label1 = new JLabel();
    label1.setText("War Path");
    myMainPanel.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    label2.setText("Server Port");
    myMainPanel.add(label2, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myServerPortField = new JTextField();
    myServerPortField.setToolTipText("Defaults to 8080");
    myMainPanel.add(myServerPortField,
        new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    final JLabel label3 = new JLabel();
    label3.setText("App Engine SDK");
    myMainPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label4 = new JLabel();
    label4.setText("VM Args");
    myMainPanel.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myVmArgsField = new JTextField();
    myMainPanel.add(myVmArgsField,
        new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    myAppEngineSdkField = new TextFieldWithBrowseButton();
    myMainPanel.add(myAppEngineSdkField,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myWarPathField = new TextFieldWithBrowseButton();
    myMainPanel.add(myWarPathField,
        new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myServerAddressField = new JTextField();
    myServerAddressField.setToolTipText("Defaults to \"localhost\" (i.e. 127.0.0.1)");
    myMainPanel.add(myServerAddressField,
        new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    final JLabel label5 = new JLabel();
    label5.setText("Server Address");
    myMainPanel.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label6 = new JLabel();
    label6.setText("Module");
    myMainPanel.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    myMainPanel.add(spacer1,
        new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    myModuleComboBox = new JComboBox();
    myMainPanel.add(myModuleComboBox,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    mySynchronizeWithBuildGradleCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(mySynchronizeWithBuildGradleCheckBox,
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("appengine.run.sync.checkbox"));
    myMainPanel.add(mySynchronizeWithBuildGradleCheckBox,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myUpdateCheckCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(myUpdateCheckCheckBox,
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("appengine.run.disable.check.updates"));
    myUpdateCheckCheckBox.setToolTipText(
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("appengine.run.disable.check.updates.tooltip"));
    myMainPanel.add(myUpdateCheckCheckBox,
        new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return myMainPanel;
  }
}
