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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.SelectConfigDestinationFolderDialog;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFile;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.icons.AllIcons.Ide;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.scanner.ScannerException;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * Panel used to configure the Flexible settings.
 */
public class FlexibleFacetEditor extends FacetEditorTab {

  private static final AppEngineProjectService APP_ENGINE_PROJECT_SERVICE =
      AppEngineProjectService.getInstance();

  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYaml;
  private TextFieldWithBrowseButton dockerfile;
  private JButton genAppYamlButton;
  private JButton genDockerfileButton;
  private JLabel noDockerfileLabel;
  private JLabel errorIcon;
  private JLabel errorMessage;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private AppEngineHelper appEngineHelper;

  FlexibleFacetEditor(@Nullable AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull Project project) {
    this.appEngineHelper = new CloudSdkAppEngineHelper(project);
    this.deploymentConfiguration = deploymentConfiguration;

    appYaml.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml")
        )
    );

    appYaml.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        toggleDockerfileSection();
        validateConfiguration();
      }
    });

    dockerfile.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        validateConfiguration();
      }
    });

    dockerfile.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.dockerfile"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor()
    );

    genAppYamlButton.addActionListener(new GenerateConfigActionListener(project, "app.yaml",
        () -> appEngineHelper.defaultAppYaml(FlexibleRuntime.java), appYaml,
        this::validateConfiguration));

    genDockerfileButton.addActionListener(new GenerateConfigActionListener(project, "Dockerfile",
        () -> appEngineHelper.defaultDockerfile(AppEngineFlexibleDeploymentArtifactType.WAR),
        dockerfile, this::validateConfiguration
    ));

    appYaml.setText(deploymentConfiguration.getAppYamlPath());
    dockerfile.setText(deploymentConfiguration.getDockerFilePath());

    errorIcon.setIcon(Ide.Error);
    errorIcon.setVisible(false);
    errorMessage.setVisible(false);

    validateConfiguration();
    toggleDockerfileSection();
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return !appYaml.getText().equals(deploymentConfiguration.getAppYamlPath())
        || !dockerfile.getText().equals(deploymentConfiguration.getDockerFilePath());
  }

  @Override
  public void reset() {
    appYaml.setText(deploymentConfiguration.getAppYamlPath());
    dockerfile.setText(deploymentConfiguration.getDockerFilePath());

    toggleDockerfileSection();
  }

  @Override
  public void apply() throws ConfigurationException {
    validateConfiguration();
    if (!isValidConfigurationFile(appYaml.getText())) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.deployment.error.staging.yaml"));
    }

    try {
      if (isRuntimeCustom() && !isValidConfigurationFile(dockerfile.getText())) {
        throw new ConfigurationException(
            GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
      }
    } catch (MalformedYamlFile myf) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.appyaml.malformed"));
    }

    deploymentConfiguration.setAppYamlPath(appYaml.getText());
    deploymentConfiguration.setDockerFilePath(dockerfile.getText());
  }

  @Override
  public void disposeUIResources() {
    // Do nothing.
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GctBundle.getString("appengine.flexible.facet.name");
  }

  private boolean isRuntimeCustom() throws MalformedYamlFile {
    return APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(appYaml.getText())
        .filter(runtime -> runtime == FlexibleRuntime.custom)
        .isPresent();
  }

  /**
   * Tests if a file is valid by checking whether it exists, is a regular file and is well formed.
   */
  private boolean isValidConfigurationFile(String path) {
    try {
      if (!Files.isRegularFile(Paths.get(path))) {
        return false;
      }
    } catch (InvalidPathException ipe) {
      return false;
    }

    return true;
  }

  /**
   * Validates the configuration and turns on/off any necessary warnings.
   */
  private void validateConfiguration() {
    boolean showError = false;

    if (!isValidConfigurationFile(appYaml.getText())) {
      errorMessage.setText(GctBundle.getString("appengine.deployment.error.staging.yaml"));
      showError = true;
    }

    try {
      if (isRuntimeCustom() && !isValidConfigurationFile(dockerfile.getText())) {
        errorMessage.setText(GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
        showError = true;
      }
    } catch (MalformedYamlFile myf) {
      errorMessage.setText(GctBundle.getString("appengine.appyaml.malformed"));
      showError = true;
    }

    errorIcon.setVisible(showError);
    errorMessage.setVisible(showError);
  }

  private void toggleDockerfileSection() {
    boolean enabled = false;
    try {
      enabled = isRuntimeCustom();
      // Shows no Dockerfile label if runtime is Java and appYaml is valid.
      noDockerfileLabel.setVisible(!enabled && isValidConfigurationFile(appYaml.getText()));
    } catch (MalformedYamlFile myf) {
      noDockerfileLabel.setVisible(false);
    }
    dockerfile.setEnabled(enabled);
    genDockerfileButton.setEnabled(enabled);
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    if (facet instanceof AppEngineFlexibleFacet) {
      ((AppEngineFlexibleFacet) facet).getConfiguration().setAppYamlPath(appYaml.getText());
      ((AppEngineFlexibleFacet) facet).getConfiguration().setDockerfilePath(dockerfile.getText());
    }
  }

  /**
   * A somewhat generic way of generating a file for a {@link TextFieldWithBrowseButton}.
   */
  private static class GenerateConfigActionListener implements ActionListener {

    private final Project project;
    private final String fileName;
    private final TextFieldWithBrowseButton filePicker;
    private final Supplier<Optional<Path>> sourceFileProvider;
    // Used to refresh the warnings.
    private final Runnable configurationValidator;

    GenerateConfigActionListener(
        Project project,
        String fileName,
        Supplier<Optional<Path>> sourceFileProvider,
        TextFieldWithBrowseButton filePicker,
        Runnable configurationValidator) {
      this.project = project;
      this.fileName = fileName;
      this.sourceFileProvider = sourceFileProvider;
      this.filePicker = filePicker;
      this.configurationValidator = configurationValidator;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Path sourceFile = sourceFileProvider.get().orElseThrow(
          () -> new AssertionError("Invalid location for " + fileName));

      SelectConfigDestinationFolderDialog destinationFolderDialog = new
          SelectConfigDestinationFolderDialog(project, filePicker.getText());
      if (destinationFolderDialog.showAndGet()) {
        Path destinationFolderPath = destinationFolderDialog.getDestinationFolder();
        Path destinationFilePath = destinationFolderPath.resolve(fileName);

        if (Files.exists(destinationFilePath)) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_OVERWRITE, destinationFilePath).showAndGet()) {
            return;
          }
        } else if (Files.isRegularFile(destinationFolderPath)) {
          new FileConfirmationDialog(
              project, DialogType.NOT_DIRECTORY_ERROR, destinationFolderPath).show();
          return;
        } else if (!Files.exists(destinationFolderPath)) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_CREATE_DIR, destinationFolderPath).showAndGet()) {
            return;
          }
        }

        try {
          FileUtil.copy(sourceFile.toFile(), destinationFilePath.toFile());
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destinationFilePath.toFile());
        } catch (IOException ex) {
          String message = GctBundle.message(
              "appengine.flex.config.generation.io.error", destinationFilePath.getFileName());
          Messages.showErrorDialog(project, message + ex.getLocalizedMessage(), "Error");
          return;
        }
        filePicker.setText(destinationFilePath.toString());
        configurationValidator.run();
      }
    }
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getAppYaml() {
    return appYaml;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerfile() {
    return dockerfile;
  }

  @VisibleForTesting
  JButton getGenDockerfileButton() {
    return genDockerfileButton;
  }

  @VisibleForTesting
  JLabel getNoDockerfileLabel() {
    return noDockerfileLabel;
  }

  @VisibleForTesting
  public JLabel getErrorIcon() {
    return errorIcon;
  }

  @VisibleForTesting
  public JLabel getErrorMessage() {
    return errorMessage;
  }
}
