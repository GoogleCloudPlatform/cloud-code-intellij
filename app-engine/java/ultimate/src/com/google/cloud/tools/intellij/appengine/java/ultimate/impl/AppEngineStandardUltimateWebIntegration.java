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

package com.google.cloud.tools.intellij.appengine.java.ultimate.impl;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardWebIntegration;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.instance.AppEngineServerModel;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.integration.AppEngineServerIntegration;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.run.AppEngineLocalServerUltimateConfigurationType;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ModuleRunConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider.FrameworkDependency;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.javaee.JavaeePersistenceDescriptorsConstants;
import com.intellij.javaee.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.facet.JavaeeFrameworkSupportInfoCollector;
import com.intellij.javaee.framework.JavaeeProjectCategory;
import com.intellij.javaee.oss.server.JavaeePersistentData;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.serverInstances.ApplicationServersManager;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.jpa.facet.JpaFacet;
import com.intellij.jpa.facet.JpaFacetType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.util.containers.ContainerUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik */
public class AppEngineStandardUltimateWebIntegration extends AppEngineStandardWebIntegration {
  public static final String WEB_INF = "WEB-INF";

  private static final Logger LOG =
      Logger.getInstance(AppEngineStandardUltimateWebIntegration.class);
  private static final FrameworkRole JAVA_PROJECT_ROLE = new FrameworkRole("JAVA_MODULE");
  private static final FrameworkRole JAVA_EE_PROJECT_ROLE = JavaeeProjectCategory.ROLE;

  @NotNull
  @Override
  public ArtifactType getAppEngineWebArtifactType() {
    return WebArtifactUtil.getInstance().getExplodedWarArtifactType();
  }

  /**
   * Returns the first WEB-INF folder determined in this order or null if none can be found. 1. If a
   * WEB-INF folder exists as one of {@code module}'s web resource directories, returns the first
   * one 2. If a WEB-INF folder is a child of one or more of the web resource directories, returns
   * the first one 3. Creates a WEB-INF folder in the first web resource directory
   */
  @Override
  public VirtualFile suggestParentDirectoryForAppEngineWebXml(
      @NotNull Module module, @NotNull ModifiableRootModel rootModel) {
    final WebFacet webFacet = ContainerUtil.getFirstItem(WebFacet.getInstances(module));
    if (webFacet == null) {
      return null;
    }

    List<WebRoot> webRoots = webFacet.getWebRoots();
    if (webRoots.isEmpty()) {
      return null;
    }

    for (WebRoot webRoot : webRoots) {
      VirtualFile webRootDir = webRoot.getFile();
      if (webRootDir != null) {
        if (WEB_INF.equals(webRootDir.getName())) {
          return webRootDir;
        }

        VirtualFile webInfDir = webRootDir.findChild(WEB_INF);
        if (webInfDir != null) {
          return webInfDir;
        }
      }
    }

    try {
      VirtualFile webRootDir = webRoots.get(0).getFile();
      if (webRootDir != null) {
        return VfsUtil.createDirectoryIfMissing(webRootDir, WEB_INF);
      } else {
        // There is a webroot, but the directory does not exist, so create it
        webRootDir = VfsUtil.createDirectories(webRoots.get(0).getPresentableUrl());
        return VfsUtil.createDirectoryIfMissing(webRootDir, WEB_INF);
      }
    } catch (IOException ioe) {
      LOG.info(ioe);
    }

    return null;
  }

  @Override
  public void setupJpaSupport(@NotNull Module module, @NotNull VirtualFile persistenceXml) {
    JpaFacet facet = FacetManager.getInstance(module).getFacetByType(JpaFacet.ID);
    if (facet == null) {
      final JpaFacet jpaFacet =
          FacetManager.getInstance(module)
              .addFacet(
                  JpaFacetType.getInstance(),
                  JpaFacetType.getInstance().getDefaultFacetName(),
                  null);
      jpaFacet
          .getDescriptorsContainer()
          .getConfiguration()
          .replaceConfigFile(
              JavaeePersistenceDescriptorsConstants.PERSISTENCE_XML_META_DATA,
              persistenceXml.getUrl());
    }
  }

