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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineServerConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationTypesRegistrar;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Adds Flexible support to new or existing IJ modules.
 */
public class AppEngineFlexibleSupportProvider extends FrameworkSupportInModuleProvider {

  @NotNull
  @Override
  public FrameworkTypeEx getFrameworkType() {
    return AppEngineFlexibleFrameworkType.getFrameworkType();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(
      @NotNull FrameworkSupportModel model) {
    return new AppEngineFlexibleSupportConfigurable();
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module,
      @NotNull FacetsProvider facetsProvider) {
    return !facetsProvider.getFacetsByType(module, AppEngineFlexibleFacetType.ID).isEmpty()
        || !facetsProvider.getFacetsByType(module, AppEngineStandardFacet.ID).isEmpty();
  }

  /** Initializes the Flexible facet by settings the default paths for app.yaml and Dockerfile
   * and generating the necessary run configurations.
   */
  public void setupFacet(@NotNull AppEngineFlexibleFacet facet,
      @NotNull ModifiableRootModel rootModel) {
    // Allows suggesting app.yaml and Dockerfile locations in facet and deployment UIs.
    VirtualFile[] contentRoots = rootModel.getContentRoots();
    AppEngineProjectService appEngineProjectService = AppEngineProjectService.getInstance();
    if (contentRoots.length > 0) {
      facet.getConfiguration().setAppYamlPath(
          appEngineProjectService.getDefaultAppYamlPath(contentRoots[0].getPath()));
      facet.getConfiguration().setDockerfilePath(
          appEngineProjectService.getDefaultDockerfilePath(contentRoots[0].getPath()));
    }

    // TODO(joaomartins): Add other run configurations here too.
    // https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/1260
    setupDeploymentRunConfiguration(facet.getModule());
  }

  private void setupDeploymentRunConfiguration(Module module) {
    RunManager runManager = RunManager.getInstance(module.getProject());
    AppEngineCloudType serverType =
        ServerType.EP_NAME.findExtension(AppEngineCloudType.class);
    DeployToServerConfigurationType configurationType
        = DeployToServerConfigurationTypesRegistrar.getDeployConfigurationType(serverType);

    RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
        configurationType.getDisplayName(), configurationType.getFactory());

    // Sets the GAE Flex server, if any exists, in the run config.
    DeployToServerRunConfiguration<?, AppEngineDeploymentConfiguration> runConfiguration =
        (DeployToServerRunConfiguration<?, AppEngineDeploymentConfiguration>)
            settings.getConfiguration();
    RemoteServer<AppEngineServerConfiguration> server =
        ContainerUtil.getFirstItem(RemoteServersManager.getInstance().getServers(serverType));
    if (server != null) {
      runConfiguration.setServerName(server.getName());
    }

    runManager.addConfiguration(settings, false /* shared */);
  }

  class AppEngineFlexibleSupportConfigurable extends FrameworkSupportInModuleConfigurable {

    private JPanel mainPanel;
    private CloudSdkPanel cloudSdkPanel;

    @Nullable
    @Override
    public JComponent createComponent() {
      return mainPanel;
    }

    @Override
    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel,
        @NotNull ModifiableModelsProvider modifiableModelsProvider) {
      FacetType<AppEngineFlexibleFacet, AppEngineFlexibleFacetConfiguration> facetType =
          AppEngineFlexibleFacet.getFacetType();
      AppEngineFlexibleFacet facet = FacetManager.getInstance(module).addFacet(
          facetType, facetType.getPresentableName(), null /* underlyingFacet */);

      AppEngineFlexibleSupportProvider.this.setupFacet(facet, rootModel);

      CloudSdkService sdkService = CloudSdkService.getInstance();
      if (!sdkService.validateCloudSdk(cloudSdkPanel.getCloudSdkDirectoryText())
          .contains(CloudSdkValidationResult.MALFORMED_PATH)) {
        sdkService.setSdkHomePath(cloudSdkPanel.getCloudSdkDirectoryText());
      }
    }

    private void createUIComponents() {
      cloudSdkPanel = new CloudSdkPanel();
      cloudSdkPanel.reset();
    }
  }
}
