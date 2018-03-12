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

import com.google.common.collect.ImmutableList;
import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.version.Version;
import org.jetbrains.idea.maven.aether.ArtifactKind;
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager;
import org.jetbrains.idea.maven.aether.ProgressConsumer;

/** An application service providing Cloud API Maven functionality. */
public class CloudApiMavenService {
  private static final Logger logger = Logger.getInstance(CloudApiManager.class);

  private static final String GOOGLE_CLOUD_JAVA_BOM_GROUP_NAME = "com.google.cloud";
  private static final String GOOGLE_CLOUD_JAVA_BOM_ARTIFACT_NAME = "google-cloud-bom";
  private static final String GOOGLE_CLOUD_JAVA_BOM_VERSION_CONSTRAINT = "[0,)";

  private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY =
      ArtifactRepositoryManager.createRemoteRepository("central", "http://repo1.maven.org/maven2/");

  static CloudApiMavenService getInstance() {
    return ServiceManager.getService(CloudApiMavenService.class);
  }

  /**
   * Returns the available Google Cloud Java client library BOM versions from Maven Central.
   *
   * @return returns the {@link Version versions} of the BOMs
   */
  List<Version> getBomVersions() {
    ArtifactRepositoryManager repositoryManager =
        new ArtifactRepositoryManager(
            JarRepositoryManager.getLocalRepositoryPath(),
            ImmutableList.of(MAVEN_CENTRAL_REPOSITORY),
            ProgressConsumer.DEAF);

    try {
      return repositoryManager.getAvailableVersions(
          GOOGLE_CLOUD_JAVA_BOM_GROUP_NAME,
          GOOGLE_CLOUD_JAVA_BOM_ARTIFACT_NAME,
          GOOGLE_CLOUD_JAVA_BOM_VERSION_CONSTRAINT,
          ArtifactKind.ARTIFACT);
    } catch (Exception e) {
      logger.warn("Error fetching available BOM versions from Maven Central", e);
      return ImmutableList.of();
    }
  }
}
