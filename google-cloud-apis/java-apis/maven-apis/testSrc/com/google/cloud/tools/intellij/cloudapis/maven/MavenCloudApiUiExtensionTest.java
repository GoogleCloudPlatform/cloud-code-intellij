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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.cloudapis.CloudApiUiPresenter;
import com.google.cloud.tools.intellij.cloudapis.maven.CloudApiMavenService.LibraryVersionFromBomException;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.icons.AllIcons.General;
import java.util.Optional;
import javax.swing.Icon;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Unit tests for {@link MavenCloudApiUiExtension}. */
public class MavenCloudApiUiExtensionTest {
  private static final String BOM_VERSION = "1.2.3-alpha";
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_1 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-1", "1.0.0");
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_2 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-2", "2.0.0");
  private static final TestCloudLibraryClient JAVA_CLIENT_1 =
      TestCloudLibraryClient.create(
          "Client 1",
          "java",
          "API Ref 1",
          "alpha",
          "Source 1",
          "Lang Level 1",
          JAVA_CLIENT_MAVEN_COORDS_1);

  private static final TestCloudLibrary LIBRARY_1 =
      TestCloudLibrary.create(
          "Library 1",
          "ID 1",
          "service_1",
          "Docs Link 1",
          "Description 1",
          "Icon Link 1",
          ImmutableList.of(JAVA_CLIENT_1));

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private MavenCloudApiUiExtension mavenCloudApiUiExtension;
  @Mock @TestService private CloudApiUiPresenter mockCloudApiUiPresenter;
  @Mock @TestService private CloudApiMavenService mavenService;

  private ArgumentCaptor<String> versionText = ArgumentCaptor.forClass(String.class);
  private ArgumentCaptor<Icon> versionIcon = ArgumentCaptor.forClass(Icon.class);

  @Before
  public void setUp() {
    mavenCloudApiUiExtension = new MavenCloudApiUiExtension();

    doNothing()
        .when(mockCloudApiUiPresenter)
        .updateCloudLibraryVersionLabel(versionText.capture(), versionIcon.capture());
  }

  @Test
  public void
      updateVersionLabel_withNoVersionReturnedFromBomQuery_fallsBackToAndDisplaysStaticVersion() {
    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();

    mavenCloudApiUiExtension.onCloudLibrarySelection(cloudLibrary, null);

    assertThat(versionText.getValue())
        .isEqualTo("Version: " + JAVA_CLIENT_MAVEN_COORDS_1.version());
  }

  @Test
  public void updateVersionLabel_withVersionReturnedFromBomQuery_displaysVersionFromBom()
      throws LibraryVersionFromBomException {
    String libVersion = "9.9-alpha";
    when(mavenService.getManagedDependencyVersion(any(), anyString()))
        .thenReturn(Optional.of(libVersion));
    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();

    mavenCloudApiUiExtension.onCloudLibrarySelection(cloudLibrary, BOM_VERSION);

    assertThat(versionText.getAllValues()).contains("Version: " + libVersion);
  }

  @Test
  public void updateVersionLabel_withBomAndNoDependencyVersion_displaysVersionNotFoundMessage()
      throws LibraryVersionFromBomException {
    when(mavenService.getManagedDependencyVersion(any(), anyString())).thenReturn(Optional.empty());
    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();

    mavenCloudApiUiExtension.onCloudLibrarySelection(cloudLibrary, BOM_VERSION);

    assertThat(versionText.getAllValues())
        .contains(
            /**/
            "Version: Library was not found in version "
                + BOM_VERSION
                + " of the Google Cloud Java Libraries");
    assertThat(versionIcon.getValue()).isEqualTo(General.Error);
  }

  @Test
  public void
      updateVersionLabel_whenFetchingDependencyVersionFromBomThrowsException_displaysErrorMessage()
          throws LibraryVersionFromBomException {
    when(mavenService.getManagedDependencyVersion(any(), anyString()))
        .thenThrow(new LibraryVersionFromBomException("Bom not found"));
    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();

    mavenCloudApiUiExtension.onCloudLibrarySelection(cloudLibrary, BOM_VERSION);

    assertThat(versionText.getValue())
        .isEqualTo("Version: Error occurred fetching library version");
    assertThat(versionIcon.getValue()).isEqualTo(General.Error);
  }
}
