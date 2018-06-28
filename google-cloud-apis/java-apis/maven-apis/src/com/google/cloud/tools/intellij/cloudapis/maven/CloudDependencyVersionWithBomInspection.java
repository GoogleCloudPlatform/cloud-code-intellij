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

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerService;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.xml.XmlTextImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;

/**
 * A {@link DomElementsInspection} that detects google-cloud-java dependencies in pom.xml files that
 * have an explicit version definition when a BOM is defined.
 *
 * <p>Provides a quick-fix to strip out the version tag from the dependency.
 */
public class CloudDependencyVersionWithBomInspection extends CloudBomInspection {

  private static final Logger logger =
      Logger.getInstance(CloudDependencyVersionWithBomInspection.class);

  @Nullable
  @Override
  public String getStaticDescription() {
    return MavenCloudApisMessageBundle.getString(
        "cloud.libraries.version.with.bom.inspection.description");
  }

  /** Only apply the inspection if there is a BOM defined. */
  @Override
  boolean shouldApplyInspection(Module module) {
    return CloudLibraryMavenProjectState.getInstance(module.getProject())
        .getCloudLibraryBomVersion(module)
        .isPresent();
  }

  /**
   * Displays an inspection warning and suggests a quickfix to delete the version from the google
   * cloud dependency if it has a version defined.
   */
  @Override
  void inspectAndFix(
      MavenDomDependency dependency, Module module, DomElementAnnotationHolder holder) {
    if (hasVersion(dependency)) {
      holder.createProblem(
          dependency.getVersion(),
          HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
          MavenCloudApisMessageBundle.message(
              "cloud.libraries.version.with.bom.inspection.problem.description"),
          new StripDependencyVersionQuickFix());
    }
  }

  /** Checks to see if the {@link MavenDomDependency} has a version defined. */
  private boolean hasVersion(MavenDomDependency dependency) {
    GenericDomValue<String> version = dependency.getVersion();

    return version != null && version.exists();
  }

  /**
   * A {@link LocalQuickFix} that will delete the {@link XmlTag} contained within the {@link
   * ProblemDescriptor}.
   */
  private class StripDependencyVersionQuickFix implements LocalQuickFix {
    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return MavenCloudApisMessageBundle.message("cloud.libraries.version.with.bom.quickfix.title");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      XmlTextImpl xmlElement = (XmlTextImpl) descriptor.getPsiElement();
      if (xmlElement == null) {
        logger.error(
            "Unexpected null xml element when attempting to apply DependencyVersionWithBom "
                + "quick-fix");
        return;
      }

      XmlTag versionTag = xmlElement.getParentTag();
      if (versionTag != null) {
        stripVersion(versionTag);

        UsageTrackerService.getInstance()
            .trackEvent(GctTracking.CLIENT_LIBRARY_VERSION_WITH_BOM_MAVEN_QUICKFIX)
            .ping();
      } else {
        logger.warn("Could not locate version tag to delete for DependencyVersionWithBom quickfix");
      }
    }
  }
}
