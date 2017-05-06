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

import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.SelectConfigDestinationFolderDialog;
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

  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYaml;
  private TextFieldWithBrowseButton dockerfileDirectory;
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
        showWarnings();
      }
    });

    dockerfileDirectory.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        showWarnings();
      }
    });

    dockerfileDirectory.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.dockerfile"),
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
            this::showWarnings
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
                dockerfileDirectory,
            this::showWarnings
        ));

    appYaml.setText(facetConfiguration.getAppYamlPath());
    dockerfileDirectory.setText(facetConfiguration.getDockerfileDirectory());

    ButtonGroup dockerfileTypeGroup = new ButtonGroup();
    dockerfileTypeGroup.add(jarRadioButton);
    dockerfileTypeGroup.add(warRadioButton);
    warRadioButton.setSelected(IS_WAR_DOCKERFILE_DEFAULT);
    jarRadioButton.setSelected(!IS_WAR_DOCKERFILE_DEFAULT);

    errorIcon.setIcon(Ide.Error);
    errorIcon.setVisible(false);
    errorMessage.setVisible(false);

    showWarnings();
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
        || !dockerfileDirectory.getText().equals(facetConfiguration.getDockerfileDirectory());
  }

  @Override
  public void reset() {
    appYaml.setText(facetConfiguration.getAppYamlPath());
    dockerfileDirectory.setText(facetConfiguration.getDockerfileDirectory());

    toggleDockerfileSection();
  }

  @Override
  public void apply() throws ConfigurationException {
    showWarnings();
    if (!isValidConfigurationFile(appYaml.getText())) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.deployment.error.staging.yaml"));
    }

    try {
      if (isRuntimeCustom() && !isValidConfigurationFile(dockerfileDirectory.getText())) {
        throw new ConfigurationException(
            GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
      }
    } catch (MalformedYamlFileException myf) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.appyaml.malformed"));
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
   */
  private void showWarnings() {
    boolean showError = false;

    if (!isValidConfigurationFile(appYaml.getText())) {
      errorMessage.setText(GctBundle.getString("appengine.deployment.error.staging.yaml"));
      showError = true;
    }

    try {
      if (isRuntimeCustom() && !isValidConfigurationFile(dockerfileDirectory.getText())) {
        errorMessage.setText(GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
        showError = true;
      }
    } catch (MalformedYamlFileException myf) {
      errorMessage.setText(GctBundle.getString("appengine.appyaml.malformed"));
      showError = true;
    }

    errorIcon.setVisible(showError);
    errorMessage.setVisible(showError);
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
      ((AppEngineFlexibleFacet) facet).getConfiguration().setDockerfileDirectory(
          dockerfileDirectory.getText());
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
    // Used to refresh the warnings.
    private final Runnable configurationValidator;

    GenerateConfigActionListener(
        Project project,
        String fileName,
        Consumer<Path> configFileGenerator,
        TextFieldWithBrowseButton directoryPicker,
        Runnable configurationValidator) {
      this.project = project;
      this.fileName = fileName;
      this.configFileGenerator = configFileGenerator;
      this.directoryPicker = directoryPicker;
      this.configurationValidator = configurationValidator;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      SelectConfigDestinationFolderDialog destinationFolderDialog = new
          SelectConfigDestinationFolderDialog(project, directoryPicker.getText());
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
        directoryPicker.setText(destinationFilePath.toString());
        configurationValidator.run();
      }
    }
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getAppYaml() {
    return appYaml;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerfileDirectory() {
    return dockerfileDirectory;
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
