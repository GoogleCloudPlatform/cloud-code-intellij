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
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorTab;
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;
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
  private TextFieldWithBrowseButton yaml;
  private TextFieldWithBrowseButton dockerfile;
  private CloudSdkPanel cloudSdkPanel;
  private JButton genYamlButton;
  private JButton genDockerfileButton;
  private javax.swing.JTextPane filesWarningLabel;
  private JLabel dockerfileLabel;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private AppEngineHelper appEngineHelper;

  public FlexibleFacetEditor(@Nullable AppEngineDeploymentConfiguration deploymentConfiguration,
      @Nullable Project project) {
    this.appEngineHelper = new CloudSdkAppEngineHelper(project);
    this.deploymentConfiguration = deploymentConfiguration;
    cloudSdkPanel.reset();

    yaml.setText(deploymentConfiguration.getAppYamlPath());
    dockerfile.setText(deploymentConfiguration.getDockerFilePath());

    yaml.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml")
        )
    );

    yaml.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
            yaml.getText()).equals(FlexibleRuntime.CUSTOM));
        validateConfiguration();
      }
    });

    dockerfile.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
            yaml.getText()).equals(FlexibleRuntime.CUSTOM));
        validateConfiguration();
      }
    });

    dockerfile.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.dockerfile"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor()
    );

    genYamlButton.addActionListener(new GenerateConfigActionListener(project, "app.yaml",
        appEngineHelper::defaultAppYaml, yaml, this::validateConfiguration));

    genDockerfileButton.addActionListener(new GenerateConfigActionListener(project, "Dockerfile",
        () -> appEngineHelper.defaultDockerfile(AppEngineFlexibleDeploymentArtifactType.WAR),
        dockerfile, this::validateConfiguration
    ));

    filesWarningLabel.setForeground(Color.RED);
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return !yaml.getText().equals(deploymentConfiguration.getAppYamlPath())
        || !dockerfile.getText().equals(deploymentConfiguration.getDockerFilePath())
        || !CloudSdkService.getInstance().getSdkHomePath().toString().equals(
            cloudSdkPanel.getCloudSdkDirectoryText()
    );
  }

  @Override
  public void reset() {
    yaml.setText(deploymentConfiguration.getAppYamlPath());
    dockerfile.setText(deploymentConfiguration.getDockerFilePath());
    cloudSdkPanel.reset();

    setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
        deploymentConfiguration.getAppYamlPath()).equals(FlexibleRuntime.CUSTOM));
    validateConfiguration();
  }

  @Override
  public void apply() throws ConfigurationException {
    validateConfiguration();

    deploymentConfiguration.setAppYamlPath(yaml.getText());
    deploymentConfiguration.setDockerFilePath(dockerfile.getText());
    cloudSdkPanel.apply();

    setDockerfileVisibility(APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(
        yaml.getText()).equals(FlexibleRuntime.CUSTOM));
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

  /**
   * Validates the configuration and turns on/off any necessary warnings.
   */
  private void validateConfiguration() {
    try {
      if (!Files.exists(Paths.get(yaml.getText()))
          || !Files.isRegularFile(Paths.get(yaml.getText()))) {
        yaml.getTextField().setForeground(Color.RED);
        filesWarningLabel.setVisible(true);
      } else {
        yaml.getTextField().setForeground(Color.BLACK);
      }
    } catch (InvalidPathException ipe) {
      yaml.getTextField().setForeground(Color.RED);
      filesWarningLabel.setVisible(true);
    }

    try {
      if (APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(yaml.getText())
          .equals(FlexibleRuntime.CUSTOM)
          && (!Files.exists(Paths.get(dockerfile.getText()))
          || !Files.isRegularFile(Paths.get(dockerfile.getText())))) {
        dockerfile.getTextField().setForeground(Color.RED);
        filesWarningLabel.setVisible(true);
      } else {
        dockerfile.getTextField().setForeground(Color.BLACK);
      }
    } catch (InvalidPathException ipe) {
      dockerfile.getTextField().setForeground(Color.RED);
      filesWarningLabel.setVisible(true);
    }

    if (!yaml.getTextField().getForeground().equals(Color.RED)
        && APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(yaml.getText())
        .equals(FlexibleRuntime.CUSTOM)
        && !dockerfile.getTextField().getForeground().equals(Color.RED)) {
      filesWarningLabel.setVisible(false);
    }
  }

  private void setDockerfileVisibility(boolean visible) {
    dockerfileLabel.setVisible(visible);
    dockerfile.setVisible(visible);
    genDockerfileButton.setVisible(visible);
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    if (facet instanceof AppEngineFlexibleFacet) {
      ((AppEngineFlexibleFacet) facet).getConfiguration().setAppYamlPath(yaml.getText());
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
    private final Runnable callback;

    GenerateConfigActionListener(
        Project project,
        String fileName,
        Supplier<Optional<Path>> sourceFileProvider,
        TextFieldWithBrowseButton filePicker,
        Runnable callback) {
      this.project = project;
      this.fileName = fileName;
      this.sourceFileProvider = sourceFileProvider;
      this.filePicker = filePicker;
      this.callback = callback;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Path sourceFile = sourceFileProvider.get().orElseThrow(
          () -> new AssertionError("appengine.deployment.error.deployable.notjarorwar"));

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
        callback.run();
      }
    }
  }
}
