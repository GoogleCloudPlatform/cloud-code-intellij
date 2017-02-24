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

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeployable;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineRuntimeInstance;
import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.tree.TreeModelAdapter;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelEvent;

/**
 * Flexible deployment run configuration user interface.
 */
public class AppEngineFlexibleDeploymentEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {
  private static final String DEFAULT_SERVICE = "default";
  private static final AppEngineProjectService APP_ENGINE_PROJECT_SERVICE =
      AppEngineProjectService.getInstance();

  private JPanel mainPanel;
  private JBTextField version;
  private JCheckBox promoteVersionCheckBox;
  private JCheckBox stopPreviousVersionCheckBox;
  private ProjectSelector gcpProjectSelector;
  private JLabel serviceLabel;
  private TextFieldWithBrowseButton appYamlTextField;
  private TextFieldWithBrowseButton dockerfileTextField;
  private TextFieldWithBrowseButton archiveSelector;
  private HyperlinkLabel appEngineCostWarningLabel;
  private AppEngineApplicationInfoPanel appInfoPanel;
  private JPanel archiveSelectorPanel;
  private HyperlinkLabel promoteInfoLabel;
  private JLabel dockerfileLabel;
  private JComboBox<Module> modulesWithFlexFacetComboBox;
  private JCheckBox appYamlOverrideCheckBox;
  private JCheckBox dockerfileOverrideCheckBox;
  private String dockerfileOverride = "";
  private JButton moduleSettingsButton;
  private JCheckBox hiddenValidationTrigger;
  private JLabel noSupportedModulesWarning;
  private DeploymentSource deploymentSource;

