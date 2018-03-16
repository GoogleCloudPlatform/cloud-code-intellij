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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.SVGLoader;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.jetbrains.annotations.Nullable;

/** The form-bound class for the Cloud API details panel. */
public final class GoogleCloudApiDetailsPanel {

  private static final String LINKS_SEPARATOR = " | ";

  private JLabel icon;
  private JLabel nameLabel;
  private JLabel versionLabel;
  private JLabel statusLabel;
  private JPanel panel;
  private JTextPane descriptionTextPane;
  private JTextPane linksTextPane;
  private JPanel apiManagementPanel;
  private JCheckBox enableApiCheckbox;
  private JPanel managementInfoPanel;
  private JTextPane managementWarningTextPane;

  private CloudLibrary currentCloudLibrary;
  private Optional<String> currentBomVersion;
  private CloudApiManagementSpec currentCloudApiManagementSpec;

  /** Returns the {@link JPanel} that holds the UI elements in this panel. */
  JPanel getPanel() {
    return panel;
  }

  /**
   * Sets the currently displayed {@link CloudLibrary} to the given one.
   *
   * <p>If the given {@link CloudLibrary} is already displayed, nothing is done and no UI updates
   * are made.
   *
   * @param library the {@link CloudLibrary} to display
   */
  void setCloudLibrary(
      CloudLibrary library,
      Optional<String> bomVersion,
      CloudApiManagementSpec cloudApiManagementSpec) {
    if (cloudLibrariesEqual(currentCloudLibrary, library)) {
      return;
    }

    currentCloudLibrary = library;
    currentBomVersion = bomVersion;
    currentCloudApiManagementSpec = cloudApiManagementSpec;
    updateUI();
  }

  /**
   * Enables or disables the components of the GCP API management UI based on if the library is
   * selected or not.
   *
   * @param enabled whether to enable or disable the the management UI components
   */
  void setManagementUIEnabled(boolean enabled) {
    managementInfoPanel.setVisible(!enabled);
    enableApiCheckbox.setEnabled(enabled);

    // If the checkbox is disabled it should always be unchecked.
    // Otherwise, it should be checked according to the saved value
    enableApiCheckbox.setSelected(enabled && currentCloudApiManagementSpec.shouldEnable());
  }

  /** Returns the {@link JLabel} that holds the library's icon. */
  @VisibleForTesting
  JLabel getIcon() {
    return icon;
  }

  /** Returns the {@link JLabel} that holds the library's name. */
  @VisibleForTesting
  JLabel getNameLabel() {
    return nameLabel;
  }

  /** Returns the {@link JLabel} that holds the library's version. */
  @VisibleForTesting
  JLabel getVersionLabel() {
    return versionLabel;
  }

  /** Returns the {@link JLabel} that holds the library's status. */
  @VisibleForTesting
  JLabel getStatusLabel() {
    return statusLabel;
  }

  /** Returns the {@link JTextPane} that holds the library's description. */
  @VisibleForTesting
  JTextPane getDescriptionTextPane() {
    return descriptionTextPane;
  }

  /** Returns the {@link JTextPane} that holds the library's links. */
  @VisibleForTesting
  JTextPane getLinksTextPane() {
    return linksTextPane;
  }

  /** Returns the {@link JCheckBox} that selects if the API should be enabled on GCP. */
  @VisibleForTesting
  JCheckBox getEnableApiCheckbox() {
    return enableApiCheckbox;
  }

  /**
   * Returns the {@link JPanel} containing the wording explaining how to enable the management
   * controls.
   */
  @VisibleForTesting
  JPanel getManagementInfoPanel() {
    return managementInfoPanel;
  }

  /**
   * Initializes some UI components in this panel that require special set-up.
   *
   * <p>This is automatically called by the IDEA SDK and should not be directly invoked.
   */
  private void createUIComponents() {
    descriptionTextPane = new JTextPane();
    descriptionTextPane.setOpaque(false);

    linksTextPane = new JTextPane();
    linksTextPane.setOpaque(false);
    linksTextPane.addHyperlinkListener(new BrowserOpeningHyperLinkListener());

    apiManagementPanel = new JPanel();
    apiManagementPanel.setBorder(
        IdeBorderFactory.createTitledBorder(
            GctBundle.message("cloud.apis.management.section.title")));

    managementWarningTextPane = new JTextPane();
    managementWarningTextPane.setOpaque(false);

    enableApiCheckbox = new JCheckBox();
    enableApiCheckbox.addActionListener(
        event ->
            currentCloudApiManagementSpec.setShouldEnable(
                ((JCheckBox) event.getSource()).isSelected()));
  }

