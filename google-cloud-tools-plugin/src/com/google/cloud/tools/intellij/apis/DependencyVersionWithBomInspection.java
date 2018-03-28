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
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlTag;
import java.util.Set;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * An {@link LocalInspectionTool} that detects google-cloud-java dependencies that have an explicit
 * version definition when a BOM is defined.
 *
 * <p>Provides a quick-fix to strip out the version tag from the dependency.
 */
// TODO refactor to use the (probably more efficient) DomElementInsepction as a base
public class DependencyVersionWithBomInspection extends XmlSuppressableInspectionTool {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesAction.class);

  @Nullable
  @Override
  public String getStaticDescription() {
    return GctBundle.getString("cloud.libraries.version.with.bom.inspection.description");
  }

  /**
   * Returns a new {@link XmlElementVisitor} that visits each tag in the pom.xml.
   *
   * <p>If it finds a version tag defined within a google-cloud-java dependency AND a BOM is also
   * imported in the pom, then an inspection warning will be registered with a quickfix.
   */
  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override
      public void visitXmlTag(XmlTag tag) {
        Module module = getModule(tag);

        if (module == null) {
          return;
        }

        Set<CloudLibrary> cloudLibraries =
            CloudLibraryProjectState.getInstance(module.getProject()).getCloudLibraries(module);

        if (cloudLibraries.isEmpty()) {
          return;
        }

        // TODO dig into the lifecycle of these inspections - does this check need to be done on
        // each visit?
        if (!CloudLibraryProjectState.getInstance(module.getProject())
            .getCloudLibraryBom(module)
            .isPresent()) {
          return;
        }

        if (isRegularDependencyVersionTag(tag)
            && isCloudLibraryDependency(getParentTagNullSafe(tag), cloudLibraries)) {
          holder.registerProblem(
              tag,
              GctBundle.message("cloud.libraries.version.with.bom.inspection.problem.description"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
              new StripDependencyVersionQuickFix());
        }
      }
    };
  }

  /**
   * Checks the supplied maven dependency {@link XmlTag} to see if it is contained in the known set
   * of managed Google {@link CloudLibrary cloudLibraries}.
   *
   * @param dependencyTag the maven dependency tag we are checking
   * @param cloudLibraries the set of {@link CloudLibrary cloudLibraries} configured in the project
   * @return {@code true} if the maven dependency is managed, and {@code false} otherwise
   */
  private boolean isCloudLibraryDependency(XmlTag dependencyTag, Set<CloudLibrary> cloudLibraries) {
    XmlTag groupTag = dependencyTag.findFirstSubTag("groupId");
    XmlTag artifactTag = dependencyTag.findFirstSubTag("artifactId");

    return cloudLibraries
        .stream()
        .anyMatch(
            library ->
                CloudLibraryUtils.getFirstJavaClientMavenCoordinates(library)
                    .map(
                        coords ->
                            tagValueEquals(artifactTag, coords.getArtifactId())
                                && tagValueEquals(groupTag, coords.getGroupId()))
                    .orElse(false));
  }

  /**
   * Checks the supplied maven dependency version {@link XmlTag} to see if it is contained within a
   * regular maven dependency.
   *
   * <p>A regular dependency is defined here as a "dependency" tag that lives under the root project
   * (in contrast to dependencies living under "dependencyManagement"):
   *
   * <pre>{@code
   * <project>
   *   <dependencies>
   *     <dependency></dependency>
   *    </dependencies>
   * </project>
   *
   * }</pre>
   */
  private boolean isRegularDependencyVersionTag(XmlTag versionTag) {
    XmlTag dependencyTag = getParentTagNullSafe(versionTag);
    XmlTag dependenciesTag = getParentTagNullSafe(dependencyTag);
    XmlTag projectTag = getParentTagNullSafe(dependenciesTag);

    return tagNameEquals(versionTag, "version")
        && tagNameEquals(dependencyTag, "dependency")
        && tagNameEquals(dependenciesTag, "dependencies")
        && tagNameEquals(projectTag, "project");
  }

  private boolean tagNameEquals(XmlTag tag, String name) {
    return tag != null && name.equals(tag.getName());
  }

  private boolean tagValueEquals(XmlTag tag, String value) {
    return tag != null && value.equals(tag.getValue().getTrimmedText());
  }

  private XmlTag getParentTagNullSafe(XmlTag tag) {
    if (tag != null && tag.getParentTag() != null) {
      return tag.getParentTag();
    }

    return null;
  }

  private Module getModule(XmlTag tag) {
    try {
      Project project = tag.getContainingFile().getProject();

      MavenProject mavenProject =
          MavenProjectsManager.getInstance(project)
              .findProject(tag.getContainingFile().getVirtualFile());

      if (mavenProject == null) {
        return null;
      }

      return MavenProjectsManager.getInstance(project).findModule(mavenProject);

    } catch (PsiInvalidElementAccessException ex) {
      logger.warn("Error retrieving module containing pom.xml version tag");
      return null;
    }
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
      // todo error handling on failures
      XmlTag xmlTag = (XmlTag) descriptor.getPsiElement();
      xmlTag.delete();
    }
  }
}
