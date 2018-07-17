/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.java.facet.standard.impl;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardWebIntegration;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.BuildSystemAppEngineWebXmlDirectoryProvider;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.util.ArrayUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik. */
public class AppEngineStandardCommunityWebIntegration extends AppEngineStandardWebIntegration {

  private static final Logger LOG =
      Logger.getInstance(AppEngineStandardCommunityWebIntegration.class);

  private static final String DEFAULT_NATIVE_WEB_DIR_NAME = "web";

  @Nullable
  @Override
  public VirtualFile suggestParentDirectoryForAppEngineWebXml(
      @NotNull Module module, @NotNull ModifiableRootModel rootModel) {
    BuildSystemAppEngineWebXmlDirectoryProvider[] appEngineWebXmlDirectoryProviders =
        Extensions.getExtensions(BuildSystemAppEngineWebXmlDirectoryProvider.EP_NAME);

    // Finds any (one only one) registered appengine-web.xml directory provider extension and uses
    // this to fetch the path to the appengine-web.xml directory. If multiple are registered (i.e.
    // there are multiple build systems in use), then one will be selected arbitrarily.
    for (BuildSystemAppEngineWebXmlDirectoryProvider provider : appEngineWebXmlDirectoryProviders) {
      Optional<String> pathOptional = provider.getAppEngineWebXmlDirectoryPath(module);

      if (pathOptional.isPresent()) {
        try {
          return VfsUtil.createDirectoryIfMissing(pathOptional.get());
        } catch (IOException ioe) {
          LOG.warn(
              "Exception attempting to create appengine-web.xml in location specified by build-aware extension",
              ioe);
        }
      }
    }

    // Build-aware strategies failed or missing, fall back to simple native approach and use the
    // default IDEA web project structure
    VirtualFile root = ArrayUtil.getFirstElement(rootModel.getContentRoots());
    if (root != null) {
      try {
        return VfsUtil.createDirectoryIfMissing(
            root, String.format("%s/WEB-INF", DEFAULT_NATIVE_WEB_DIR_NAME));
      } catch (IOException ioe) {
        LOG.info(ioe);
        return null;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public ArtifactType getAppEngineWebArtifactType() {
    return PlainArtifactType.getInstance();
  }

  @Nullable
  @Override
  public String getUnderlyingFrameworkTypeId() {
    return null;
  }

  @NotNull
  @Override
  public FrameworkRole[] getFrameworkRoles() {
    return new FrameworkRole[0];
  }

  @Override
  public void setupJpaSupport(@NotNull Module module, @NotNull VirtualFile persistenceXml) {}

  @Override
  public void setupDevServer() {}

  @Override
  public void addDevServerToModuleDependencies(@NotNull ModifiableRootModel rootModel) {}

  @Override
  public void addLibraryToArtifact(
      @NotNull Library library, @NotNull Artifact artifact, @NotNull Project project) {
    final ArtifactManager artifactManager = ArtifactManager.getInstance(project);
    for (PackagingElement<?> element :
        PackagingElementFactory.getInstance().createLibraryElements(library)) {
      final String dir =
          element
                  .getFilesKind(artifactManager.getResolvingContext())
                  .containsDirectoriesWithClasses()
              ? "classes"
              : "lib";
      artifactManager.addElementsToDirectory(artifact, "WEB-INF/" + dir, element);
    }
  }

  @Override
  public void addDescriptor(
      @NotNull Artifact artifact, @NotNull Project project, @NotNull VirtualFile descriptor) {
    ArtifactManager.getInstance(project)
        .addElementsToDirectory(
            artifact,
            "WEB-INF",
            PackagingElementFactory.getInstance().createFileCopy(descriptor.getPath(), null));
  }

  @Override
  @NotNull
  public List<FrameworkSupportInModuleProvider.FrameworkDependency>
      getAppEngineFrameworkDependencies() {
    return Collections.emptyList();
  }
}