  /**
   * Updates the UI elements in this panel to reflect the attributes in the {@link
   * #currentCloudLibrary}.
   */
  private void updateUI() {
    panel.setVisible(true);

    if (currentCloudLibrary.getIcon() == null) {
      icon.setIcon(null);
    } else {
      icon.setIcon(GoogleCloudCoreIcons.LOADING);
      loadImageAsync(currentCloudLibrary.getIcon(), icon::setIcon);
    }

    nameLabel.setText(currentCloudLibrary.getName());
    descriptionTextPane.setText(currentCloudLibrary.getDescription());
    descriptionTextPane.setSize(
        descriptionTextPane.getWidth(), descriptionTextPane.getPreferredSize().height);

    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                if (currentBomVersion.isPresent()) {
                  updateManagedLibraryVersionFromBom(currentBomVersion.get());
                } else {
                  if (client.getMavenCoordinates() != null) {
                    versionLabel.setText(
                        GctBundle.message(
                            "cloud.libraries.version.label",
                            client.getMavenCoordinates().getVersion()));
                  }
                }

                statusLabel.setText(
                    GctBundle.message("cloud.libraries.status.label", client.getLaunchStage()));

                Optional<String> docsLink =
                    makeLink(
                        "cloud.libraries.documentation.link",
                        currentCloudLibrary.getDocumentation());
                Optional<String> sourceLink =
                    makeLink("cloud.libraries.source.link", client.getSource());
                Optional<String> apiReferenceLink =
                    makeLink("cloud.libraries.apireference.link", client.getApiReference());

                List<Optional<String>> links =
                    ImmutableList.of(docsLink, sourceLink, apiReferenceLink);
                linksTextPane.setText(joinLinks(links));
              });
    }

    managementWarningTextPane.setText(GctBundle.message("cloud.apis.management.section.info.text"));
  }

  /**
   * Asynchronously fetches and displays the version of the client library that is managed by the
   * given BOM version.
   *
   * @param bomVersion the version of the BOM from which to load the version of the current client
   *     library
   */
  // TODO (eshaul) this unoptimized implementation fetches all managed BOM versions each time the
  // BOM is updated and library is selected. The bomVersion -> managedLibraryVersions results can be
  // cached on disk to reduce network calls.
  @SuppressWarnings("FutureReturnValueIgnored")
  void updateManagedLibraryVersionFromBom(String bomVersion) {
    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                CloudLibraryClientMavenCoordinates coordinates = client.getMavenCoordinates();

                if (coordinates != null) {
                  versionLabel.setIcon(GoogleCloudCoreIcons.LOADING);
                  versionLabel.setText("");

                  ThreadUtil.getInstance()
                      .executeInBackground(
                          () -> {
                            Optional<String> versionOptional =
                                CloudApiMavenService.getInstance()
                                    .getManagedDependencyVersion(coordinates, bomVersion);

                            versionOptional.ifPresent(
                                version ->
                                    ApplicationManager.getApplication()
                                        .invokeAndWait(
                                            () -> {
                                              versionLabel.setIcon(null);
                                              versionLabel.setText(
                                                  GctBundle.message(
                                                      "cloud.libraries.version.label", version));
                                            },
                                            ModalityState.any()));
                          });
                }
              });
    }
  }

  /**
   * Loads the image at the given URL asynchronously and passes the resulting {@link ImageIcon} to
   * the given {@link Consumer} when finished.
   *
   * <p>Note that while the loading of the image is done asynchronously, the call to {@link
   * Consumer#accept(Object)} is done on the EDT.
   *
   * @param imageUrl the URL to load the image from. If this is {@code null}, the given consumer is
   *     immediately called to accept {@code null}.
   * @param imageConsumer the {@link Consumer} for the final resulting {@link ImageIcon}. If there
   *     was a problem loading the image, {@code null} will be passed to this consumer.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private static void loadImageAsync(@Nullable String imageUrl, Consumer<ImageIcon> imageConsumer) {
    if (imageUrl == null) {
      imageConsumer.accept(null);
      return;
    }

    ThreadUtil.getInstance()
        .executeInBackground(
            () -> {
              ImageIcon imageIcon = null;
              try {
                URL iconUrl = new URL(imageUrl);
                Image image = SVGLoader.load(iconUrl, 0.5f);
                if (image != null) {
                  imageIcon = new ImageIcon(image);
                }
              } catch (IOException e) {
                // Do nothing.
              }

              ImageIcon finalImageIcon = imageIcon;
              ApplicationManager.getApplication()
                  .invokeAndWait(() -> imageConsumer.accept(finalImageIcon), ModalityState.any());
            });
  }

  /**
   * Optionally returns an HTML-formatted link for the given URL.
   *
   * @param key the key of the text (via {@link GctBundle#message}) to show for the link
   * @param url the URL to make into an HTML link
   * @return the HTML-formatted link, or {@link Optional#empty()} if the given URL is {@code null}
   */
  private static Optional<String> makeLink(String key, @Nullable String url) {
    if (url == null) {
      return Optional.empty();
    }
    return Optional.of(String.format("<a href=\"%s\">%s</a>", url, GctBundle.message(key)));
  }

  /**
   * Joins the given list of HTML links into a delimited string, separated by {@link
   * #LINKS_SEPARATOR} and wrapped by {@literal <html>} tags.
   *
   * @param links the list of {@link Optional} links to join. Only links that are present are joined
   *     together; {@link Optional#empty()} values are ignored.
   */
  private static String joinLinks(List<Optional<String>> links) {
    String joinedLinks =
        links
            .stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(LINKS_SEPARATOR));
    return String.format(
        "<html><body><p style=\"margin-top: 0\">%s</p></body></html>", joinedLinks);
  }

  /**
   * Returns {@code true} if the given {@link CloudLibrary CloudLibraries} are equal, {@code false}
   * otherwise.
   */
  // TODO(nkibler): Add an equals() implementation to the CloudLibrary class that does a deep field
  // comparison.
  private static boolean cloudLibrariesEqual(CloudLibrary library1, CloudLibrary library2) {
    if (library1 == library2) {
      return true;
    } else if (library1 == null || library2 == null) {
      return false;
    }
    return Objects.equals(library1.getId(), library2.getId());
  }
}
