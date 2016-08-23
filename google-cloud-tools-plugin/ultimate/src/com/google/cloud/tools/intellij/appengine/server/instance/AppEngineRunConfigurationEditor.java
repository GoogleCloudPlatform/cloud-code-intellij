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
import com.google.cloud.tools.intellij.util.GctBundle;
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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 * @author nik
 */
public class AppEngineRunConfigurationEditor extends SettingsEditor<CommonModel> implements
    PanelWithAnchor {

  private JPanel myMainPanel;
  private JComboBox myArtifactComboBox;
  private JTextField port;
  private RawCommandLineEditor jvmFlags;
  private JBLabel myWebArtifactToDeployLabel;
  private JBLabel myPortLabel;
  private JBLabel myServerParametersLabel;
  private JPanel advancedSettingsPanel;
  private final Project myProject;
  private Artifact myLastSelectedArtifact;
  private JComponent anchor;
  private JTextField authDomain;
  private JTextField storagePath;
  private JTextField adminHost;
  private JTextField adminPort;
  private JTextField apiPort;
  private JTextField host;
  private JComboBox logLevel;
  private JTextField maxModuleInstances;
  private JCheckBox useMtimeFileWatcher;
  private JTextField threadsafeOverride;
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
  private JTabbedPane tabbedPane1;
  private JCheckBox allowSkippedFiles;
  private JCheckBox automaticRestart;
  private JComboBox devappserverLogLevel;
  private JCheckBox skipSdkUpdateCheck;
  private JTextField gcsBucketName;
  // TODO(joaomartins): Change "Advanced Settings" to a collapsable drop down, like Before Launch.

  public AppEngineRunConfigurationEditor(Project project) {
    myProject = project;
    myArtifactComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        onArtifactChanged();
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
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(
          commonModel.getProject(), commonModel, (Artifact) myArtifactComboBox.getSelectedItem());
    }
    port.setText(serverModel.getPort() != null
        ? String.valueOf(serverModel.getPort()) : "");
    host.setText(serverModel.getHost());
    adminHost.setText(serverModel.getAdminHost());
    adminPort.setText(serverModel.getAdminPort() != null
        ? String.valueOf(serverModel.getAdminPort()) : "");
    authDomain.setText(serverModel.getAuthDomain());
    storagePath.setText(serverModel.getStoragePath());
    logLevel.setSelectedItem(serverModel.getLogLevel());
    maxModuleInstances.setText(serverModel.getMaxModuleInstances() != null
        ? String.valueOf(serverModel.getMaxModuleInstances()) : "");
    useMtimeFileWatcher.setSelected(serverModel.getUseMtimeFileWatcher());
    threadsafeOverride.setText(serverModel.getThreadsafeOverride());
    jvmFlags.setDialogCaption(GctBundle.getString("appengine.run.jvmflags.title"));
    jvmFlags.setText(Joiner.on(" ").join(serverModel.getJvmFlags()));
    allowSkippedFiles.setSelected(serverModel.getAllowSkippedFiles());
    apiPort.setText(serverModel.getApiPort() != null
        ? String.valueOf(serverModel.getApiPort()) : "");
    automaticRestart.setSelected(serverModel.getAutomaticRestart());
    devappserverLogLevel.setSelectedItem(serverModel.getDevAppserverLogLevel());
    skipSdkUpdateCheck.setSelected(serverModel.getSkipSdkUpdateCheck());
    gcsBucketName.setText(serverModel.getDefaultGcsBucketName());
  }

  protected void applyEditorTo(CommonModel commonModel) throws ConfigurationException {
    final AppEngineServerModel serverModel = (AppEngineServerModel) commonModel.getServerModel();
    serverModel.setPort(validateInteger(port.getText(), "port"));
    serverModel.setArtifact(getSelectedArtifact());

    serverModel.setHost(host.getText());
    serverModel.setAdminHost(adminHost.getText());
    if (!adminPort.getText().isEmpty()) {
      serverModel.setAdminPort(validateInteger(adminPort.getText(), "admin port"));
    }
    serverModel.setAuthDomain(authDomain.getText());
    serverModel.setStoragePath(storagePath.getText());
    serverModel.setLogLevel((String) logLevel.getSelectedItem());
    if (!maxModuleInstances.getText().isEmpty()) {
      serverModel.setMaxModuleInstances(validateInteger(
          maxModuleInstances.getText(), "maximum module instances"));
    }
    serverModel.setUseMtimeFileWatcher(useMtimeFileWatcher.isSelected());
    serverModel.setThreadsafeOverride(threadsafeOverride.getText());
    serverModel.setJvmFlags(jvmFlags.getText());
    serverModel.setAllowSkippedFiles(allowSkippedFiles.isSelected());
    if (!apiPort.getText().isEmpty()) {
      serverModel.setApiPort(validateInteger(apiPort.getText(), "API port"));
    }
    serverModel.setAutomaticRestart(automaticRestart.isSelected());
    serverModel.setDevAppserverLogLevel((String) devappserverLogLevel.getSelectedItem());
    serverModel.setSkipSdkUpdateCheck(skipSdkUpdateCheck.isSelected());
    serverModel.setDefaultGcsBucketName(gcsBucketName.getText());
  }

  private Integer validateInteger(String intText, String description)
      throws ConfigurationException {
    try {
      return Integer.parseInt(intText);
    } catch (NumberFormatException nfe) {
      throw new ConfigurationException(
          "'" + intText + "' is not a valid " + description + " number.");
    }
  }

  private Artifact getSelectedArtifact() {
    return (Artifact) myArtifactComboBox.getSelectedItem();
  }

  @NotNull
  protected JComponent createEditor() {
    // TODO(joaomartins): Switch to AppEngineProjectService and deprecate AppEngineUtilLegacy.
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
