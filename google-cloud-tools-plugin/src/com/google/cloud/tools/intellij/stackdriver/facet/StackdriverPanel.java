/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Stackdriver facet's user interface.
 */
public class StackdriverPanel extends FacetEditorTab {

  private JPanel stackdriverPanel;
  private JCheckBox generateSourceContext;
  private JCheckBox ignoreErrors;
  private HyperlinkLabel stackdriverInfo;
  private TextFieldWithBrowseButton moduleSourceDirectory;
  private JLabel moduleSourceDirectoryLabel;
  private JLabel warning;
  private StackdriverFacetConfiguration configuration;

  /**
   * @param configuration contains Stackdriver parameters
   * @param fromNewProjectDialog if {@code true}, hides the module source directory prompt
   */
  public StackdriverPanel(StackdriverFacetConfiguration configuration,
      boolean fromNewProjectDialog) {
    this.configuration = configuration;
    stackdriverInfo.setHyperlinkText(GctBundle.message("stackdriver.documentation"));
    stackdriverInfo.setHyperlinkTarget(GctBundle.message("stackdriver.documentation.url"));

    generateSourceContext.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        ignoreErrors.setEnabled(((JCheckBox)event.getSource()).isSelected());
      }
    });

    // If panel is summoned from the New Project/Module window, there are no possible module source
    // directory suggestions, so the directory prompt shouldn't be shown.
    moduleSourceDirectory.setVisible(!fromNewProjectDialog);
    moduleSourceDirectoryLabel.setVisible(!fromNewProjectDialog);
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return stackdriverPanel;
  }

  @Override
  public boolean isModified() {
    return isGenerateSourceContextSelected() != configuration.getState().isGenerateSourceContext()
        || isIgnoreErrorsSelected() != configuration.getState().isIgnoreErrors()
        || !getModuleSourceDirectory().equals(configuration.getState().getModuleSourceDirectory());
  }

  @Override
  public void apply() throws ConfigurationException {
    if (!CloudSdkService.getInstance().isValidCloudSdk()) {
      throw new RuntimeConfigurationException(GctBundle.message("stackdriver.sdk.misconfigured"));
    }
    configuration.getState().setGenerateSourceContext(isGenerateSourceContextSelected());
    configuration.getState().setIgnoreErrors(isIgnoreErrorsSelected());
    configuration.getState().setCloudSdkPath(
        CloudSdkService.getInstance().getSdkHomePath().toString());
    configuration.getState().setModuleSourceDirectory(moduleSourceDirectory.getText());
  }

  @Override
  public void reset() {
    if (!CloudSdkService.getInstance().isValidCloudSdk()) {
      warning.setForeground(Color.RED);
      warning.setText(GctBundle.message("stackdriver.sdk.misconfigured"));
    }
    generateSourceContext.setSelected(configuration.getState().isGenerateSourceContext());
    ignoreErrors.setEnabled(isGenerateSourceContextSelected());
    ignoreErrors.setSelected(configuration.getState().isIgnoreErrors());
    moduleSourceDirectory.setText(configuration.getState().getModuleSourceDirectory());
  }

  @Override
  public void disposeUIResources() {
    // Do nothing.
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GctBundle.getString("stackdriver.name");
  }

  public boolean isGenerateSourceContextSelected() {
    return generateSourceContext.isSelected();
  }

  public boolean isIgnoreErrorsSelected() {
    return ignoreErrors.isSelected();
  }

  public String getModuleSourceDirectory() {
    return moduleSourceDirectory.getText();
  }
}
