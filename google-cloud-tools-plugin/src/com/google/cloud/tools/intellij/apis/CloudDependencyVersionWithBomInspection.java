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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.xml.XmlTextImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

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
    return GctBundle.getString("cloud.libraries.version.with.bom.inspection.description");
  }

  @Override
  public void checkFileElement(
      DomFileElement<MavenDomProjectModel> domFileElement, DomElementAnnotationHolder holder) {
    MavenDomProjectModel projectModel = domFileElement.getRootElement();

    checkDependencyVersionWithBom(projectModel, holder);
  }

  /**
   * Locates google-cloud-java dependencies that have a version tag defined when a BOM is also
   * imported in the pom.xml. Displays an inspection warning and suggests a quickfix to delete the
   * version from the dependency.
   */
  private void checkDependencyVersionWithBom(
      MavenDomProjectModel projectModel, DomElementAnnotationHolder holder) {
    Module module = projectModel.getModule();

    if (module == null) {
      return;
    }

    if (!CloudLibraryProjectState.getInstance(module.getProject())
        .getCloudLibraryBomVersion(module)
        .isPresent()) {
      return;
    }

    checkCloudDependencies(
        projectModel,
        module,
        dependency -> {
          if (hasVersion(dependency)) {
            holder.createProblem(
                dependency.getVersion(),
                HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
                GctBundle.message(
                    "cloud.libraries.version.with.bom.inspection.problem.description"),
                new StripDependencyVersionQuickFix());
          }
        });
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
      return GctBundle.message("cloud.libraries.version.with.bom.quickfix.title");
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
      } else {
        logger.warn("Could not locate version tag to delete for DependencyVersionWithBom quickfix");
      }
    }
  }
}
