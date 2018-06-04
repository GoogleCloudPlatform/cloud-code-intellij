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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;
import icons.GradleIcons;
import java.io.File;
import java.util.Optional;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An app-gradle-plugin based {@link ModuleDeploymentSource} and {@link AppEngineDeployable}. Sets
 * up the deployment source and provides the path to the Gradle exploded war directory stored in the
 * App Engine Gradle facet.
 */
public class GradlePluginDeploymentSource extends ModuleDeploymentSourceImpl
    implements AppEngineDeployable {

  private static final String EXPLODED_WAR_DIR_PREFIX_FORMAT = "/exploded-%s";

  private String cloudProjectName;
  private String version;

  public GradlePluginDeploymentSource(@NotNull ModulePointer pointer) {
    super(pointer);
  }

  @Override
  public AppEngineEnvironment getEnvironment() {
    return AppEngineEnvironment.APP_ENGINE_STANDARD;
  }

  @Override
  public String getProjectName() {
    return cloudProjectName;
  }

  @Override
  public void setProjectName(String projectName) {
    this.cloudProjectName = projectName;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GradleIcons.Gradle;
  }

  @Nullable
  @Override
  public File getFile() {
    if (getModule() == null) {
      return null;
    }

    AppEngineStandardFacet appEngineFacet =
        AppEngineStandardFacet.getAppEngineFacetByModule(getModule());

    if (appEngineFacet == null) {
      return null;
    }

    Optional<String> gradleBuildDirOptional = appEngineFacet.getConfiguration().getGradleBuildDir();

    return gradleBuildDirOptional
        .map(
            gradleBuildDir ->
                new File(
                    gradleBuildDir,
                    String.format(EXPLODED_WAR_DIR_PREFIX_FORMAT, getModule().getName())))
        .orElse(null);
  }

  @NotNull
  @Override
  public DeploymentSourceType<?> getType() {
    return DeploymentSourceType.EP_NAME.findExtension(GradlePluginDeploymentSourceType.class);
  }
}
