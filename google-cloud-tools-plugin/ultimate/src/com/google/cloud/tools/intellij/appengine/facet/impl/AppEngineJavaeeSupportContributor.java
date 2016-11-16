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

import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardWebIntegration;

import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributionModel;
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributor;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public class AppEngineJavaeeSupportContributor extends JavaeeFrameworkSupportContributor {

  @Override
  public void setupFrameworkSupport(JavaeeFrameworkSupportContributionModel model) {
    AppEngineStandardFacet appEngineStandardFacet = model.getFacet(AppEngineStandardFacet.ID);
    if (appEngineStandardFacet == null) {
      return;
    }

    Artifact artifactToDeploy = model.getExplodedEarArtifact();
    if (artifactToDeploy == null) {
      artifactToDeploy = model.getExplodedWarArtifact();
    }
    if (artifactToDeploy != null) {
      AppEngineStandardWebIntegration.getInstance()
          .setupRunConfigurations(artifactToDeploy, model.getProject(), model.getRunConfiguration());
    }
  }
}