  public AppEngineFlexibleDeploymentEditor(Project project, AppEngineDeployable deploymentSource) {
    this.deploymentSource = deploymentSource;
    version.getEmptyText().setText(GctBundle.getString("appengine.flex.version.placeholder.text"));
    appYamlTextField.addBrowseFolderListener(
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

    appYamlOverrideCheckBox.addActionListener(event -> {
      boolean isAppYamlOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
      appYamlTextField.setVisible(isAppYamlOverrideSelected);
      toggleDockerfileSection();
    });

    dockerfileOverrideCheckBox.addActionListener(
        event -> {
          boolean isDockerfileOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
          dockerfileTextField.setEnabled(isDockerfileOverrideSelected);
          if (isDockerfileOverrideSelected) {
            if (dockerfileOverride.isEmpty()) {
              dockerfileOverride = dockerfileTextField.getText();
            }
            dockerfileTextField.setText(dockerfileOverride);
          } else {
            dockerfileOverride = dockerfileTextField.getText();
            dockerfileTextField.setText(getDockerfilePath());
          }
        }
    );

    appYamlTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        updateServiceName();
        toggleDockerfileSection();
      }
    });

    appEngineCostWarningLabel.setHyperlinkText(
        GctBundle.getString("appengine.flex.deployment.cost.warning.beforeLink"),
        GctBundle.getString("appengine.flex.deployment.cost.warning.link"),
        " " + GctBundle.getString("appengine.flex.deployment.cost.warning.afterLink"));
    appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appEngineCostWarningLabel.setHyperlinkTarget(CloudSdkAppEngineHelper.APP_ENGINE_BILLING_URL);

    gcpProjectSelector.addProjectSelectionListener(appInfoPanel::refresh);
    gcpProjectSelector.addModelListener(new TreeModelAdapter() {
      @Override
      public void treeStructureChanged(TreeModelEvent event) {
        // projects have finished loading
        refreshApplicationInfoPanel();
      }
    });

    promoteInfoLabel.setHyperlinkText(
        GctBundle.getString("appengine.promote.info.label.beforeLink") + " ",
        GctBundle.getString("appengine.promote.info.label.link"),
        "");
    promoteInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    promoteInfoLabel.setHyperlinkTarget(GctBundle.getString("appengine.promoteinfo.url"));

    promoteVersionCheckBox.addItemListener(event -> {
      boolean isPromoteSelected = ((JCheckBox) event.getItem()).isSelected();

      stopPreviousVersionCheckBox.setEnabled(isPromoteSelected);

      if (!isPromoteSelected) {
        stopPreviousVersionCheckBox.setSelected(false);
      }
    });
    stopPreviousVersionCheckBox.setEnabled(false);

    modulesWithFlexFacetComboBox.setModel(new DefaultComboBoxModel<>(
        Arrays.stream(ModuleManager.getInstance(project).getModules())
            .filter(module ->
                FacetManager.getInstance(module)
                    .getFacetByType(AppEngineFlexibleFacetType.ID) != null)
            .toArray(Module[]::new)
    ));
    modulesWithFlexFacetComboBox.addItemListener(event -> toggleDockerfileSection());
    modulesWithFlexFacetComboBox.setRenderer(new ListCellRendererWrapper<Module>() {
      @Override
      public void customize(JList list, Module value, int index, boolean selected,
          boolean hasFocus) {
        if (value != null) {
          setText(value.getName());
        }
      }
    });

    // For the case Flex isn't enabled for any modules, the user can still deploy filesystem
    // jars/wars.
    if (modulesWithFlexFacetComboBox.getItemCount() == 0) {
      modulesWithFlexFacetComboBox.setVisible(false);
      moduleSettingsButton.setVisible(false);
      appYamlOverrideCheckBox.setVisible(false);
      dockerfileOverrideCheckBox.setVisible(false);
      noSupportedModulesWarning.setVisible(true);
      appYamlTextField.setVisible(true);
      dockerfileTextField.setVisible(true);
      // These checks are important so getAppYamlPath() and getDockerfilePath() work correctly.
      appYamlOverrideCheckBox.setSelected(true);
      dockerfileOverrideCheckBox.setSelected(true);
    }

    moduleSettingsButton.addActionListener(event -> {
      AppEngineFlexibleFacet flexFacet =
          FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
              .getFacetByType(AppEngineFlexibleFacetType.ID);
      ModulesConfigurator.showFacetSettingsDialog(flexFacet, null /* tabNameToSelect */);
      // When we get out of the dialog window, we want to re-eval the configuration.
      // validateConfiguration() can't be used here because the ConfigurationException isn't caught
      // anywhere, and fireEditorStateChanged() doesn't trigger any listeners called from here.
      // Emulating a user action triggers apply(), so that's what we're doing here.
      hiddenValidationTrigger.doClick();
      toggleDockerfileSection();
    });

    updateSelectors();
    toggleDockerfileSection();
  }

  private void refreshApplicationInfoPanel() {
    if (gcpProjectSelector.getProject() != null && gcpProjectSelector.getSelectedUser() != null) {
      appInfoPanel.refresh(gcpProjectSelector.getProject().getProjectId(),
          gcpProjectSelector.getSelectedUser().getCredential());
    } else {
      appInfoPanel.setMessage(GctBundle.getString("appengine.infopanel.noproject"),
          true /* isError*/);
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    version.setText(configuration.getVersion());
    promoteVersionCheckBox.setSelected(configuration.isPromote());
    stopPreviousVersionCheckBox.setSelected(configuration.isStopPreviousVersion());
    appYamlTextField.setText(configuration.getAppYamlPath());
    dockerfileTextField.setText(configuration.getDockerFilePath());
    gcpProjectSelector.setText(configuration.getCloudProjectName());
    appYamlTextField.setVisible(configuration.isOverrideAppYaml()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);
    archiveSelector.setText(configuration.getUserSpecifiedArtifactPath());
    appYamlOverrideCheckBox.setSelected(configuration.isOverrideAppYaml()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);
    dockerfileOverrideCheckBox.setSelected(configuration.isOverrideDockerfile()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);
    modulesWithFlexFacetComboBox.setEnabled(!configuration.isOverrideAppYaml());

    toggleDockerfileSection();
    updateServiceName();
    refreshApplicationInfoPanel();
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    configuration.setVersion(version.getText());
    configuration.setPromote(promoteVersionCheckBox.isSelected());
    configuration.setStopPreviousVersion(stopPreviousVersionCheckBox.isSelected());
    configuration.setAppYamlPath(getAppYamlPath());
    configuration.setDockerFilePath(getDockerfilePath());
    configuration.setCloudProjectName(gcpProjectSelector.getText());
    CredentialedUser user = gcpProjectSelector.getSelectedUser();
    if (user != null) {
      configuration.setGoogleUsername(user.getEmail());
    }
    String environment = "";
    if (deploymentSource instanceof AppEngineDeployable) {
      environment = ((AppEngineDeployable) deploymentSource).getEnvironment().name();
    }
    configuration.setEnvironment(environment);
    configuration.setUserSpecifiedArtifact(
        deploymentSource instanceof UserSpecifiedPathDeploymentSource);
    configuration.setUserSpecifiedArtifactPath(archiveSelector.getText());
    configuration.setOverrideAppYaml(appYamlOverrideCheckBox.isSelected());
    configuration.setOverrideDockerfile(dockerfileOverrideCheckBox.isSelected());
    updateSelectors();
    setDeploymentProjectAndVersion();
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
    if (StringUtils.isBlank(getAppYamlPath())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.browse.app.yaml"));
    }
    if (!isValidConfigurationFile(getAppYamlPath())) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.deployment.error.staging.yaml") + " "
              + GctBundle.getString("appengine.deployment.error.staging.gotosettings"));
    }
    if (isCustomRuntime()) {
      if (StringUtils.isBlank(getDockerfilePath())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.browse.dockerfile"));
      }
      if (!isValidConfigurationFile(getDockerfilePath())) {
        throw new ConfigurationException(
            GctBundle.getString("appengine.deployment.error.staging.dockerfile") + " "
                + GctBundle.getString("appengine.deployment.error.staging.gotosettings"));
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
    Optional<String> service =
        APP_ENGINE_PROJECT_SERVICE.getServiceNameFromAppYaml(getAppYamlPath());
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

  private boolean isCustomRuntime() {
    return APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(getAppYamlPath())
        .filter(runtime -> runtime == FlexibleRuntime.custom)
        .isPresent();
  }

  /**
   * Enables the Dockerfile section of the UI if the Yaml file contains "runtime: custom". Disables
   * it otherwise.
   */
  private void toggleDockerfileSection() {
    boolean visible = isCustomRuntime();
    dockerfileOverrideCheckBox.setVisible(
        visible && modulesWithFlexFacetComboBox.getItemCount() != 0);
    dockerfileTextField.setVisible(visible);
    dockerfileTextField.setEnabled(dockerfileOverrideCheckBox.isSelected());
    dockerfileLabel.setVisible(visible);
    if (visible) {
      dockerfileTextField.setText(getDockerfilePath());
    }
  }

  /**
   * Checks if a configuration file is valid by checking if it exists and is a regular file.
   */
  private boolean isValidConfigurationFile(String path) {
    try {
      if (!Files.exists(Paths.get(path)) || !Files.isRegularFile(Paths.get(path))) {
        return false;
      }
    } catch (InvalidPathException ipe) {
      return false;
    }

    return true;
  }

  /**
   * Returns the final Yaml file path from the combobox or text field, depending on if it's
   * overridden.
   */
  private String getAppYamlPath() {
    if (appYamlOverrideCheckBox.isSelected()) {
      return appYamlTextField.getText();
    }

    if (modulesWithFlexFacetComboBox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
        .getFacetByType(AppEngineFlexibleFacetType.ID))
        .map(flexFacet -> flexFacet.getConfiguration().getAppYamlPath())
        .orElse("");
  }

  private String getDockerfilePath() {
    if (dockerfileOverrideCheckBox.isSelected()) {
      return dockerfileTextField.getText();
    }

    if (modulesWithFlexFacetComboBox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
            .getFacetByType(AppEngineFlexibleFacetType.ID))
        .map(flexFacet -> flexFacet.getConfiguration().getDockerfilePath())
        .orElse("");
  }

  /**
   * Sets the project / version to allow the deployment line items to be decorated with additional
   * identifying data. See {@link AppEngineRuntimeInstance#getDeploymentName}.
   */
  private void setDeploymentProjectAndVersion() {
    if (deploymentSource instanceof AppEngineDeployable) {
      ((AppEngineDeployable) deploymentSource).setProjectName(gcpProjectSelector.getText());
      ((AppEngineDeployable) deploymentSource).setVersion(
          Strings.isNullOrEmpty(version.getText()) ? "auto" : version.getText());
    }
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
  TextFieldWithBrowseButton getAppYamlTextField() {
    return appYamlTextField;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerfileTextField() {
    return dockerfileTextField;
  }

  @VisibleForTesting
  JLabel getDockerfileLabel() {
    return dockerfileLabel;
  }

  @VisibleForTesting
  JCheckBox getDockerfileOverrideCheckBox() {
    return dockerfileOverrideCheckBox;
  }

  @VisibleForTesting
  JCheckBox getAppYamlOverrideCheckBox() {
    return appYamlOverrideCheckBox;
  }

  @VisibleForTesting
  JLabel getServiceLabel() {
    return serviceLabel;
  }

  @VisibleForTesting
  JComboBox getModulesWithFlexFacetComboBox() {
    return modulesWithFlexFacetComboBox;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getArchiveSelector() {
    return archiveSelector;
  }

  @VisibleForTesting
  ProjectSelector getGcpProjectSelector() {
    return gcpProjectSelector;
  }

  @VisibleForTesting
  void setAppInfoPanel(AppEngineApplicationInfoPanel appInfoPanel) {
    this.appInfoPanel = appInfoPanel;
  }

  @VisibleForTesting
  void setDeploymentSource(DeploymentSource deploymentSource) {
    this.deploymentSource = deploymentSource;
  }
}
