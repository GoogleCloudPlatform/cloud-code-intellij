/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.project;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Booleans;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DefaultAppEngineAssetProvider extends AppEngineAssetProvider {
  private static final Logger logger = Logger.getInstance(DefaultAppEngineAssetProvider.class);

  @Nullable
  @Override
  public XmlFile loadAppEngineStandardWebXml(@NotNull Project project, @NotNull Artifact artifact) {
    Set<Module> modules
        = ArtifactUtil.getModulesIncludedInArtifacts(Collections.singletonList(artifact), project);

    return loadAppEngineStandardWebXml(project, modules);
  }

  @Nullable
  @Override
  public XmlFile loadAppEngineStandardWebXml(@NotNull Project project,
      @NotNull Collection<Module> modules) {
    List<VirtualFile> appEngineWebXmls = new ArrayList<>();

    for (Module module : modules) {
      appEngineWebXmls.addAll(FilenameIndex.getVirtualFilesByName(
          project, "appengine-web.xml", module.getModuleContentScope()));
    }

    VirtualFile appEngineWebXml = findHighestPriorityAppEngineWebXml(appEngineWebXmls);

    if (appEngineWebXml != null) {
      return (XmlFile) PsiManager.getInstance(project).findFile(appEngineWebXml);
    }
    return null;
  }

  @Nullable
  @VisibleForTesting
  VirtualFile findHighestPriorityAppEngineWebXml(List<VirtualFile> appEngineWebXmls) {
    List<VirtualFile> nonNulls = new ArrayList<>();
    for (VirtualFile file : appEngineWebXmls) {
      if (file != null) {
        nonNulls.add(file);
      }
    }

    if (nonNulls.size() > 1) {
      // Prefer the appengine-web.xml located under the WEB-INF directory
      Collections.sort(nonNulls, new AppEngineWebXmlOrdering());

      logger.warn("The following appengine-web.xml's were found: " + nonNulls
          + "\nThe first one in the list will be used.");
    }

    if (!nonNulls.isEmpty()) {
      return nonNulls.get(0);
    }
    return null;
  }

  /**
   * Provides an ordering for a collection of appengine-web.xml {@link VirtualFile}'s where those
   * under the WEB-INF directory appear first.
   */
  @VisibleForTesting
  static class AppEngineWebXmlOrdering extends Ordering<VirtualFile> implements Serializable {
    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return Booleans.compare(hasWebInfParent(file2), hasWebInfParent(file1));
    }

    private boolean hasWebInfParent(VirtualFile file) {
      return file != null
          && "WEB-INF".equalsIgnoreCase(file.getParent().getName());
    }
  }
}
