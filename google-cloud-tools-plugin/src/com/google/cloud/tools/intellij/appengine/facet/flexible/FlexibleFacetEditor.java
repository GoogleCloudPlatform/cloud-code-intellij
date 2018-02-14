/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.DockerfileArtifactTypePanel;
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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** App Engine flexible facet editor configuration panel. */
public class FlexibleFacetEditor extends FacetEditorTab {

  private static final AppEngineProjectService APP_ENGINE_PROJECT_SERVICE =
      AppEngineProjectService.getInstance();
  private static final String DOCKERFILE_NAME = "Dockerfile";
  public static final String APP_YAML_FILE_NAME = "app.yaml";

  private Module module;
  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYamlField;
  private JPanel dockerfilePanel;
  private JPanel appYamlErrorPanel;
  private HyperlinkLabel appYamlErrorMessage;
  private AppEngineFlexibleRuntimePanel runtimePanel;
  private TextFieldWithBrowseButton dockerDirectoryField;
  private JPanel dockerfileErrorPanel;
  private HyperlinkLabel dockerfileErrorMessage;
  private AppEngineFlexibleFacetConfiguration facetConfiguration;

  FlexibleFacetEditor(
      @NotNull AppEngineFlexibleFacetConfiguration facetConfiguration, @NotNull Module module) {
    this.module = module;
    this.facetConfiguration = facetConfiguration;

    appYamlErrorMessage.addHyperlinkListener(new AppYamlGenerateActionListener());
    dockerfileErrorMessage.addHyperlinkListener(new DockerfileGenerateActionListener());

    appYamlField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withFileFilter(
                virtualFile ->
                    Comparing.equal(virtualFile.getExtension(), "yaml")
                        || Comparing.equal(virtualFile.getExtension(), "yml")));