  @Override
  public void setupRunConfigurations(
      Artifact artifact, @Nullable Module module, ModuleRunConfiguration existingConfiguration) {
    super.setupRunConfigurations(artifact, module, existingConfiguration);
    setupLocalDevRunConfiguration(artifact, module.getProject(), existingConfiguration);
  }

  private void setupLocalDevRunConfiguration(
      Artifact artifact, @NotNull Project project, ModuleRunConfiguration existingConfiguration) {
    final ApplicationServer appServer = getOrCreateAppServer();
    if (appServer != null) {
      AppEngineLocalServerUltimateConfigurationType configurationType =
          AppEngineLocalServerUltimateConfigurationType.getInstance();

      CommonModel configuration;
      if (existingConfiguration instanceof CommonModel
          && ((CommonModel) existingConfiguration).getServerModel()
              instanceof AppEngineServerModel) {
        configuration = (CommonModel) existingConfiguration;
      } else if (RunManager.getInstance(project)
          .getConfigurationSettingsList(configurationType)
          .isEmpty()) {
        final RunnerAndConfigurationSettings settings =
            J2EEConfigurationFactory.getInstance()
                .addAppServerConfiguration(project, configurationType.getLocalFactory(), appServer);
        configuration = (CommonModel) settings.getConfiguration();
      } else {
        configuration = null;
      }

      if (artifact != null
          && configuration != null
          && configuration.getServerModel() instanceof AppEngineServerModel) {
        ((AppEngineServerModel) configuration.getServerModel()).setArtifact(artifact);
        BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(
            project, configuration, artifact);
      }
    }
  }

  @Override
  public void addDevServerToModuleDependencies(@NotNull ModifiableRootModel rootModel) {
    final ApplicationServer appServer = getOrCreateAppServer();
    if (appServer != null) {
      rootModel.addLibraryEntry(appServer.getLibrary()).setScope(DependencyScope.PROVIDED);
    }
  }

  @Override
  public void addLibraryToArtifact(
      @NotNull Library library, @NotNull Artifact artifact, @NotNull Project project) {
    WebArtifactUtil.getInstance().addLibrary(library, artifact, project);
  }

  @Override
  public void setupDevServer() {
    getOrCreateAppServer();
  }

  private static ApplicationServer getOrCreateAppServer() {
    final CloudSdkService sdkService = CloudSdkService.getInstance();
    if (sdkService == null) {
      return null;
    }

    final AppEngineServerIntegration integration = AppEngineServerIntegration.getInstance();

    // There are no distinguishing features about the App Engine servers so just return
    // the first one found
    final List<ApplicationServer> servers =
        ApplicationServersManager.getInstance().getApplicationServers(integration);
    if (!servers.isEmpty()) {
      return servers.iterator().next();
    }

    return ApplicationServersManager.getInstance()
        .createServer(integration, new JavaeePersistentData());
  }

  @Override
  public void registerFrameworkInModel(
      FrameworkSupportModel model, AppEngineStandardFacet appEngineStandardFacet) {
    JavaeeFrameworkSupportInfoCollector.getOrCreateCollector(model)
        .setFacet(AppEngineStandardFacetType.ID, appEngineStandardFacet);
  }

  @Override
  @NotNull
  public List<FrameworkDependency> getAppEngineFrameworkDependencies() {
    return Collections.singletonList(FrameworkDependency.required("web"));
  }

  @Nullable
  @Override
  public String getUnderlyingFrameworkTypeId() {
    return WebFacet.ID.toString();
  }

  @NotNull
  @Override
  public FrameworkRole[] getFrameworkRoles() {
    return new FrameworkRole[] {JAVA_PROJECT_ROLE, JAVA_EE_PROJECT_ROLE};
  }
}
