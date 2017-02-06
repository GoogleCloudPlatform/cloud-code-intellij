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

import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributionModel;
import com.intellij.javaee.supportProvider.JavaeeFrameworkSupportContributor;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;

/**
 * @author nik
 */
public class AppEngineJavaeeSupportContributor extends JavaeeFrameworkSupportContributor {

  private static final String SERVLET_VERSION = "2.5";
  private static final String SERVLET_NAMESPACE = "http://java.sun.com/xml/ns/javaee";
  private static final String SERVLET_SCHEMA_URL
      = "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd";

  public AppEngineJavaeeSupportContributor() {

  }

  @Override
  public void setupFrameworkSupport(JavaeeFrameworkSupportContributionModel model) {
    AppEngineStandardFacet appEngineStandardFacet = model.getFacet(AppEngineStandardFacet.ID);
    if (appEngineStandardFacet == null) {
      return;
    }

    setWebXmlServletVersion(model.getModule());

    Artifact artifactToDeploy = model.getExplodedEarArtifact();
    if (artifactToDeploy == null) {
      artifactToDeploy = model.getExplodedWarArtifact();
    }
    if (artifactToDeploy != null) {
      AppEngineStandardWebIntegration.getInstance().setupRunConfigurations(
          artifactToDeploy, model.getProject(), model.getRunConfiguration());
    }
  }

  /**
   * App Engine standard apps with the default runtime (Java7) require Servlet 2.5. This updates
   * web.xml to the correct version.
   */
  private void setWebXmlServletVersion(Module module) {
    StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(() -> {
      for (WebFacet webFacet : WebFacet.getInstances(module)) {
        final WebApp webApp = webFacet.getRoot();
        if (webApp != null) {
          new WriteCommandAction.Simple(module.getProject()) {
            @Override
            protected void run() throws Throwable {
              webApp.getVersion().setStringValue(SERVLET_VERSION);

              XmlTag webAppTag = webApp.getXmlTag();
              XmlAttribute xmlns = webAppTag.getAttribute("xmlns");
              XmlAttribute schemaLocation = webAppTag.getAttribute("xsi:schemaLocation");

              if (xmlns != null) {
                xmlns.setValue(SERVLET_NAMESPACE);
              }
              if (schemaLocation != null) {
                schemaLocation.setValue(SERVLET_SCHEMA_URL);
              }
            }
          }.execute();
        }
      }
    });
  }
}
