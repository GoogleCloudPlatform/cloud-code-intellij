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

package com.intellij.appengine.facet.impl;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineSupportProvider;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineWebIntegration;
import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdk;

import com.intellij.appengine.server.instance.AppEngineServerModel;
import com.intellij.appengine.server.integration.AppEngineServerData;
import com.intellij.appengine.server.integration.AppEngineServerIntegration;
import com.intellij.appengine.server.run.AppEngineServerConfigurationType;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.facet.FacetManager;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider.FrameworkDependency;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.javaee.JavaeePersistenceDescriptorsConstants;
import com.intellij.javaee.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.facet.JavaeeFrameworkSupportInfoCollector;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.serverInstances.ApplicationServersManager;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.jpa.facet.JpaFacet;
import com.intellij.jpa.facet.JpaFacetType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.descriptors.ConfigFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineUltimateWebIntegration extends AppEngineWebIntegration {

  @NotNull
  @Override
  public ArtifactType getAppEngineWebArtifactType() {
    return WebArtifactUtil.getInstance().getExplodedWarArtifactType();
  }

  @Nullable
  @Override
  public ArtifactType getAppEngineApplicationArtifactType() {
    return JavaeeArtifactUtil.getInstance().getExplodedEarArtifactType();
  }

  public VirtualFile suggestParentDirectoryForAppEngineWebXml(@NotNull Module module,
      @NotNull ModifiableRootModel rootModel) {
    final WebFacet webFacet = ContainerUtil.getFirstItem(WebFacet.getInstances(module));
    if (webFacet == null) {
      return null;
    }

    ConfigFile configFile = webFacet.getWebXmlDescriptor();
    if (configFile == null) {
      return null;
    }

    final VirtualFile webXml = configFile.getVirtualFile();
    if (webXml == null) {
      return null;
    }

    return webXml.getParent();
  }

  public void setupJpaSupport(@NotNull Module module, @NotNull VirtualFile persistenceXml) {
    JpaFacet facet = FacetManager.getInstance(module).getFacetByType(JpaFacet.ID);
    if (facet == null) {
      final JpaFacet jpaFacet = FacetManager.getInstance(module).addFacet(
          JpaFacetType.getInstance(), JpaFacetType.getInstance().getDefaultFacetName(), null);
      jpaFacet.getDescriptorsContainer().getConfiguration().replaceConfigFile(
          JavaeePersistenceDescriptorsConstants.PERSISTENCE_XML_META_DATA, persistenceXml.getUrl());
    }
  }

  public void setupRunConfiguration(@NotNull AppEngineSdk sdk,
      Artifact artifact,
      @NotNull Project project) {
    final ApplicationServer appServer = getOrCreateAppServer(sdk);
    if (appServer != null) {
      AppEngineServerConfigurationType configurationType = AppEngineServerConfigurationType
          .getInstance();
      List<RunnerAndConfigurationSettings> list = RunManager.getInstance(project)
          .getConfigurationSettingsList(configurationType);
      if (list.isEmpty()) {
        final RunnerAndConfigurationSettings settings = J2EEConfigurationFactory.getInstance()
            .addAppServerConfiguration(project, configurationType.getLocalFactory(), appServer);
        if (artifact != null) {
          final CommonModel configuration = (CommonModel) settings.getConfiguration();
          ((AppEngineServerModel) configuration.getServerModel()).setArtifact(artifact);
          BuildArtifactsBeforeRunTaskProvider
              .setBuildArtifactBeforeRun(project, configuration, artifact);
        }
      }
    }
  }

  @Override
  public void addDevServerToModuleDependencies(@NotNull ModifiableRootModel rootModel,
      @NotNull AppEngineSdk sdk) {
    final ApplicationServer appServer = getOrCreateAppServer(sdk);
    if (appServer != null) {
      rootModel.addLibraryEntry(appServer.getLibrary()).setScope(DependencyScope.PROVIDED);
    }
  }

  @Override
  public void addLibraryToArtifact(@NotNull Library library, @NotNull Artifact artifact,
      @NotNull Project project) {
    WebArtifactUtil.getInstance().addLibrary(library, artifact, project);
  }

  public void setupDevServer(@NotNull final AppEngineSdk sdk) {
    getOrCreateAppServer(sdk);
  }

  private static ApplicationServer getOrCreateAppServer(AppEngineSdk sdk) {
    if (!sdk.isValid()) {
      return null;
    }
    final ApplicationServersManager serversManager = ApplicationServersManager.getInstance();
    final AppEngineServerIntegration integration = AppEngineServerIntegration.getInstance();

    final List<ApplicationServer> servers = serversManager.getApplicationServers(integration);
    File sdkHomeFile = new File(sdk.getSdkHomePath());
    for (ApplicationServer server : servers) {
      final String path = ((AppEngineServerData) server.getPersistentData()).getSdkPath();
      if (FileUtil.filesEqual(sdkHomeFile, new File(path))) {
        return server;
      }
    }

    return ApplicationServersManager.getInstance()
        .createServer(integration, new AppEngineServerData(sdk.getSdkHomePath()));
  }

  public List<? extends AppEngineSdk> getSdkForConfiguredDevServers() {
    final List<ApplicationServer> servers = ApplicationServersManager.getInstance()
        .getApplicationServers(AppEngineServerIntegration.getInstance());
    List<AppEngineSdk> sdkList = new ArrayList<AppEngineSdk>();
    for (ApplicationServer server : servers) {
      final AppEngineSdk sdk = ((AppEngineServerData) server.getPersistentData()).getSdk();
      if (sdk.isValid()) {
        sdkList.add(sdk);
      }
    }
    return sdkList;
  }

  @Override
  public void registerFrameworkInModel(FrameworkSupportModel model, AppEngineFacet appEngineFacet) {
    JavaeeFrameworkSupportInfoCollector.getOrCreateCollector(model)
        .setFacet(AppEngineFacet.ID, appEngineFacet);
  }

  @Override
  @NotNull
  public List<FrameworkDependency> getAppEngineFrameworkDependencies() {
    return Arrays.asList(FrameworkDependency.required("web"), FrameworkDependency.optional(
        AppEngineSupportProvider.JPA_FRAMEWORK_ID));
  }
}
