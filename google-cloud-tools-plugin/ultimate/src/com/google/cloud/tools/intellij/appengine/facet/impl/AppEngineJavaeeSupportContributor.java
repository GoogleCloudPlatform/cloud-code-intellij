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

package com.google.cloud.tools.intellij.appengine.facet.impl;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineSupportProvider;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineTemplateGroupDescriptorFactory;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineWebIntegration;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;

import com.intellij.javaee.DeploymentDescriptorsConstants;
import com.intellij.javaee.application.facet.JavaeeApplicationFacet;
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributionModel;
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.util.descriptors.ConfigFile;

import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class AppEngineJavaeeSupportContributor extends JavaeeFrameworkSupportContributor {

  @Override
  public void setupFrameworkSupport(JavaeeFrameworkSupportContributionModel model) {
    AppEngineFacet appEngineFacet = model.getFacet(AppEngineFacet.ID);
    if (appEngineFacet == null) {
      return;
    }

    Artifact earArtifact = model.getModifiableExplodedEarArtifact();
    JavaeeApplicationFacet applicationFacet = model.getFacet(JavaeeApplicationFacet.ID);
    if (earArtifact != null && applicationFacet != null) {
      VirtualFile applicationDescriptorDir = getParentDirForAppDescriptor(applicationFacet);
      if (applicationDescriptorDir != null) {
        VirtualFile descriptor =
            AppEngineSupportProvider.createFileFromTemplate(
                AppEngineTemplateGroupDescriptorFactory.APP_ENGINE_APPLICATION_XML_TEMPLATE,
                applicationDescriptorDir, AppEngineUtilLegacy.APP_ENGINE_APPLICATION_XML_NAME);
        if (descriptor != null) {
          PackagingElement<?> packagingElement = PackagingElementFactory.getInstance()
              .createFileCopy(descriptor.getPath(), null);
          PackagingElementFactory.getInstance()
              .getOrCreateDirectory(earArtifact.getRootElement(), "META-INF")
              .addFirstChild(packagingElement);
        }
      }
    }
    Artifact artifactToDeploy = model.getExplodedEarArtifact();
    if (artifactToDeploy == null) {
      artifactToDeploy = model.getExplodedWarArtifact();
    }
    if (artifactToDeploy != null) {
      AppEngineWebIntegration.getInstance()
          .setupRunConfiguration(artifactToDeploy, model.getProject());
    }
  }

  private static VirtualFile getParentDirForAppDescriptor(
      @NotNull JavaeeApplicationFacet applicationFacet) {
    ConfigFile configFile = applicationFacet.getDescriptorsContainer()
        .getConfigFile(DeploymentDescriptorsConstants.APPLICATION_XML_META_DATA);
    if (configFile != null) {
      VirtualFile file = configFile.getVirtualFile();
      if (file != null) {
        return file.getParent();
      }
    }
    return null;
  }
}
