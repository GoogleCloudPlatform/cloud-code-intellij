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
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.SelectConfigDestinationFolderDialog;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
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
  private TextFieldWithBrowseButton appYamlField;
  private JPanel dockerfilePanel;
  private HyperlinkLabel appYamlGenerateLink;
  private JPanel appYamlErrorPanel;
  private HyperlinkLabel appYamlErrorMessage;
  private JPanel runtimePanel;
  private JLabel runtimeLabel;
  private TextFieldWithBrowseButton dockerDirectoryField;
  private JPanel dockerfileErrorPanel;
  private HyperlinkLabel dockerfileErrorMessage;
  private HyperlinkLabel dockerfileGenerateLink;
  private JLabel runtimeExplanationLabel;
  private AppEngineFlexibleFacetConfiguration facetConfiguration;

  FlexibleFacetEditor(@NotNull AppEngineFlexibleFacetConfiguration facetConfiguration,
      @NotNull Module module) {
    this.facetConfiguration = facetConfiguration;

    // todo use htmltext instead?
    // todo move all these out of here
    appYamlGenerateLink.setHyperlinkText("or ",
        GctBundle.message("appengine.flex.facet.config.appyaml.generate.link.text"), "");
    appYamlGenerateLink.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appYamlGenerateLink.setHyperlinkTarget("http://www.google.com");
    appYamlErrorMessage.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appYamlErrorMessage.setHyperlinkTarget("http://www.google.com");

    dockerfileGenerateLink.setHyperlinkText("or ",
        GctBundle.message("appengine.flex.facet.config.dockerfile.generate.link.text"), "");
    dockerfileGenerateLink.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    dockerfileGenerateLink.setHyperlinkTarget("http://www.google.com");
    dockerfileErrorMessage.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    dockerfileErrorMessage.setHyperlinkText("http://www.google.com");

    runtimeExplanationLabel.setFont(
        new Font(
            runtimeExplanationLabel.getFont().getName(),
            Font.ITALIC,
            runtimeExplanationLabel.getFont().getSize() - 1));

    appYamlField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml")
        )
    );

    appYamlField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        validateConfiguration();
      }
    });

    dockerDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        validateDockerfile();
      }
    });

    dockerDirectoryField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.docker.directory"),
        null /* description */,
        module.getProject(),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

//    genAppYamlButton.addActionListener(
//        new GenerateConfigActionListener(
//            module.getProject(),
//            "app.yaml",
//            (outputFolderPath) -> APP_ENGINE_PROJECT_SERVICE.generateAppYaml(
//                FlexibleRuntime.JAVA,
//                module,
//                outputFolderPath),
//            appYaml,
//            false,
//            this::validateAndShowWarnings
//        ));

//    genDockerfileButton.addActionListener(
//        new GenerateConfigActionListener(
//            module.getProject(),
//            "Dockerfile",
//            (outputFolderPath) -> APP_ENGINE_PROJECT_SERVICE.generateDockerfile(
//                warRadioButton.isSelected()
//                    ? AppEngineFlexibleDeploymentArtifactType.WAR
//                    : AppEngineFlexibleDeploymentArtifactType.JAR,
//                module,
//                outputFolderPath),
//            dockerDirectory,
//            true,
//            this::validateDockerfile
//        ));

    // todo move
//    dockerDirectory.setText(facetConfiguration.getDockerDirectory());

    // todo move
//    ButtonGroup dockerfileTypeGroup = new ButtonGroup();
//    dockerfileTypeGroup.add(jarRadioButton);
//    dockerfileTypeGroup.add(warRadioButton);
//    warRadioButton.setSelected(IS_WAR_DOCKERFILE_DEFAULT);
//    jarRadioButton.setSelected(!IS_WAR_DOCKERFILE_DEFAULT);

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
    // todo move this to appropriate places
    // todo get default font for resuse rather then font of each component
    //    appYaml.getTextField().setFont(new Font(appYaml.getFont().getName(), Font.ITALIC, appYaml.getFont().getSize()));
    //    appYamlErrorMessage.setFont(new Font(appYamlErrorMessage.getFont().getName(), appYamlErrorMessage.getFont().getStyle(), appYamlErrorMessage.getFont().getSize()));

    appYamlField.setText(facetConfiguration.getAppYamlPath());
    dockerDirectoryField.setText(facetConfiguration.getDockerDirectory());
  }

  @Override
  public void apply() throws ConfigurationException {
//    ValidationResult result =
    // TODO: not blocking on errors, so can prob get rid of Status.Error
//    validateAndShowWarnings();
//    if (result.status == Status.ERROR) {
//      throw new ConfigurationException(result.message);
//    }
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
    return getRuntime().filter(runtime -> runtime == FlexibleRuntime.CUSTOM).isPresent();
  }

  private String getRuntimeText() throws MalformedYamlFileException {
    return getRuntime()
        .map(FlexibleRuntime::toString)
        .orElse("");
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

  /**
   * Validates the configuration and toggles on/off any necessary warnings and components.
   */
  private void validateConfiguration() {
    boolean isValidAppYaml = validateAppYaml();

    if (isValidAppYaml) {
      try {
        if (isRuntimeCustom()) {
          validateDockerfile();
        }
      } catch (MalformedYamlFileException myf) {
        // TODO can this be refactored?
        // We already know that runtime is parsable from {@link #validateAppYaml}
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
        toggleInvalidAppYamlState(GctBundle.getString("appengine.appyaml.malformed"));
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
    // todo no need to set the text/hyperlinkstuff here: do it once on init
    appYamlErrorMessage.setHyperlinkText(
        message, GctBundle.message("appengine.flex.facet.config.appyaml.generate.link.text"), "");
    appYamlGenerateLink.setVisible(false);
    runtimePanel.setVisible(false);
    dockerfilePanel.setVisible(false);
    runtimeExplanationLabel.setVisible(false);
  }

  private void toggleValidAppYamlState(String runtimeText) {
    appYamlErrorPanel.setVisible(false);
    appYamlGenerateLink.setVisible(true);
    runtimePanel.setVisible(true);
    runtimeLabel.setText(runtimeText);
    if (runtimeText.equalsIgnoreCase(FlexibleRuntime.CUSTOM.toString())) {
      dockerfilePanel.setVisible(true);
      runtimeExplanationLabel.setVisible(false);
    } else {
      dockerfilePanel.setVisible(false);
      runtimeExplanationLabel.setVisible(true);
    }
  }

  private void toggleInvalidDockerfileState(String message) {
    dockerfileErrorPanel.setVisible(true);
    // todo no need to set this here; can do it on init
    dockerfileErrorMessage.setHyperlinkText(message,
        GctBundle.message("appengine.flex.facet.config.dockerfile.generate.link.text"),"");
    dockerfileGenerateLink.setVisible(false);
  }

  private void toggleValidDockerfileState() {
    dockerfileErrorPanel.setVisible(false);
    dockerfileGenerateLink.setVisible(true);
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    if (facet instanceof AppEngineFlexibleFacet) {
      ((AppEngineFlexibleFacet) facet).getConfiguration().setAppYamlPath(appYamlField.getText());
      ((AppEngineFlexibleFacet) facet).getConfiguration().setDockerDirectory(
          dockerDirectoryField.getText());
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

  // todo expose panel for testing

  @VisibleForTesting
  public HyperlinkLabel getAppYamlErrorMessage() {
    return appYamlErrorMessage;
  }
}
