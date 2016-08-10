/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.server.instance;

import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;

import com.google.common.base.Joiner;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author nik
 */
public class AppEngineRunConfigurationEditor extends SettingsEditor<CommonModel> implements
    PanelWithAnchor {

  private JPanel myMainPanel;
  private JComboBox myArtifactComboBox;
  private JTextField port;
  private RawCommandLineEditor myServerParametersEditor;
  private JBLabel myWebArtifactToDeployLabel;
  private JBLabel myPortLabel;
  private JBLabel myServerParametersLabel;
  private JCheckBox enableAdvanced;
  private JPanel advancedSettingsPanel;
  private final Project myProject;
  private Artifact myLastSelectedArtifact;
  private JComponent anchor;
  private JTextField authDomain;
  private JTextField storagePath;
  private JTextField adminHost;
  private JTextField adminPort;
  private JTextField apiPort;

  public AppEngineRunConfigurationEditor(Project project) {
    myProject = project;
    myArtifactComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        onArtifactChanged();
      }
    });
    enableAdvanced.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        advancedSettingsPanel.setVisible(enableAdvanced.isSelected());
      }
    });

    setAnchor(myWebArtifactToDeployLabel);
  }

  private void onArtifactChanged() {
    final Artifact selectedArtifact = getSelectedArtifact();
    if (!Comparing.equal(myLastSelectedArtifact, selectedArtifact)) {
      if (myLastSelectedArtifact != null) {
        BuildArtifactsBeforeRunTaskProvider
            .setBuildArtifactBeforeRunOption(myMainPanel, myProject, myLastSelectedArtifact, false);
      }
      if (selectedArtifact != null) {
        BuildArtifactsBeforeRunTaskProvider
            .setBuildArtifactBeforeRunOption(myMainPanel, myProject, selectedArtifact, true);
      }
      myLastSelectedArtifact = selectedArtifact;
    }
  }

  protected void resetEditorFrom(CommonModel commonModel) {
    final AppEngineServerModel serverModel = (AppEngineServerModel) commonModel.getServerModel();
    final Artifact artifact = serverModel.getArtifact();
    myArtifactComboBox.setSelectedItem(artifact);
    if (artifact == null && myArtifactComboBox.getItemCount() == 1) {
      myArtifactComboBox.setSelectedIndex(0);
    }
    port.setText(String.valueOf(serverModel.getPort()));
    enableAdvanced.setSelected(serverModel.getAdvancedSettings());
    adminHost.setText(serverModel.getAdminHost());
    adminPort.setText(String.valueOf(serverModel.getAdminPort()));
    authDomain.setText(serverModel.getAuthDomain());
    storagePath.setText(serverModel.getStoragePath());
    myServerParametersEditor.setDialogCaption("Server Parameters");
    myServerParametersEditor.setText(Joiner.on(" ").join(serverModel.getJvmFlags()));
    apiPort.setText(String.valueOf(serverModel.getApiPort()));
    advancedSettingsPanel.setVisible(enableAdvanced.isSelected());
  }

  protected void applyEditorTo(CommonModel commonModel) throws ConfigurationException {
    final AppEngineServerModel serverModel = (AppEngineServerModel) commonModel.getServerModel();
    try {
      serverModel.setPort(Integer.parseInt(port.getText()));
    } catch (NumberFormatException nfe) {
      throw new ConfigurationException("'" + port.getText() + "' is not a valid port "
          + "number");
    }
    serverModel.setArtifact(getSelectedArtifact());
    serverModel.setAdvancedSettings(enableAdvanced.isSelected());

    if (enableAdvanced.isSelected()) {
      serverModel.setAdminHost(adminHost.getText());
      try {
        if (!adminPort.getText().isEmpty()) {
          serverModel.setAdminPort(Integer.parseInt(adminPort.getText()));
        }
      } catch (NumberFormatException nfe) {
        throw new ConfigurationException("'" + adminPort.getText() + "' is not a valid admin port "
            + "number.");
      }
      serverModel.setAuthDomain(authDomain.getText());
      serverModel.setStoragePath(storagePath.getText());
      serverModel.setJvmFlags(myServerParametersEditor.getText());
      try {
        if (!apiPort.getText().isEmpty()) {
          serverModel.setApiPort(Integer.parseInt(apiPort.getText()));
        }
      } catch (NumberFormatException nfe) {
        throw new ConfigurationException("'" + apiPort.getText() + "' is not a valid API port "
            + "number.");
      }
    }
    // TODO(joaomartins): What happens when there are already advanced settings serialized
    // and we turn advanced settings off?
  }

  private Artifact getSelectedArtifact() {
    return (Artifact) myArtifactComboBox.getSelectedItem();
  }

  @NotNull
  protected JComponent createEditor() {
    AppEngineUtilLegacy.setupAppEngineArtifactCombobox(myProject, myArtifactComboBox, false);
    return myMainPanel;
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    this.anchor = anchor;
    myWebArtifactToDeployLabel.setAnchor(anchor);
    myPortLabel.setAnchor(anchor);
    myServerParametersLabel.setAnchor(anchor);
  }
}
