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
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.xml.XmlTextImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;
import java.util.Set;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

/**
 * An {@link LocalInspectionTool} that detects google-cloud-java dependencies that have an explicit
 * version definition when a BOM is defined.
 *
 * <p>Provides a quick-fix to strip out the version tag from the dependency.
 */
public class DependencyVersionWithBomInspection
    extends DomElementsInspection<MavenDomProjectModel> {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesAction.class);

  public DependencyVersionWithBomInspection() {
    super(MavenDomProjectModel.class);
  }

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

    Set<CloudLibrary> cloudLibraries =
        CloudLibraryProjectState.getInstance(module.getProject()).getCloudLibraries(module);

    if (cloudLibraries.isEmpty()) {
      return;
    }

    if (!CloudLibraryProjectState.getInstance(module.getProject())
        .getCloudLibraryBom(module)
        .isPresent()) {
      return;
    }

    projectModel
        .getDependencies()
        .getDependencies()
        .forEach(
            dependency -> {
              if (hasVersion(dependency) && isCloudLibraryDependency(dependency, cloudLibraries)) {
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
    return dependency.getVersion().exists();
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
  private boolean isCloudLibraryDependency(
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
    return domValue.getStringValue() != null && domValue.getStringValue().equals(value);
  }

  /**
   * A {@link LocalQuickFix} that will delete the {@link XmlTag} contained within the {@link
   * ProblemDescriptor}.
   */
  private static class StripDependencyVersionQuickFix implements LocalQuickFix {
    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return GctBundle.message("cloud.libraries.version.with.bom.quickfix.title");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      XmlTextImpl xmlElement = (XmlTextImpl) descriptor.getPsiElement();
      XmlTag versionTag = xmlElement.getParentTag();
      if (versionTag != null) {
        try {
          versionTag.delete();
        } catch (IncorrectOperationException ioe) {
          logger.warn("Failed to delete version tag for DependencyVersionWithBom quickfix");
        }
      } else {
        logger.warn("Could not locate version tag to delete for DependencyVersionWithBom quickfix");
      }
    }
  }
}
