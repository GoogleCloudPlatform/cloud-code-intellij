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

package com.google.cloud.tools.intellij.appengine.server.instance;

import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.javaee.deployment.DeploymentProvider;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.configuration.DeploysArtifactsOnStartupOnly;
import com.intellij.javaee.run.configuration.ServerModel;
import com.intellij.javaee.run.execution.DefaultOutputProcessor;
import com.intellij.javaee.run.execution.OutputProcessor;
import com.intellij.javaee.serverInstances.J2EEServerInstance;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.roots.ProjectRootManager;
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
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik */
public class AppEngineServerModel
    implements ServerModel, DeploysArtifactsOnStartupOnly, RunConfiguration, Cloneable {

  public static final String JVM_FLAG_DELIMITER = " ";
  private ArtifactPointer artifactPointer;
  private CommonModel commonModel;
  private AppEngineModelSettings settings = new AppEngineModelSettings();

  @Override
  public J2EEServerInstance createServerInstance() throws ExecutionException {
    if (ProjectRootManager.getInstance(commonModel.getProject()).getProjectSdk() == null) {
      throw new ExecutionException(GctBundle.getString("appengine.run.server.nosdk"));
    }

    return new AppEngineServerInstance(commonModel);
  }

  // TODO(joaomartins): Consider adding something here so we get a nice log pane
  // like Tomcat and Jetty.
  // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/836
  @Override
  public DeploymentProvider getDeploymentProvider() {
    return null;
  }

  @Override
  @NotNull
  public String getDefaultUrlForBrowser() {
    String host =
        settings.getHost() != null && settings.getHost().compareTo("") != 0
            ? settings.getHost()
            : commonModel.getHost();
    return "http://" + host + ":" + settings.getPort();
  }

  @Override
  public SettingsEditor<CommonModel> getEditor() {
    return new AppEngineRunConfigurationEditor(commonModel.getProject());
  }

  @Override
  public OutputProcessor createOutputProcessor(
      ProcessHandler processHandler, J2EEServerInstance serverInstance) {
    return new DefaultOutputProcessor(processHandler);
  }

  @Override
  public List<Pair<String, Integer>> getAddressesToCheck() {
    String host = settings.getHost() != null ? settings.getHost() : commonModel.getHost();
    return Collections.singletonList(Pair.create(host, settings.getPort()));
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
    if (artifactPointer == null || artifactPointer.getArtifact() == null) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.run.server.artifact.missing"));
    }

    if (!CloudSdkService.getInstance().isValidCloudSdk()) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.run.server.sdk.misconfigured.panel.message"));
    }

    if (ProjectRootManager.getInstance(commonModel.getProject()).getProjectSdk() == null) {
      throw new RuntimeConfigurationError(GctBundle.getString("appengine.run.server.nosdk"));
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
    AppEngineServerModel clone = (AppEngineServerModel) super.clone();
    clone.setSettings((AppEngineModelSettings) settings.clone());
    return clone;
  }

  /** Only to be used in cloning. */
  private void setSettings(AppEngineModelSettings settings) {
    this.settings = settings;
  }

  @Override
  public int getLocalPort() {
    return getPort();
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    XmlSerializer.deserializeInto(settings, element);
    final String artifactName = settings.getArtifact();
    if (artifactName != null) {
      artifactPointer =
          ArtifactPointerManager.getInstance(commonModel.getProject()).createPointer(artifactName);
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

  public void setArtifact(@Nullable Artifact artifact) {
    if (artifact != null) {
      artifactPointer =
          ArtifactPointerManager.getInstance(commonModel.getProject()).createPointer(artifact);
      settings.setArtifact(artifact.getName());
    } else {
      artifactPointer = null;
    }
  }

  @Override
  public List<File> getServices() {
    return ImmutableList.of(Paths.get(artifactPointer.getArtifact().getOutputPath()).toFile());
  }

  @Override
  public String getHost() {
    return settings.getHost();
  }

  public void setHost(String host) {
    settings.setHost(host);
  }

  @Override
  public Integer getPort() {
    return settings.getPort();
  }

  public void setPort(Integer port) {
    settings.setPort(port);
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
  public File getStoragePath() {
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
    return settings.getJvmFlags() != null
        ? Splitter.on(JVM_FLAG_DELIMITER).splitToList(settings.getJvmFlags())
        : new ArrayList<String>();
  }

  public void appendJvmFlags(Collection<String> flags) {
    settings.setJvmFlags(
        settings.getJvmFlags() == null
            ? Joiner.on(JVM_FLAG_DELIMITER).join(flags)
            : settings.getJvmFlags()
                + JVM_FLAG_DELIMITER
                + Joiner.on(JVM_FLAG_DELIMITER).join(flags));
  }

  @Override
  public String getRuntime() {
    return null;
  }

  @Override
  public String getCustomEntrypoint() {
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
    return settings.getDefaultGcsBucketName();
  }

  public void setDefaultGcsBucketName(String defaultGcsBucketName) {
    settings.setDefaultGcsBucketName(defaultGcsBucketName);
  }

  @Override
  public File getDatastorePath() {
    return null;
  }

  @Override
  public Map<String, String> getEnvironment() {
    return settings.getEnvironment();
  }

  @Override
  public List<String> getAdditionalArguments() {
    return ImmutableList.of();
  }

  public void setEnvironment(Map<String, String> environment) {
    settings.setEnvironment(environment);
  }

  @Override
  public Boolean getClearDatastore() {
    return null;
  }

  /**
   * This class is used to serialize run/debug config settings. It only supports basic types (e.g.,
   * int, String, etc.).
   *
   * <p>We use this class to store data and use {@link AppEngineServerModel} as an interface to get
   * that data. We need to interface some non-basic types (e.g., File, Path). {@link
   * AppEngineServerModel} translates stored data in its basic form to non-basic form.
   */
  private static class AppEngineModelSettings implements Cloneable {

    @Tag("artifact")
    private String artifact;

    @Tag("host")
    private String host = "localhost";

    @Tag("port")
    private Integer port = 8080;

    @Tag("jvm_flags")
    private String jvmFlags;

    @Tag("environment")
    private Map<String, String> environment;

    @Tag("default_gcs_bucket_name")
    private String defaultGcsBucketName;

    String getArtifact() {
      return artifact;
    }

    void setArtifact(String artifact) {
      this.artifact = artifact;
    }

    String getHost() {
      return host;
    }

    void setHost(String host) {
      this.host = host;
    }

    Integer getPort() {
      return port;
    }

    void setPort(Integer port) {
      this.port = port;
    }

    String getJvmFlags() {
      return jvmFlags;
    }

    void setJvmFlags(String jvmFlags) {
      this.jvmFlags = jvmFlags;
    }

    String getDefaultGcsBucketName() {
      return defaultGcsBucketName;
    }

    public Map<String, String> getEnvironment() {
      return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
      this.environment = environment;
    }

    void setDefaultGcsBucketName(String defaultGcsBucketName) {
      this.defaultGcsBucketName = defaultGcsBucketName;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }
}
