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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import static java.util.stream.Collectors.toList;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.ModulePathPair.ConfigurationFileType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.ModulePathPair.ModulePathPairRenderer;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.impl.ui.FacetEditorFacade;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * Flexible deployment run configuration user interface.
 */
public class AppEngineFlexibleDeploymentEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {
  private static final String DEFAULT_SERVICE = "default";
  private static final String LABEL_OPEN_TAG = "<html><font face='sans' size='-1'>";
  private static final String LABEL_CLOSE_TAG = "</font></html>";
  private static final String LABEL_HREF_CLOSE_TAG = "</a>";
  private static final String COST_WARNING_HREF_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/pricing'>";
  private static final String PROMOTE_INFO_HREF_OPEN_TAG =
      "<a href='https://console.cloud.google.com/appengine/versions'>";
  private static final AppEngineProjectService APP_ENGINE_PROJECT_SERVICE =
      AppEngineProjectService.getInstance();

  private JPanel mainPanel;
  private JBTextField version;
  private JCheckBox promoteVersionCheckBox;
  private JCheckBox stopPreviousVersionCheckBox;
  private ProjectSelector gcpProjectSelector;
  private JLabel serviceLabel;
  private TextFieldWithBrowseButton yamlTextField;
  private TextFieldWithBrowseButton dockerfileTextField;
  private TextFieldWithBrowseButton archiveSelector;
  private JComboBox<ConfigType> configurationTypeComboBox;
  private JTextPane appEngineCostWarningLabel;
  private AppEngineApplicationInfoPanel appInfoPanel;
  private JPanel archiveSelectorPanel;
  private JTextPane promoteInfoLabel;
  private JLabel dockerfileLabel;
  private JTextPane filesWarningLabel;
  private JLabel yamlLabel;
  private JComboBox<ModulePathPair> presetYamls;
  private JCheckBox yamlOverrideCheckBox;
  private JComboBox<ModulePathPair> presetDockerfiles;
  private JCheckBox dockerfileOverrideCheckBox;
  private JButton yamlModuleSettings;
  private DeploymentSource deploymentSource;

  public AppEngineFlexibleDeploymentEditor(Project project, DeploymentSource deploymentSource) {
    this.deploymentSource = deploymentSource;
    version.getEmptyText().setText(GctBundle.getString("appengine.flex.version.placeholder.text"));
    configurationTypeComboBox.setModel(new DefaultComboBoxModel<>(ConfigType.values()));
    configurationTypeComboBox.setSelectedItem(ConfigType.CUSTOM);
    yamlTextField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml"))
    );

