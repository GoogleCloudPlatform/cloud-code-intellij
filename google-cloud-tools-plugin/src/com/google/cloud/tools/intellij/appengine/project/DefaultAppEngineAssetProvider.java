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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultAppEngineAssetProvider extends AppEngineAssetProvider {

  @Nullable
  @Override
  public XmlFile loadAppEngineStandardWebXml(@NotNull Project project, @NotNull Artifact artifact) {
    PackagingElementResolvingContext context = ArtifactManager.getInstance(project)
        .getResolvingContext();
    VirtualFile descriptorFile = ArtifactUtil
        .findSourceFileByOutputPath(artifact, "WEB-INF/appengine-web.xml", context);

    if (descriptorFile != null) {
      return (XmlFile) PsiManager.getInstance(project).findFile(descriptorFile);
    }

    return null;
  }
}
