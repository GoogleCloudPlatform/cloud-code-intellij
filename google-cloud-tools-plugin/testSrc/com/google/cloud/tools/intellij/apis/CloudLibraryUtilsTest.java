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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.cloud.tools.libraries.json.CloudLibraryClient;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link CloudLibraryUtils}. */
@RunWith(JUnit4.class)
public final class CloudLibraryUtilsTest {

  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS =
      TestCloudLibraryClientMavenCoordinates.create("group", "java-client", "1.0.0");
  private static final TestCloudLibraryClientMavenCoordinates PYTHON_CLIENT_MAVEN_COORDS =
      TestCloudLibraryClientMavenCoordinates.create("group", "python-client", "1.0.0");
  private static final TestCloudLibraryClient JAVA_CLIENT_FULL =
      TestCloudLibraryClient.create(
          "Java Client", "java", "", "", "", "", JAVA_CLIENT_MAVEN_COORDS);
  private static final TestCloudLibraryClient JAVA_CLIENT_NO_MAVEN_COORDS =
      TestCloudLibraryClient.create("No Maven Coords Client", "java", "", "", "", "", null);
  private static final TestCloudLibraryClient PYTHON_CLIENT_FULL =
      TestCloudLibraryClient.create(
          "Python Client", "python", "", "", "", "", PYTHON_CLIENT_MAVEN_COORDS);

  private static final TestCloudLibrary LIBRARY_FULL =
      TestCloudLibrary.create(
          "Full Library",
          "full",
          "",
          "",
          "",
          "",
          ImmutableList.of(PYTHON_CLIENT_FULL, JAVA_CLIENT_FULL));
  private static final TestCloudLibrary LIBRARY_NO_CLIENTS =
      TestCloudLibrary.create(
          "No Clients Library", "no-clients", "", "", "", "", ImmutableList.of());
  private static final TestCloudLibrary LIBRARY_NULL_CLIENTS =
      TestCloudLibrary.create("Null Clients Library", "null-clients", "", "", "", "", null);
  private static final TestCloudLibrary LIBRARY_PYTHON_CLIENT_ONLY =
      TestCloudLibrary.create(
          "Python Client Library",
          "python-client",
          "",
          "",
          "",
          "",
          ImmutableList.of(PYTHON_CLIENT_FULL));
  private static final TestCloudLibrary LIBRARY_NO_MAVEN_COORDS =
      TestCloudLibrary.create(
          "No Maven Coords Library",
          "no-maven-coords",
          "",
          "",
          "",
          "",
          ImmutableList.of(JAVA_CLIENT_NO_MAVEN_COORDS));

  @Test
  public void getJavaClient_withFullLibrary_returnsClient() {
    Optional<CloudLibraryClient> client =
        CloudLibraryUtils.getFirstJavaClient(LIBRARY_FULL.toCloudLibrary());

    // TODO(nkibler): Use .hasValue() once CloudLibrary classes have overridden equality methods.
    assertThat(client).isPresent();
    assertThat(client.get().getName()).isEqualTo(JAVA_CLIENT_FULL.name());
  }

  @Test
  public void getJavaClient_withNoClientsLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClient> client =
        CloudLibraryUtils.getFirstJavaClient(LIBRARY_NO_CLIENTS.toCloudLibrary());

    assertThat(client).isEmpty();
  }

  @Test
  public void getJavaClient_withNullClientsLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClient> client =
        CloudLibraryUtils.getFirstJavaClient(LIBRARY_NULL_CLIENTS.toCloudLibrary());

    assertThat(client).isEmpty();
  }

  @Test
  public void getJavaClient_withPythonClientLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClient> client =
        CloudLibraryUtils.getFirstJavaClient(LIBRARY_PYTHON_CLIENT_ONLY.toCloudLibrary());

    assertThat(client).isEmpty();
  }

  @Test
  public void getJavaClientMavenCoordinates_withFullLibrary_returnsMavenCoords() {
    Optional<CloudLibraryClientMavenCoordinates> mavenCoordinates =
        CloudLibraryUtils.getFirstJavaClientMavenCoordinates(LIBRARY_FULL.toCloudLibrary());

    // TODO(nkibler): Use .hasValue() once CloudLibrary classes have overridden equality methods.
    assertThat(mavenCoordinates).isPresent();
    assertThat(mavenCoordinates.get().getArtifactId())
        .isEqualTo(JAVA_CLIENT_MAVEN_COORDS.artifactId());
  }

  @Test
  public void getJavaClientMavenCoordinates_withNoClientsLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClientMavenCoordinates> mavenCoordinates =
        CloudLibraryUtils.getFirstJavaClientMavenCoordinates(LIBRARY_NO_CLIENTS.toCloudLibrary());

    assertThat(mavenCoordinates).isEmpty();
  }

  @Test
  public void getJavaClientMavenCoordinates_withNullClientsLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClientMavenCoordinates> mavenCoordinates =
        CloudLibraryUtils.getFirstJavaClientMavenCoordinates(LIBRARY_NULL_CLIENTS.toCloudLibrary());

    assertThat(mavenCoordinates).isEmpty();
  }

  @Test
  public void getJavaClientMavenCoordinates_withPythonClientLibrary_returnsEmptyOptional() {
    Optional<CloudLibraryClientMavenCoordinates> mavenCoordinates =
        CloudLibraryUtils.getFirstJavaClientMavenCoordinates(
            LIBRARY_PYTHON_CLIENT_ONLY.toCloudLibrary());

    assertThat(mavenCoordinates).isEmpty();
  }

  @Test
  public void getJavaClientMavenCoordinates_withNoMavenCoords_returnsEmptyOptional() {
    Optional<CloudLibraryClientMavenCoordinates> mavenCoordinates =
        CloudLibraryUtils.getFirstJavaClientMavenCoordinates(
            LIBRARY_NO_MAVEN_COORDS.toCloudLibrary());

    assertThat(mavenCoordinates).isEmpty();
  }
}
