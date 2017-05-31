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

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.SelectConfigDestinationFolderDialog;
import com.google.cloud.tools.intellij.appengine.facet.flexible.FlexibleFacetEditor.ValidationResult.Status;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.icons.AllIcons.Ide;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;

/**
 * Panel used to configure the Flexible settings.
 */
public class FlexibleFacetEditor extends FacetEditorTab {

  private static final AppEngineProjectService APP_ENGINE_PROJECT_SERVICE =
      AppEngineProjectService.getInstance();
  private static final boolean IS_WAR_DOCKERFILE_DEFAULT = true;
  private static final String DOCKERFILE_NAME = "Dockerfile";

  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYaml;
  private TextFieldWithBrowseButton dockerDirectory;
  private JButton genAppYamlButton;
  private JButton genDockerfileButton;
  private JLabel errorIcon;
  private JLabel errorMessage;
  private JRadioButton jarRadioButton;
  private JRadioButton warRadioButton;
  private JPanel dockerfilePanel;
  private AppEngineFlexibleFacetConfiguration facetConfiguration;

  FlexibleFacetEditor(@NotNull AppEngineFlexibleFacetConfiguration facetConfiguration,
      @NotNull Module module) {
    this.facetConfiguration = facetConfiguration;

    appYaml.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml")
        )
    );

    appYaml.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        toggleDockerfileSection();
        validateAndShowWarnings();
      }
    });

    dockerDirectory.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        validateAndShowWarnings();
      }
    });

    dockerDirectory.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.docker.directory"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

    genAppYamlButton.addActionListener(
        new GenerateConfigActionListener(
            module.getProject(),
            "app.yaml",
            (outputFolderPath) -> APP_ENGINE_PROJECT_SERVICE.generateAppYaml(
                FlexibleRuntime.JAVA,
                module,
                outputFolderPath),
            appYaml,
            false,
            this::validateAndShowWarnings
        ));

    genDockerfileButton.addActionListener(
        new GenerateConfigActionListener(
            module.getProject(),
            "Dockerfile",
            (outputFolderPath) -> APP_ENGINE_PROJECT_SERVICE.generateDockerfile(
                warRadioButton.isSelected()
                    ? AppEngineFlexibleDeploymentArtifactType.WAR
                    : AppEngineFlexibleDeploymentArtifactType.JAR,
                module,
                outputFolderPath),
            dockerDirectory,
            true,
            this::validateAndShowWarnings
        ));

    appYaml.setText(facetConfiguration.getAppYamlPath());
    dockerDirectory.setText(facetConfiguration.getDockerDirectory());

    ButtonGroup dockerfileTypeGroup = new ButtonGroup();
    dockerfileTypeGroup.add(jarRadioButton);
    dockerfileTypeGroup.add(warRadioButton);
    warRadioButton.setSelected(IS_WAR_DOCKERFILE_DEFAULT);
    jarRadioButton.setSelected(!IS_WAR_DOCKERFILE_DEFAULT);

    errorIcon.setIcon(Ide.Error);
    errorIcon.setVisible(false);
    errorMessage.setVisible(false);

    validateAndShowWarnings();
    toggleDockerfileSection();
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return !appYaml.getText().equals(facetConfiguration.getAppYamlPath())
        || !dockerDirectory.getText().equals(facetConfiguration.getDockerDirectory());
  }

  @Override
  public void reset() {
    appYaml.setText(facetConfiguration.getAppYamlPath());
    dockerDirectory.setText(facetConfiguration.getDockerDirectory());

    toggleDockerfileSection();
  }

  @Override
  public void apply() throws ConfigurationException {
    ValidationResult result = validateAndShowWarnings();
    if (result.status == Status.ERROR) {
      throw new ConfigurationException(result.message);
    }
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

  private boolean isRuntimeCustom() throws MalformedYamlFileException {
    return APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(appYaml.getText())
        .filter(runtime -> runtime == FlexibleRuntime.CUSTOM)
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
   * @return the validation result
   */
  private ValidationResult validateAndShowWarnings() {
    ValidationResult result = validateConfiguration();
    if (result.status == Status.OK) {
      errorIcon.setVisible(false);
      errorMessage.setVisible(false);
    } else if (result.status == Status.ERROR) {
      errorMessage.setText(result.message);
      errorIcon.setVisible(true);
      errorMessage.setVisible(true);
    }
    return result;
  }

  private ValidationResult validateConfiguration() {
    if (!isValidConfigurationFile(appYaml.getText())) {
      return new ValidationResult(Status.ERROR,
          GctBundle.getString("appengine.deployment.error.staging.yaml"));
    } else {
      try {
        if (isRuntimeCustom()) {
          String dockerDirectoryText = dockerDirectory.getText();
          if (dockerDirectoryText.isEmpty() || !Files.isDirectory(Paths.get(dockerDirectoryText))) {
            return new ValidationResult(Status.ERROR,
                GctBundle.getString("appengine.deployment.error.staging.docker.directory"));
          } else if (!isValidConfigurationFile(
              Paths.get(dockerDirectoryText, DOCKERFILE_NAME).toString())) {
            return new ValidationResult(Status.ERROR,
                GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
          }
        }
      } catch (MalformedYamlFileException myf) {
        return new ValidationResult(Status.ERROR, GctBundle.getString("appengine.appyaml.malformed"));
      }
    }
    return new ValidationResult(Status.OK, "");
  }

  private void toggleDockerfileSection() {
    boolean visible = false;
    try {
      visible = isRuntimeCustom();
    } catch (MalformedYamlFileException myf) {
      // do nothing
    }
    dockerfilePanel.setVisible(visible);
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    if (facet instanceof AppEngineFlexibleFacet) {
      ((AppEngineFlexibleFacet) facet).getConfiguration().setAppYamlPath(appYaml.getText());
      ((AppEngineFlexibleFacet) facet).getConfiguration().setDockerDirectory(
          dockerDirectory.getText());
    }
  }

  /**
   * A somewhat generic way of generating a file for a {@link TextFieldWithBrowseButton}.
   */
  private static class GenerateConfigActionListener implements ActionListener {

    private final Project project;
    private final String fileName;
    private final TextFieldWithBrowseButton directoryPicker;
    private final Consumer<Path> configFileGenerator;
    private final boolean isDirectory;
    // Used to refresh the warnings.
    private final Runnable configurationValidator;

    /**
     * Generates a configuration file and updates the directory picker text after generation.
     *
     * @param configFileGenerator the callback that generates the file
     * @param directoryPicker the text field in the Flex facet editor that provides the initial
     *   value of the Choose Generated Configuration Destination Folder dialog
     * @param isDirectory true when the <@code>directoryPicker</@code> browses to a directory and
     *   false when <@code>directoryPicker</@code> browses to a file
     * @param configurationValidator the validation method for the updated configuration in Flex
     *   facet settings
     */
    GenerateConfigActionListener(
        Project project,
        String fileName,
        Consumer<Path> configFileGenerator,
        TextFieldWithBrowseButton directoryPicker,
        boolean isDirectory,
        Runnable configurationValidator) {
      this.project = project;
      this.fileName = fileName;
      this.configFileGenerator = configFileGenerator;
      this.directoryPicker = directoryPicker;
      this.isDirectory = isDirectory;
      this.configurationValidator = configurationValidator;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      String directoryPath = directoryPicker.getText();
      if (!isDirectory) {
        try {
          Path directoryPickerPathParent = Paths.get(directoryPicker.getText()).getParent();
          if (directoryPickerPathParent != null) {
            directoryPath = directoryPickerPathParent.toString();
          } else {
            directoryPath = "";
          }
        } catch (InvalidPathException ipe) {
          directoryPath = "";
        }
      }

      SelectConfigDestinationFolderDialog destinationFolderDialog = new
          SelectConfigDestinationFolderDialog(project, directoryPath);

      if (destinationFolderDialog.showAndGet()) {
        Path destinationFolderPath = destinationFolderDialog.getDestinationFolder();
        Path destinationFilePath = destinationFolderPath.resolve(fileName);

        if (Files.exists(destinationFilePath)) {
          Messages.showErrorDialog(project,
              GctBundle.message("appengine.flex.config.generation.file.exists.error",
                  destinationFilePath.getFileName().toString()), "Error");
          return;
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

        configFileGenerator.accept(destinationFolderPath);
        directoryPicker.setText(isDirectory ?
            destinationFolderPath.toString() :
            destinationFilePath.toString());
        configurationValidator.run();
      }
    }
  }

  /**
   * An object representing the outcome of a configuration validation check.
   */
  static class ValidationResult {
    enum Status {OK, ERROR}

    final Status status;
    final String message;

    ValidationResult(Status status, String message) {
      Preconditions.checkNotNull(status);
      Preconditions.checkNotNull(message);

      this.status = status;
      this.message = message;
    }
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getAppYaml() {
    return appYaml;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerDirectory() {
    return dockerDirectory;
  }

  @VisibleForTesting
  JPanel getDockerfilePanel() {
    return dockerfilePanel;
  }

  @VisibleForTesting
  public JLabel getErrorIcon() {
    return errorIcon;
  }

  @VisibleForTesting
  public JLabel getErrorMessage() {
    return errorMessage;
  }

  @VisibleForTesting
  public JRadioButton getJarRadioButton() {
    return jarRadioButton;
  }

  @VisibleForTesting
  public JRadioButton getWarRadioButton() {
    return warRadioButton;
  }
}
