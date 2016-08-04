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

package com.google.cloud.tools.intellij.appengine.server.instance;

import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtilLegacy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.deployment.DeploymentProvider;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.configuration.DeploysArtifactsOnStartupOnly;
import com.intellij.javaee.run.configuration.ServerModel;
import com.intellij.javaee.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.run.execution.OutputProcessor;
import com.intellij.javaee.serverInstances.J2EEServerInstance;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineServerModel implements ServerModel, DeploysArtifactsOnStartupOnly,
    RunConfiguration {

  private ArtifactPointer artifactPointer;
  private String serverParameters = "";
  private CommonModel commonModel;

  private final AppEngineModelSettings settings = new AppEngineModelSettings();

  @Override
  public J2EEServerInstance createServerInstance() throws ExecutionException {
    return new AppEngineServerInstance(commonModel);
  }

  @Override
  public DeploymentProvider getDeploymentProvider() {
    return null;
  }

  @Override
  @NotNull
  public String getDefaultUrlForBrowser() {
    return "http://" + commonModel.getHost() + ":" + settings.getPort();
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new AppEngineRunConfigurationEditor(commonModel.getProject());
  }

  @Override
  public OutputProcessor createOutputProcessor(ProcessHandler processHandler,
      J2EEServerInstance serverInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    return Collections.singletonList(Pair.create(commonModel.getHost(),
        settings.getPort()));
  }

  @Override
  public boolean isResourcesReloadingSupported() {
    return commonModel.isLocal();
  }

  @Override
  public List<Artifact> getArtifactsToDeploy() {
    return ContainerUtil.createMaybeSingletonList(getArtifact());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    Artifact artifact;
    if (artifactPointer == null || (artifact = artifactPointer.getArtifact()) == null) {
      throw new RuntimeConfigurationError("Artifact isn't specified");
    }

    final AppEngineFacet facet = AppEngineUtilLegacy
        .findAppEngineFacet(commonModel.getProject(), artifact);
    if (facet == null) {
      throw new RuntimeConfigurationWarning(
          "App Engine facet not found in '" + artifact.getName() + "' artifact");
    }
  }

  @Override
  public int getDefaultPort() {
    return 8080;
  }

  @Override
  public void setCommonModel(CommonModel commonModel) {
    this.commonModel = commonModel;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public int getLocalPort() {
    return settings.getPort();
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    XmlSerializer.deserializeInto(settings, element);
//    port = settings.getPort();
    serverParameters = settings.getServerParameters();
    final String artifactName = settings.getArtifact();
    if (artifactName != null) {
      artifactPointer = ArtifactPointerManager.getInstance(commonModel.getProject())
          .createPointer(artifactName);
    } else {
      artifactPointer = null;
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    XmlSerializer.serializeInto(settings, element, new SkipDefaultValuesSerializationFilters());
  }

  @Nullable
  public Artifact getArtifact() {
    return artifactPointer != null ? artifactPointer.getArtifact() : null;
  }

  public void setPort(int port) {
    settings.setPort(port);
  }

  public String getServerParameters() {
    return serverParameters;
  }

  public void setServerParameters(String serverParameters) {
    this.serverParameters = serverParameters;
  }

  public void setArtifact(@Nullable Artifact artifact) {
    if (artifact != null) {
      artifactPointer = ArtifactPointerManager.getInstance(commonModel.getProject())
          .createPointer(artifact);
    } else {
      artifactPointer = null;
    }
  }

  public AppEngineModelSettings getSettings() {
    return settings;
  }

  @Override
  public List<File> getAppYamls() {
    List<File> appYamls = new ArrayList<>();
    Path appYaml = Paths.get(artifactPointer.getArtifact().getOutputPath()).resolve("app.yaml");
    appYamls.add(appYaml.toFile());
    return appYamls;
  }

  @Override
  public String getHost() {
    return null;
  }

  @Override
  public Integer getPort() {
    return settings.getPort();
  }

  @Override
  public String getAdminHost() {
    return null;
  }

  @Override
  public Integer getAdminPort() {
    return null;
  }

  @Override
  public String getAuthDomain() {
    return null;
  }

  @Override
  public String getStoragePath() {
    return null;
  }

  @Override
  public String getLogLevel() {
    return null;
  }

  @Override
  public Integer getMaxModuleInstances() {
    return null;
  }

  @Override
  public Boolean getUseMtimeFileWatcher() {
    return null;
  }

  @Override
  public String getThreadsafeOverride() {
    return null;
  }

  @Override
  public String getPythonStartupScript() {
    return null;
  }

  @Override
  public String getPythonStartupArgs() {
    return null;
  }

  @Override
  public List<String> getJvmFlags() {
    return settings.jvmFlags;
  }

  public void addJvmFlag(String flag) {
    if (settings.jvmFlags == null) {
      settings.jvmFlags = new ArrayList<>();
    }
    settings.jvmFlags.add(flag);
  }

  @Override
  public String getCustomEntrypoint() {
    return null;
  }

  @Override
  public String getRuntime() {
    return null;
  }

  @Override
  public Boolean getAllowSkippedFiles() {
    return null;
  }

  @Override
  public Integer getApiPort() {
    return null;
  }

  @Override
  public Boolean getAutomaticRestart() {
    return null;
  }

  @Override
  public String getDevAppserverLogLevel() {
    return null;
  }

  @Override
  public Boolean getSkipSdkUpdateCheck() {
    return null;
  }

  @Override
  public String getDefaultGcsBucketName() {
    return null;
  }

  public static class AppEngineModelSettings {

    @Tag("artifact")
    private String artifact;

    @Tag("host")
    private String host;
    @Tag("port")
    private int port = 8080;
    @Tag("admin_host")
    private String adminHost;
    @Tag("admin_port")
    private String adminPort;
    @Tag("auth_domain")
    private String authDomain;
    @Tag("storage_path")
    private String storagePath;

    @Tag("jvmFlags")
    List<String> jvmFlags;

    @Tag("server-parameters")
    private String serverParameters = "";

    public void setPort(int port) {
      this.port = port;
    }

    public String getArtifact() {
      return artifact;
    }

    public void setArtifact(String artifact) {
      this.artifact = artifact;
    }

    public String getServerParameters() {
      return serverParameters;
    }

    public void setServerParameters(String serverParameters) {
      this.serverParameters = serverParameters;
    }

    public Integer getPort() {
      return port;
    }
  }
}