    dockerfileTextField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.dockerfile"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor()
    );

    archiveSelector.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.user.specified.artifact.title"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile ->
                Comparing.equal(
                    virtualFile.getExtension(), "jar", SystemInfo.isFileSystemCaseSensitive)
                || Comparing.equal(
                    virtualFile.getExtension(), "war", SystemInfo.isFileSystemCaseSensitive)
        )
    );

    archiveSelector.getTextField().getDocument().addDocumentListener(
        new DocumentAdapter() {
          @Override
          protected void textChanged(DocumentEvent event) {
            if (deploymentSource instanceof UserSpecifiedPathDeploymentSource) {
              ((UserSpecifiedPathDeploymentSource) deploymentSource).setFilePath(
                  archiveSelector.getText());
            }
          }
        }
    );

    configurationTypeComboBox.addItemListener(
        event -> {
          boolean isCustomSelected = event.getItem().equals(ConfigType.CUSTOM);
          yamlOverrideCheckBox.setEnabled(isCustomSelected);
          dockerfileOverrideCheckBox.setEnabled(isCustomSelected);
          if (isCustomSelected) {
            yamlOverrideCheckBox.setSelected(false);
            dockerfileOverrideCheckBox.setSelected(false);
          }
        }
    );

    yamlOverrideCheckBox.addActionListener(
        event -> {
          boolean isYamlOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
          presetYamls.setEnabled(!isYamlOverrideSelected);
          yamlTextField.setVisible(isYamlOverrideSelected);
        });

    dockerfileOverrideCheckBox.addActionListener(
        event -> {
          boolean isDockerfileOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
          presetDockerfiles.setEnabled(!isDockerfileOverrideSelected);
          dockerfileTextField.setVisible(isDockerfileOverrideSelected);
        });

    yamlTextField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent event) {
        updateServiceName();
        setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
            yamlTextField.getText()).equals(FlexibleRuntime.CUSTOM));
        checkConfigurationFiles();
      }

      @Override
      public void removeUpdate(DocumentEvent event) {
        updateServiceName();
        setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
            yamlTextField.getText()).equals(FlexibleRuntime.CUSTOM));
        checkConfigurationFiles();
      }

      @Override
      public void changedUpdate(DocumentEvent event) {
        updateServiceName();
        setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
            yamlTextField.getText()).equals(FlexibleRuntime.CUSTOM));
        checkConfigurationFiles();
      }
    });

    appEngineCostWarningLabel.setText(
        GctBundle.message("appengine.flex.deployment.cost.warning",
            LABEL_OPEN_TAG,
            COST_WARNING_HREF_OPEN_TAG,
            LABEL_HREF_CLOSE_TAG,
            LABEL_CLOSE_TAG));
    appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appEngineCostWarningLabel.setBackground(mainPanel.getBackground());

    gcpProjectSelector.addProjectSelectionListener(event ->
        appInfoPanel.refresh(event.getSelectedProject().getProjectId(),
            event.getUser().getCredential()));
    gcpProjectSelector.addModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent event) {
        // Do nothing.
      }

      @Override
      public void treeNodesInserted(TreeModelEvent event) {
        // Do nothing.
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent event) {
        // Do nothing.
      }

      @Override
      public void treeStructureChanged(TreeModelEvent event) {
        // projects have finished loading
        refreshApplicationInfoPanel();
      }
    });

    promoteInfoLabel.setText(
        GctBundle.message("appengine.promote.info.label",
            LABEL_OPEN_TAG,
            PROMOTE_INFO_HREF_OPEN_TAG,
            LABEL_HREF_CLOSE_TAG,
            LABEL_CLOSE_TAG));
    promoteInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());

    promoteVersionCheckBox.addItemListener(event -> {
      boolean isPromoteSelected = ((JCheckBox) event.getItem()).isSelected();

      stopPreviousVersionCheckBox.setEnabled(isPromoteSelected);

      if (!isPromoteSelected) {
        stopPreviousVersionCheckBox.setSelected(false);
      }
    });
    stopPreviousVersionCheckBox.setEnabled(false);

    presetYamls.setModel(getComboBoxModelForFileType(ConfigurationFileType.APP_YAML, project));
    presetYamls.setRenderer(new ModulePathPairRenderer());

    presetDockerfiles.setModel(
        getComboBoxModelForFileType(ConfigurationFileType.DOCKERFILE, project));
    presetDockerfiles.setRenderer(new ModulePathPairRenderer());

    filesWarningLabel.setForeground(Color.RED);

