/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.SVGLoader;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
import org.fest.util.Lists;
import org.jetbrains.annotations.Nullable;

/** The form-bound class for the Cloud API details panel. */
public final class GoogleCloudApiDetailsPanel {

  private static final String LINKS_SEPARATOR = " | ";

  private JLabel nameLabel;
  private JLabel versionLabel;
  private JPanel panel;
  private JTextPane descriptionTextPane;
  private JTextPane linksTextPane;
  private JPanel apiManagementPanel;
  private JCheckBox enableApiCheckbox;
  private JPanel managementInfoPanel;
  private JTextPane managementWarningTextPane;
  private JLabel warningLabel;

  private CloudLibrary currentCloudLibrary;
  private CloudApiManagementSpec currentCloudApiManagementSpec;

  private final List<Optional<String>> links = Lists.newArrayList();

  /**
   * Optionally returns an HTML-formatted link for the given URL.
   *
   * @param text the text to show for the link
   * @param url the URL to make into an HTML link
   * @return the HTML-formatted link, or {@link Optional#empty()} if the given URL is {@code null}
   */
  public static Optional<String> makeLink(String text, @Nullable String url) {
    if (url == null) {
      return Optional.empty();
    }
    return Optional.of(String.format("<a href=\"%s\">%s</a>", url, text));
  }

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
  void setCloudLibrary(CloudLibrary library, CloudApiManagementSpec cloudApiManagementSpec) {
    if (cloudLibrariesEqual(currentCloudLibrary, library)) {
      return;
    }

    currentCloudLibrary = library;
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

  void addCloudLibraryLinks(Collection<Optional<String>> addedLinks) {
    links.addAll(addedLinks);
    linksTextPane.setText(joinLinks(links));
  }

  public CloudLibrary getCurrentCloudLibrary() {
    return currentCloudLibrary;
  }

  /** Returns the {@link JLabel} that holds the library's icon. */
  @VisibleForTesting
  JLabel getIcon() {
    return nameLabel;
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
            GoogleCloudApisMessageBundle.message("cloud.apis.management.section.title")));

    managementWarningTextPane = new JTextPane();
    managementWarningTextPane.setOpaque(false);

    warningLabel = new JLabel();
    warningLabel.setIcon(General.Information);

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
      nameLabel.setIcon(null);
    } else {
      nameLabel.setIcon(GoogleCloudCoreIcons.LOADING);
      loadImageAsync(currentCloudLibrary.getIcon(), nameLabel::setIcon);
    }

    nameLabel.setText(currentCloudLibrary.getName());
    descriptionTextPane.setText(currentCloudLibrary.getDescription());
    descriptionTextPane.setSize(
        descriptionTextPane.getWidth(), descriptionTextPane.getPreferredSize().height);

    links.clear();
    Optional<String> docsLink =
        makeLink(
            GoogleCloudApisMessageBundle.message("cloud.libraries.documentation.link"),
            currentCloudLibrary.getDocumentation());

    links.add(docsLink);
    linksTextPane.setText(joinLinks(links));

    managementWarningTextPane.setText(
        GoogleCloudApisMessageBundle.message("cloud.apis.management.section.info.text"));
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
