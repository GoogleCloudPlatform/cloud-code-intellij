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
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
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
 */
public class DependencyVersionWithBomInspection extends XmlSuppressableInspectionTool {

  @Nls
  @NotNull
  @Override
  public String getGroupDisplayName() {
    return "set-group-name";
  }

  @Nullable
  @Override
  public String getStaticDescription() {
    return "pom.xml inspection that does blah blah";
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override
      public void visitXmlTag(XmlTag tag) {
        Module module = getModule(tag);
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

        if (isNormalDependencyVersionTag(tag)
            && isCloudLibraryDependency(getParentTagNullSafe(tag))) {
          holder.registerProblem(
              tag,
              "Version should not be specified when you are using the google-cloud-java BOM",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
      }
    };
  }

  private boolean isCloudLibraryDependency(XmlTag dependencyTag) {
    XmlTag groupTag = dependencyTag.findFirstSubTag("groupId");

    // TODO any need to check this against the known state of managed cloud libraries?
    return groupTag != null
        && "com.google.cloud".equalsIgnoreCase(groupTag.getValue().getTrimmedText());
  }

  private boolean tagNameEquals(XmlTag tag, String value) {
    return tag != null && value.equalsIgnoreCase(tag.getName());
  }

  private boolean isNormalDependencyVersionTag(XmlTag versionTag) {
    XmlTag dependencyTag = getParentTagNullSafe(versionTag);
    XmlTag dependenciesTag = getParentTagNullSafe(dependencyTag);
    XmlTag projectTag = getParentTagNullSafe(dependenciesTag);

    return tagNameEquals(versionTag, "version")
        && tagNameEquals(dependencyTag, "dependency")
        && tagNameEquals(dependenciesTag, "dependencies")
        && tagNameEquals(projectTag, "project");
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
      //      LOG.error("Error getting project with annotation " + element.getText(), ex);
      return null;
    }
  }
}
