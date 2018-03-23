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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableList;
import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.Optional;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;
import org.jetbrains.idea.maven.aether.ArtifactKind;
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager;

/** Aether-based application service providing Cloud API Maven functionality. */
public class CloudApiMavenService {
  private static final Logger logger = Logger.getInstance(CloudApiManager.class);

  private final RepositorySystem SYSTEM;
  private final RepositorySystemSession SESSION;

  static final String GOOGLE_CLOUD_JAVA_BOM_GROUP = "com.google.cloud";
  static final String GOOGLE_CLOUD_JAVA_BOM_ARTIFACT = "google-cloud-bom";
  static final String GOOGLE_CLOUD_JAVA_BOM_TYPE = "pom";
  static final String GOOGLE_CLOUD_JAVA_BOM_SCOPE = "import";
  private static final String GOOGLE_CLOUD_JAVA_BOM_ALL_VERSIONS_CONSTRAINT = "[0,)";

  private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY =
      ArtifactRepositoryManager.createRemoteRepository("central", "http://repo1.maven.org/maven2/");

  static CloudApiMavenService getInstance() {
    return ServiceManager.getService(CloudApiMavenService.class);
  }

  private CloudApiMavenService() {
    SYSTEM = newRepositorySystem();
    SESSION = newRepositorySystemSession(SYSTEM);
  }

  /**
   * Returns the available Google Cloud Java client library BOM versions from Maven Central.
   *
   * @return returns the {@link Version versions} of the BOMs
   */
  List<Version> getBomVersions() {
    Artifact artifact =
        new DefaultArtifact(toBomCoordinates(GOOGLE_CLOUD_JAVA_BOM_ALL_VERSIONS_CONSTRAINT));

    VersionRangeRequest rangeRequest = new VersionRangeRequest();
    rangeRequest.setArtifact(artifact);
    rangeRequest.addRepository(MAVEN_CENTRAL_REPOSITORY);

    try {
      VersionRangeResult result = SYSTEM.resolveVersionRange(SESSION, rangeRequest);

      return result.getVersions();
    } catch (VersionRangeResolutionException e) {
      logger.warn("Error fetching available BOM versions from Maven Central", e);
      return ImmutableList.of();
    }
  }

  /**
   * Finds the version of the passed in {@link CloudLibraryClientMavenCoordinates} that is managed
   * by the given BOM version.
   *
   * @param libraryMavenCoordinates the maven coordinates of the {@link CloudLibrary} for which we
   *     are finding the version
   * @param bomVersion the version of the BOM from which to fetch the library version
   * @return the optional version of the library found in the given BOM
   */
  Optional<String> getManagedDependencyVersion(
      CloudLibraryClientMavenCoordinates libraryMavenCoordinates, String bomVersion) {
    Artifact bomArtifact = new DefaultArtifact(toBomCoordinates(bomVersion));

    ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
    request.setArtifact(bomArtifact);
    request.addRepository(MAVEN_CENTRAL_REPOSITORY);

    try {
      ArtifactDescriptorResult result = SYSTEM.readArtifactDescriptor(SESSION, request);
      return result
          .getManagedDependencies()
          .stream()
          .filter(
              dependency -> {
                Artifact artifact = dependency.getArtifact();
                String coordinatesFromBom =
                    toFormattedMavenCoordinates(
                        libraryMavenCoordinates.getGroupId(),
                        libraryMavenCoordinates.getArtifactId());
                String libraryCoordinates =
                    toFormattedMavenCoordinates(artifact.getGroupId(), artifact.getArtifactId());

                return coordinatesFromBom.equalsIgnoreCase(libraryCoordinates);
              })
          .findFirst()
          .map(dependency -> dependency.getArtifact().getVersion());
    } catch (ArtifactDescriptorException e) {
      logger.warn("Error fetching version of client library from bom version " + bomVersion);
      return Optional.empty();
    }
  }

  private static String toFormattedMavenCoordinates(String groupName, String artifactName) {
    return groupName + ":" + artifactName;
  }

  private static String toBomCoordinates(String versionConstraint) {
    return String.format(
        "%s:%s:%s:%s",
        GOOGLE_CLOUD_JAVA_BOM_GROUP,
        GOOGLE_CLOUD_JAVA_BOM_ARTIFACT,
        ArtifactKind.ARTIFACT,
        versionConstraint);
  }

  private static RepositorySystem newRepositorySystem() {
    final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    locator.setErrorHandler(
        new DefaultServiceLocator.ErrorHandler() {
          @Override
          public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
            if (exception != null) {
              throw new RuntimeException(exception);
            }
          }
        });
    return locator.getService(RepositorySystem.class);
  }

  private static DefaultRepositorySystemSession newRepositorySystemSession(
      RepositorySystem system) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(
        system.newLocalRepositoryManager(
            session, new LocalRepository(JarRepositoryManager.getLocalRepositoryPath())));

    return session;
  }
}
