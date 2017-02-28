/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineServerConfiguration;
import com.google.cloud.tools.intellij.debugger.CloudDebugConfigType;
import com.google.cloud.tools.intellij.debugger.CloudDebugRunConfiguration;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleRunConfiguration;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationTypesRegistrar;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class AppEngineStandardWebIntegration {

  public static AppEngineStandardWebIntegration getInstance() {
    return ServiceManager.getService(AppEngineStandardWebIntegration.class);
  }

  @Nullable
  public abstract VirtualFile suggestParentDirectoryForAppEngineWebXml(@NotNull Module module,
      @NotNull ModifiableRootModel rootModel);

  @NotNull
  public List<ArtifactType> getAppEngineTargetArtifactTypes() {
    return ContainerUtil
        .packNullables(getAppEngineWebArtifactType(), getAppEngineApplicationArtifactType());
  }

  @NotNull
  public abstract ArtifactType getAppEngineWebArtifactType();

  @Nullable
  public abstract ArtifactType getAppEngineApplicationArtifactType();

  @NotNull
  public abstract List<FrameworkSupportInModuleProvider.FrameworkDependency>
      getAppEngineFrameworkDependencies();

  @Nullable
  public abstract String getUnderlyingFrameworkTypeId();

  @NotNull
  public abstract FrameworkRole[] getFrameworkRoles();

  // TODO(joaomartins): Delete unused code.
  public abstract void setupJpaSupport(@NotNull Module module, @NotNull VirtualFile persistenceXml);

  public void setupRunConfigurations(@Nullable Artifact artifact, @Nullable Module module,
      @Nullable ModuleRunConfiguration existingConfiguration) {
    setupDebugRunConfiguration(module.getProject());
    setupDeployRunConfiguration(module);
  }

  public abstract void setupDevServer();

  // TODO(joaomartins): Delete unused code.
  public abstract void addDevServerToModuleDependencies(@NotNull ModifiableRootModel rootModel);

  public abstract void addLibraryToArtifact(@NotNull Library library, @NotNull Artifact artifact,
      @NotNull Project project);

  public void addDescriptor(@NotNull Artifact artifact, @NotNull Project project,
      @NotNull VirtualFile descriptor) {
  }

  public void registerFrameworkInModel(FrameworkSupportModel model,
      AppEngineStandardFacet appEngineStandardFacet) {
  }

  private void setupDeployRunConfiguration(@NotNull Module module) {
    AppEngineCloudType serverType =
        ServerType.EP_NAME.findExtension(AppEngineCloudType.class);
    RemoteServer<AppEngineServerConfiguration> server
        = ContainerUtil.getFirstItem(RemoteServersManager.getInstance().getServers(serverType));

    DeployToServerConfigurationType configurationType
        = DeployToServerConfigurationTypesRegistrar.getDeployConfigurationType(serverType);
    RunManager runManager = RunManager.getInstance(module.getProject());
    ConfigurationFactoryEx factory = configurationType.getFactory();
    RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
        configurationType.getDisplayName(), factory);
    DeployToServerRunConfiguration<?, ?> runConfiguration
        = (DeployToServerRunConfiguration<?, ?>)settings.getConfiguration();

    if (server != null) {
      runConfiguration.setServerName(server.getName());
    }

    runManager.addConfiguration(settings, false /*isShared*/);
  }

  private void setupDebugRunConfiguration(@NotNull Project project) {
    CloudDebugConfigType debugConfigType = CloudDebugConfigType.getInstance();
    ConfigurationFactory factory = debugConfigType.getConfigurationFactories()[0];
    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettings settings = runManager.createConfiguration(
        new CloudDebugRunConfiguration(project, factory).clone(), factory);

    runManager.addConfiguration(settings, false /*isShared*/);
  }
}