    appYamlField
        .getTextField()
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                validateConfiguration();
              }
            });

    dockerDirectoryField
        .getTextField()
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                validateDockerfile();
              }
            });

    dockerDirectoryField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.docker.directory"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor());

    validateConfiguration();
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return !appYamlField.getText().equals(facetConfiguration.getAppYamlPath())
        || !dockerDirectoryField.getText().equals(facetConfiguration.getDockerDirectory());
  }

  @Override
  public void reset() {
    appYamlField.setText(facetConfiguration.getAppYamlPath());
    dockerDirectoryField.setText(facetConfiguration.getDockerDirectory());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GctBundle.getString("appengine.flexible.facet.name.title");
  }

  private boolean isRuntimeCustom() throws MalformedYamlFileException {
    return getRuntime().filter(runtime -> runtime == FlexibleRuntime.CUSTOM).isPresent();
  }

  private String getRuntimeText() throws MalformedYamlFileException {
    return getRuntime().map(FlexibleRuntime::toString).orElse("");
  }

  private Optional<FlexibleRuntime> getRuntime() throws MalformedYamlFileException {
    return APP_ENGINE_PROJECT_SERVICE.getFlexibleRuntimeFromAppYaml(appYamlField.getText());
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

  /** Validates the configuration and toggles on/off any necessary warnings and components. */
  private void validateConfiguration() {
    boolean isValidAppYaml = validateAppYaml();

    if (isValidAppYaml) {
      try {
        if (isRuntimeCustom()) {
          validateDockerfile();
        }
      } catch (MalformedYamlFileException myf) {
        // We already know that runtime is parsable from {@link validateAppYaml}
      }
    }
  }

  private boolean validateAppYaml() {
    if (!isValidConfigurationFile(appYamlField.getText())) {
      toggleInvalidAppYamlState(
          GctBundle.getString("appengine.flex.facet.config.appyaml.error.before.text"));
      return false;
    } else {
      try {
        toggleValidAppYamlState(getRuntimeText());
      } catch (MalformedYamlFileException myf) {
        toggleInvalidAppYamlState(
            GctBundle.getString("appengine.flex.facet.config.appyaml.malformed.error"));
        return false;
      }
    }

    return true;
  }

  private void validateDockerfile() {
    String dockerDirectoryText = dockerDirectoryField.getText();
    if (dockerDirectoryText.isEmpty()
        || !Files.isDirectory(Paths.get(dockerDirectoryText))
        || !isValidConfigurationFile(Paths.get(dockerDirectoryText, DOCKERFILE_NAME).toString())) {
      toggleInvalidDockerfileState(
          GctBundle.getString(
              "appengine.flex.facet.config.dockerfile.directory.error.before.text"));
    } else {
      toggleValidDockerfileState();
    }
  }

  private void toggleInvalidAppYamlState(String message) {
    appYamlErrorPanel.setVisible(true);
    appYamlErrorMessage.setHyperlinkText(
        message, GctBundle.message("appengine.flex.facet.config.appyaml.generate.link.text"), "");
    runtimePanel.setVisible(false);
    dockerfilePanel.setVisible(false);
  }

  private void toggleValidAppYamlState(String runtimeText) {
    appYamlErrorPanel.setVisible(false);
    runtimePanel.setVisible(true);
    runtimePanel.setLabelText(runtimeText);
    if (runtimeText.equalsIgnoreCase(FlexibleRuntime.CUSTOM.toString())) {
      dockerfilePanel.setVisible(true);
    } else {
      dockerfilePanel.setVisible(false);
    }
  }

  private void toggleInvalidDockerfileState(String message) {
    dockerfileErrorPanel.setVisible(true);
    dockerfileErrorMessage.setHyperlinkText(
        message,
        GctBundle.message("appengine.flex.facet.config.dockerfile.generate.link.text"),
        "");
  }

  private void toggleValidDockerfileState() {
    dockerfileErrorPanel.setVisible(false);
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    if (facet instanceof AppEngineFlexibleFacet) {
      ((AppEngineFlexibleFacet) facet).getConfiguration().setAppYamlPath(appYamlField.getText());
      ((AppEngineFlexibleFacet) facet)
          .getConfiguration()
          .setDockerDirectory(dockerDirectoryField.getText());
    }

    // Called on explicitly adding the facet through Project Settings -> Facets, but not on the
    // Framework discovered "Configure" popup.
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_FACET_ADD)
        .addMetadata("source", "setOnModule")
        .addMetadata("env", "flex")
        .ping();
  }

  private class AppYamlGenerateActionListener implements HyperlinkListener {
    private static final String DEFAULT_APP_YAML_DIRECTORY_NAME = "appengine";

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.APP_ENGINE_GENERATE_FILE_APPYAML)
          .addMetadata("source", "setOnModule")
          .addMetadata("env", "flex")
          .ping();

      String appYamlFilePath = appYamlField.getText();
      String appYamlDirectoryPath =
          StringUtils.isEmpty(appYamlFilePath)
              ? getDefaultConfigPath(DEFAULT_APP_YAML_DIRECTORY_NAME)
              : getParentDirectory(appYamlFilePath);

      SelectConfigDestinationFolderDialog dialog =
          new SelectConfigDestinationFolderDialog(
              module.getProject(),
              appYamlDirectoryPath,
              GctBundle.message("appengine.flex.config.appyaml.destination.chooser.title"));

      if (dialog.showAndGet()) {
        Path destinationFolderPath = dialog.getDestinationFolder();
        Path destinationFilePath = destinationFolderPath.resolve(APP_YAML_FILE_NAME);

        if (validateAndWarnFileGeneration(destinationFolderPath, destinationFilePath)) {
          APP_ENGINE_PROJECT_SERVICE.generateAppYaml(
              FlexibleRuntime.JAVA, module, destinationFolderPath);
          appYamlField.setText(destinationFilePath.toString());
          validateConfiguration();
        }
      }
    }

    /**
     * Returns the parent directory path string of the file, or empty string if it can't be
     * resolved.
     */
    private String getParentDirectory(String filePath) {
      try {
        Path parentDirecotry = Paths.get(filePath).getParent();
        return parentDirecotry != null ? parentDirecotry.toString() : "";
      } catch (InvalidPathException ipe) {
        return "";
      }
    }
  }

  private class DockerfileGenerateActionListener implements HyperlinkListener {
    private static final String DEFAULT_DOCKERFILE_DIRECTORY_NAME = "docker";

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.APP_ENGINE_GENERATE_FILE_DOCKERFILE)
          .addMetadata("source", "setOnModule")
          .addMetadata("env", "flex")
          .ping();
      String dockerfileDirectoryPath =
          StringUtils.isEmpty(dockerDirectoryField.getText())
              ? getDefaultConfigPath(DEFAULT_DOCKERFILE_DIRECTORY_NAME)
              : dockerDirectoryField.getText();

      SelectDockerfileDestinationFolderDialog dialog =
          new SelectDockerfileDestinationFolderDialog(
              module.getProject(),
              dockerfileDirectoryPath,
              GctBundle.message("appengine.flex.config.dockerfile.destination.chooser.title"));

      if (dialog.showAndGet()) {
        Path destinationFolderPath = dialog.getDestinationFolder();
        Path destinationFilePath = destinationFolderPath.resolve(DOCKERFILE_NAME);

        if (validateAndWarnFileGeneration(destinationFolderPath, destinationFilePath)) {
          APP_ENGINE_PROJECT_SERVICE.generateDockerfile(
              dialog.getArtifactType(), module, destinationFolderPath);
          dockerDirectoryField.setText(destinationFolderPath.toString());
          validateConfiguration();
        }
      }
    }
  }

  /** Returns the canonical directory path for config file under src/main/{directoryName}. */
  private String getDefaultConfigPath(String directoryName) {
    VirtualFile virtualFile =
        ModuleRootManager.getInstance(module)
            .getContentRoots()[0]
            .findFileByRelativePath("src/main");

    if (virtualFile == null) {
      return "";
    }

    return Paths.get(virtualFile.getPath(), directoryName).toString();
  }

  /**
   * Performs validation on the config file destination directory and file paths, and warns if
   * generation cannot be performed.
   */
  private boolean validateAndWarnFileGeneration(
      Path destinationFolderPath, Path destinationFilePath) {
    if (Files.exists(destinationFilePath)) {
      Messages.showErrorDialog(
          module.getProject(),
          GctBundle.message(
              "appengine.flex.config.generation.file.exists.error",
              destinationFilePath.getFileName().toString()),
          GctBundle.message("appengine.flex.config.generation.error.title"));
      return false;
    } else if (Files.isRegularFile(destinationFolderPath)) {
      new FileConfirmationDialog(
              module.getProject(), DialogType.NOT_DIRECTORY_ERROR, destinationFolderPath)
          .show();
      return false;
    } else if (!Files.exists(destinationFolderPath)) {
      if (!new FileConfirmationDialog(
              module.getProject(), DialogType.CONFIRM_CREATE_DIR, destinationFolderPath)
          .showAndGet()) {
        return false;
      }
    }
    return true;
  }

  private static class SelectDockerfileDestinationFolderDialog
      extends SelectConfigDestinationFolderDialog {

    private DockerfileArtifactTypePanel panel;

    SelectDockerfileDestinationFolderDialog(
        @Nullable Project project, String directoryPath, String title) {
      super(project, directoryPath, title);
    }

    @Override
    public void createUIComponents() {
      panel = new DockerfileArtifactTypePanel();
      setAdditionalConfigurationPanel(panel.getPanel());
    }

    AppEngineFlexibleDeploymentArtifactType getArtifactType() {
      if (panel == null) {
        return AppEngineFlexibleDeploymentArtifactType.UNKNOWN;
      }

      return panel.getArtifactType();
    }
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getAppYamlField() {
    return appYamlField;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerDirectory() {
    return dockerDirectoryField;
  }

  @VisibleForTesting
  JPanel getDockerfilePanel() {
    return dockerfilePanel;
  }

  @VisibleForTesting
  JPanel getAppYamlErrorPanel() {
    return appYamlErrorPanel;
  }

  @VisibleForTesting
  AppEngineFlexibleRuntimePanel getRuntimePanel() {
    return runtimePanel;
  }

  @VisibleForTesting
  JPanel getDockerfileErrorPanel() {
    return dockerfileErrorPanel;
  }
}