//    yamlModuleSettings.addMouseListener(new MouseListener() {
//      @Override
//      public void mouseClicked(MouseEvent e) {
//        FacetEditorFacade
//        ShowSettingsUtil.getInstance().editConfigurable(project, )
//      }
//
//      @Override
//      public void mousePressed(MouseEvent e) {
//
//      }
//
//      @Override
//      public void mouseReleased(MouseEvent e) {
//
//      }
//
//      @Override
//      public void mouseEntered(MouseEvent e) {
//
//      }
//
//      @Override
//      public void mouseExited(MouseEvent e) {
//
//      }
//    });

    updateSelectors();
    checkConfigurationFiles();
  }

  private ComboBoxModel<ModulePathPair> getComboBoxModelForFileType(ConfigurationFileType fileType,
      Project project) {
    return new DefaultComboBoxModel<>(
        Arrays.stream(ModuleManager.getInstance(project).getModules())
            .map(module -> new ModulePathPair(module, fileType))
            .collect(toList())
            .toArray(new ModulePathPair[ModuleManager.getInstance(project).getModules().length])
    );
  }

  private void refreshApplicationInfoPanel() {
    if (gcpProjectSelector.getProject() != null && gcpProjectSelector.getSelectedUser() != null) {
      appInfoPanel.refresh(gcpProjectSelector.getProject().getProjectId(),
          gcpProjectSelector.getSelectedUser().getCredential());
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    version.setText(configuration.getVersion());
    promoteVersionCheckBox.setSelected(configuration.isPromote());
    stopPreviousVersionCheckBox.setSelected(configuration.isStopPreviousVersion());
    yamlTextField.setText(configuration.getAppYamlPath());
    dockerfileTextField.setText(configuration.getDockerFilePath());
    gcpProjectSelector.setText(configuration.getCloudProjectName());
    configurationTypeComboBox.setSelectedItem(configuration.getConfigType());
    yamlTextField.setVisible(
        configuration.getConfigType().equals(ConfigType.CUSTOM) && configuration.isOverrideYaml());
    dockerfileTextField.setVisible(configuration.getConfigType().equals(ConfigType.CUSTOM)
        && configuration.isOverrideDockerfile());
    archiveSelector.setText(configuration.getUserSpecifiedArtifactPath());
    yamlOverrideCheckBox.setSelected(configuration.isOverrideYaml());
    dockerfileOverrideCheckBox.setSelected(configuration.isOverrideDockerfile());
    presetYamls.setEnabled(!configuration.isOverrideYaml());
    presetDockerfiles.setEnabled(!configuration.isOverrideDockerfile());

    setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
        yamlTextField.getText()).equals(FlexibleRuntime.CUSTOM));
    updateServiceName();
    refreshApplicationInfoPanel();
    checkConfigurationFiles();
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    configuration.setVersion(version.getText());
    configuration.setPromote(promoteVersionCheckBox.isSelected());
    configuration.setStopPreviousVersion(stopPreviousVersionCheckBox.isSelected());
    configuration.setAppYamlPath(getYamlPath());
    configuration.setDockerFilePath(getDockerfilePath());
    configuration.setCloudProjectName(gcpProjectSelector.getText());
    CredentialedUser user = gcpProjectSelector.getSelectedUser();
    if (user != null) {
      configuration.setGoogleUsername(user.getEmail());
    }
    configuration.setConfigType((ConfigType) configurationTypeComboBox.getSelectedItem());
    configuration.setEnvironment(
        ((AppEngineArtifactDeploymentSource) deploymentSource).getEnvironment().name());
    configuration.setUserSpecifiedArtifact(
        deploymentSource instanceof UserSpecifiedPathDeploymentSource);
    configuration.setUserSpecifiedArtifactPath(archiveSelector.getText());
    configuration.setOverrideYaml(yamlOverrideCheckBox.isSelected());
    configuration.setOverrideDockerfile(dockerfileOverrideCheckBox.isSelected());
    updateSelectors();
    checkConfigurationFiles();
  }

  private void validateConfiguration() throws ConfigurationException {
    if (deploymentSource instanceof UserSpecifiedPathDeploymentSource
        && (StringUtil.isEmpty(archiveSelector.getText())
        || !isJarOrWar(archiveSelector.getText()))) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.user.specified.artifact.error"));
    }
    if (!(deploymentSource instanceof UserSpecifiedPathDeploymentSource)
        && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    }
    if (StringUtils.isBlank(gcpProjectSelector.getText())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.project.missing.message"));
    }
    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk();
    if (validationResults.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
      throw new ConfigurationException(GctBundle.message(
          "appengine.cloudsdk.deploymentconfiguration.location.invalid.message"));
    }
    if (configurationTypeComboBox.getSelectedItem().equals(ConfigType.CUSTOM)
        && yamlOverrideCheckBox.isSelected()) {
      if (StringUtils.isBlank(yamlTextField.getText())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.browse.app.yaml"));
      }
      try {
        if (!Files.exists(Paths.get(getYamlPath()))) {
          throw new ConfigurationException(
              GctBundle.getString("appengine.deployment.error.staging.yaml"));
        }
      } catch (InvalidPathException ipe) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.badchars", "Yaml"));
      }
      if (APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
          getYamlPath()).equals(FlexibleRuntime.CUSTOM)) {
        if (StringUtils.isBlank(getDockerfilePath())) {
          throw new ConfigurationException(
              GctBundle.message("appengine.flex.config.browse.dockerfile"));
        }
        try {
          if (!Files.exists(Paths.get(getDockerfilePath()))) {
            throw new ConfigurationException(
                GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
          }
        } catch (InvalidPathException ipe) {
          throw new ConfigurationException(
              GctBundle.message("appengine.flex.config.badchars", "Dockerfile"));
        }
      }
    }
    if (!appInfoPanel.isApplicationValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.application.required.deployment"));
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return mainPanel;
  }

  private void updateServiceName() {
    Optional<String> service = APP_ENGINE_PROJECT_SERVICE.getServiceNameFromAppYaml(
        yamlTextField.getText());
    serviceLabel.setText(service.orElse(DEFAULT_SERVICE));
  }

  private void updateSelectors() {
    archiveSelectorPanel.setVisible(deploymentSource instanceof UserSpecifiedPathDeploymentSource);
  }

  private boolean isJarOrWar(String stringPath) {
    try {
      Path path = Paths.get(stringPath);
      return !Files.isDirectory(path) && (StringUtil.endsWithIgnoreCase(stringPath, ".jar")
          || StringUtil.endsWithIgnoreCase(stringPath, ".war"));
    } catch (InvalidPathException ipe) {
      return false;
    }
  }

  /**
   * Hides the Dockerfile section of the UI. For example, when app.yaml contains "runtime: java".
   */
  private void setDockerfileVisibility(boolean visible) {
    dockerfileLabel.setVisible(visible);
    presetDockerfiles.setVisible(visible);
    dockerfileOverrideCheckBox.setVisible(visible);
    dockerfileTextField.setVisible(dockerfileOverrideCheckBox.isSelected());
  }

  private void checkConfigurationFiles() {
    checkConfigurationFile(getYamlPath(), yamlTextField.getTextField(), yamlLabel);
    if (APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(yamlTextField.getText())
        .equals(FlexibleRuntime.CUSTOM)) {
      checkConfigurationFile(getDockerfilePath(), dockerfileTextField.getTextField(),
          dockerfileLabel);
    }

    filesWarningLabel.setVisible(yamlTextField.getTextField().getForeground().equals(Color.RED)
        || (APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(yamlTextField.getText())
        .equals(FlexibleRuntime.CUSTOM)
        && dockerfileTextField.getTextField().getForeground().equals(Color.RED)));
  }

  /**
   * Checks if a specified configuration file is valid or not and triggers UI warnings
   * accordingly.
   */
  private void checkConfigurationFile(String path, JTextField textField, JLabel label) {
    try {
      if (!Files.exists(Paths.get(path)) || !Files.isRegularFile(Paths.get(path))) {
        textField.setForeground(Color.RED);
        label.setForeground(Color.RED);
      } else {
        textField.setForeground(Color.BLACK);
        label.setForeground(Color.BLACK);
      }
    } catch (InvalidPathException ipe) {
      textField.setForeground(Color.RED);
      label.setForeground(Color.BLACK);
    }
  }

  /**
   * Returns the final Yaml file path from the combobox or text field, depending on if it's
   * overridden.
   */
  private String getYamlPath() {
    return yamlOverrideCheckBox.isSelected() ? yamlTextField.getText()
        : ((ModulePathPair) presetYamls.getSelectedItem()).getPath();
  }

  private String getDockerfilePath () {
    return dockerfileOverrideCheckBox.isSelected() ? dockerfileTextField.getText()
        : ((ModulePathPair) presetDockerfiles.getSelectedItem()).getPath();
  }

  @VisibleForTesting
  JCheckBox getPromoteVersionCheckBox() {
    return promoteVersionCheckBox;
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckBox() {
    return stopPreviousVersionCheckBox;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getYamlTextField() {
    return yamlTextField;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerfileTextField() {
    return dockerfileTextField;
  }
}
