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

package com.google.cloud.tools.intellij.cloudapis.maven;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.cloudapis.maven.CloudApiMavenService.LibraryVersionFromBomException;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** UI extension to support additional components and events for Maven Cloud API integration. */
public class MavenCloudApiUiExtension implements CloudApiUiExtension {

  private CloudApiUiPresenter uiPresenter;

  @Override
  public void init(@NotNull CloudApiUiPresenter uiPresenter) {
    this.uiPresenter = uiPresenter;
  }

  @Override
  public void onCurrentCloudLibrarySelected(
      CloudLibrary currentCloudLibrary, String currentBomVersion) {
    if (currentCloudLibrary != null && currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                if (currentBomVersion != null) {
                  updateManagedLibraryVersionFromBom(currentCloudLibrary, currentBomVersion);
                } else {
                  if (client.getMavenCoordinates() != null) {
                    uiPresenter.updateCloudLibraryVersionLabel(
                        MavenCloudApisMessageBundle.message(
                            "cloud.libraries.version.label",
                            client.getMavenCoordinates().getVersion()),
                        null);
                  }
                }

                makeLink(
                        MavenCloudApisMessageBundle.message("cloud.libraries.source.link"),
                        client.getSource())
                    .ifPresent(link -> uiPresenter.addCloudLibraryDocumentationLink(link));
                makeLink(
                        MavenCloudApisMessageBundle.message("cloud.libraries.apireference.link"),
                        client.getApiReference())
                    .ifPresent(link -> uiPresenter.addCloudLibraryDocumentationLink(link));
              });
    }
  }

  @Override
  public void onModuleSelected(Module module) {}

  /**
   * Optionally returns an HTML-formatted link for the given URL.
   *
   * @param text the text to show for the link
   * @param url the URL to make into an HTML link
   * @return the HTML-formatted link, or {@link Optional#empty()} if the given URL is {@code null}
   */
  // TODO move back to core cloud-api when maven module is complete.
  public static Optional<String> makeLink(String text, @Nullable String url) {
    if (url == null) {
      return Optional.empty();
    }
    return Optional.of(String.format("<a href=\"%s\">%s</a>", url, text));
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
  private void updateManagedLibraryVersionFromBom(
      CloudLibrary currentCloudLibrary, String bomVersion) {
    if (currentCloudLibrary.getClients() != null) {
      CloudLibraryUtils.getFirstJavaClient(currentCloudLibrary)
          .ifPresent(
              client -> {
                CloudLibraryClientMavenCoordinates coordinates = client.getMavenCoordinates();

                if (coordinates != null) {
                  uiPresenter.updateCloudLibraryVersionLabel("", GoogleCloudCoreIcons.LOADING);

                  ThreadUtil.getInstance()
                      .executeInBackground(
                          () -> {
                            try {
                              Optional<String> versionOptional =
                                  CloudApiMavenService.getInstance()
                                      .getManagedDependencyVersion(coordinates, bomVersion);

                              if (versionOptional.isPresent()) {
                                ApplicationManager.getApplication()
                                    .invokeAndWait(
                                        () -> uiPresenter.updateCloudLibraryVersionLabel(
                                            MavenCloudApisMessageBundle.message(
                                                "cloud.libraries.version.label",
                                                versionOptional.get()),
                                            null),
                                        ModalityState.any());

                                uiPresenter.updateCloudLibraryVersionLabel(null, null);
                              } else {
                                uiPresenter.updateCloudLibraryVersionLabel(
                                    MavenCloudApisMessageBundle.message(
                                        "cloud.libraries.version.label",
                                        MavenCloudApisMessageBundle.message(
                                            "cloud.libraries.version.notfound.text", bomVersion)),
                                    General.Error);
                              }
                            } catch (LibraryVersionFromBomException ex) {
                              uiPresenter.updateCloudLibraryVersionLabel(
                                  MavenCloudApisMessageBundle.message(
                                      "cloud.libraries.version.label",
                                      MavenCloudApisMessageBundle.message(
                                          "cloud.libraries.version.exception.text")),
                                  General.Error);
                            }
                          });
                }
              });
    }
  }
}
