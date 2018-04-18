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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementsInspection;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

/**
 * Base {@link DomElementsInspection} for inspecting google-cloud-java BOM related issues in the
 * project's pom.xml.
 */
abstract class CloudBomInspection extends DomElementsInspection<MavenDomProjectModel> {

  private static final Logger logger = Logger.getInstance(CloudBomInspection.class);

  CloudBomInspection() {
    super(MavenDomProjectModel.class);
  }

  /**
   * Iterates through the google cloud dependencies in the supplied {@link MavenDomProjectModel} and
   * applies the given {@link Consumer} on the dependency to create the inspection warning and
   * quickfix.
   *
   * @param projectModel the DOM model of the given pom.xml
   * @param module the current module
   * @param inspectionAndQuickFix a consumer callback that applies the inspection warning and
   *     quickfix
   */
  void checkCloudDependencies(
      MavenDomProjectModel projectModel,
      Module module,
      Consumer<MavenDomDependency> inspectionAndQuickFix) {
    Set<CloudLibrary> cloudLibraries =
        CloudLibraryProjectState.getInstance(module.getProject()).getCloudLibraries(module);

    if (cloudLibraries.isEmpty()) {
      return;
    }

    projectModel
        .getDependencies()
        .getDependencies()
        .forEach(
            dependency -> {
              if (isCloudLibraryDependency(dependency, cloudLibraries)) {
                inspectionAndQuickFix.accept(dependency);
              }
            });
  }

  /** Deletes the supplied version {@link XmlTag}. */
  void stripVersion(XmlTag versionTag) {
    try {
      versionTag.delete();
    } catch (IncorrectOperationException ioe) {
      logger.warn("Failed to delete version tag for CloudBomInspection quickfix");
    }
  }

  /**
   * Checks the supplied {@link MavenDomDependency} to see if it is contained in the known set of
   * managed Google {@link CloudLibrary cloudLibraries}.
   *
   * @param dependency the maven dependency we are checking
   * @param cloudLibraries the set of {@link CloudLibrary cloudLibraries} configured in the project
   * @return {@code true} if the maven dependency is a google cloud library, and {@code false}
   *     otherwise
   */
  boolean isCloudLibraryDependency(
      MavenDomDependency dependency, Set<CloudLibrary> cloudLibraries) {
    return cloudLibraries
        .stream()
        .anyMatch(
            library ->
                CloudLibraryUtils.getFirstJavaClientMavenCoordinates(library)
                    .map(
                        coords ->
                            domValueEquals(dependency.getGroupId(), coords.getGroupId())
                                && domValueEquals(
                                    dependency.getArtifactId(), coords.getArtifactId()))
                    .orElse(false));
  }

  private boolean domValueEquals(GenericDomValue domValue, @Nullable String value) {
    return domValue != null
        && domValue.getStringValue() != null
        && domValue.getStringValue().equals(value);
  }
}
