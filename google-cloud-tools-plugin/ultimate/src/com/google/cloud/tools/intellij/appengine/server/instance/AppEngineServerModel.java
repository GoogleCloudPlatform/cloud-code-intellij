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
import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
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

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineServerModel implements ServerModel, DeploysArtifactsOnStartupOnly,
    RunConfiguration, Cloneable {

  private ArtifactPointer artifactPointer;
  private CommonModel commonModel;
  private Sdk devAppServerJdk;

  private AppEngineModelSettings settings = new AppEngineModelSettings();

  public AppEngineServerModel() {
    initDefaultJdk();
  }

  private void initDefaultJdk() {
    Project currentProject = ProjectManager.getInstance().getDefaultProject();
    Sdk projectJdk = ProjectRootManager.getInstance(currentProject).getProjectSdk();
    if (projectJdk != null) {
      setDevAppServerJdk(projectJdk);
    }
  }

  @Override
  public J2EEServerInstance createServerInstance() throws ExecutionException {
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
    String host = settings.getHost() != null && settings.getHost().compareTo("") != 0
        ? settings.getHost() : commonModel.getHost();
    return "http://" + host + ":" + settings.getPort();
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
    Artifact artifact;
    if (artifactPointer == null || (artifact = artifactPointer.getArtifact()) == null) {
      throw new RuntimeConfigurationError("Artifact isn't specified");
    }

    final AppEngineFacet facet = AppEngineUtil
        .findAppEngineFacet(commonModel.getProject(), artifact);
    if (facet == null) {
      throw new RuntimeConfigurationWarning(
          "App Engine facet not found in '" + artifact.getName() + "' artifact");
    }

    try {
      new CloudSdk.Builder()
          .sdkPath(CloudSdkService.getInstance().getSdkHomePath())
          .build()
          .validateAppEngineJavaComponents();
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.cloudsdk.java.components.missing"));
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

  /**
   * Only to be used in cloning.
   */
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

  public void setArtifact(@Nullable Artifact artifact) {
    if (artifact != null) {
      artifactPointer = ArtifactPointerManager.getInstance(commonModel.getProject())
          .createPointer(artifact);
      settings.setArtifact(artifact.getName());
    } else {
      artifactPointer = null;
    }
  }

  @Override
  public List<File> getAppYamls() {
    List<File> appYamls = new ArrayList<>();
    Path appYaml = Paths.get(artifactPointer.getArtifact().getOutputPath());
    appYamls.add(appYaml.toFile());
    return appYamls;
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
    return settings.getAdminHost();
  }

  public void setAdminHost(String adminHost) {
    settings.setAdminHost(adminHost);
  }

  @Override
  public Integer getAdminPort() {
    return settings.getAdminPort();
  }

  public void setAdminPort(Integer adminPort) {
    settings.setAdminPort(adminPort);
  }

  @Override
  public String getAuthDomain() {
    return settings.getAuthDomain();
  }

  public void setAuthDomain(String authDomain) {
    settings.setAuthDomain(authDomain);
  }

  @Override
  public String getStoragePath() {
    return settings.getStoragePath();
  }

  public void setStoragePath(String storagePath) {
    settings.setStoragePath(storagePath);
  }

  @Override
  public String getLogLevel() {
    return settings.getLogLevel();
  }

  public void setLogLevel(String logLevel) {
    settings.setLogLevel(logLevel);
  }

  @Override
  public Integer getMaxModuleInstances() {
    return settings.getMaxModuleInstances();
  }

  public void setMaxModuleInstances(Integer maxModuleInstances) {
    settings.setMaxModuleInstances(maxModuleInstances);
  }

  @Override
  public Boolean getUseMtimeFileWatcher() {
    return settings.isUseMtimeFileWatcher();
  }

  public void setUseMtimeFileWatcher(Boolean useMtimeFileWatcher) {
    settings.setUseMtimeFileWatcher(useMtimeFileWatcher);
  }

  @Override
  public String getThreadsafeOverride() {
    return settings.getThreadsafeOverride();
  }

  public void setThreadsafeOverride(String threadsafeOverride) {
    settings.setThreadsafeOverride(threadsafeOverride);
  }

  @Override
  public String getPythonStartupScript() {
    return settings.getPythonStartupScript();
  }

  public void setPythonStartupScript(String pythonStartupScript) {
    settings.setPythonStartupScript(pythonStartupScript);
  }

  @Override
  public String getPythonStartupArgs() {
    return settings.getPythonStartupArgs();
  }

  public void setPythonStartupArgs(String pythonStartupArgs) {
    settings.setPythonStartupArgs(pythonStartupArgs);
  }

  public static final String JVM_FLAG_DELIMITER = " ";

  @Override
  public List<String> getJvmFlags() {
    return settings.getJvmFlags() != null
        ? Splitter.on(JVM_FLAG_DELIMITER).splitToList(settings.getJvmFlags())
        : new ArrayList<String>();
  }

  public void addJvmFlag(String flag) {
    settings.setJvmFlags(settings.getJvmFlags() == null
        ? flag : settings.getJvmFlags() + JVM_FLAG_DELIMITER + flag);
  }

  public void addAllJvmFlags(Collection<String> flags) {
    settings.setJvmFlags(settings.getJvmFlags() == null
        ? Joiner.on(JVM_FLAG_DELIMITER).join(flags)
        : settings.getJvmFlags() + JVM_FLAG_DELIMITER + Joiner.on(JVM_FLAG_DELIMITER).join(flags));
  }

  public void setJvmFlags(String jvmFlags) {
    settings.setJvmFlags(jvmFlags);
  }

  @Override
  public String getCustomEntrypoint() {
    return settings.getCustomEntrypoint();
  }

  public void setCustomEntrypoint(String customEntrypoint) {
    settings.setCustomEntrypoint(customEntrypoint);
  }

  @Override
  public String getRuntime() {
    return settings.getRuntime();
  }

  public void setRuntime(String runtime) {
    settings.setRuntime(runtime);
  }

  @Override
  public Boolean getAllowSkippedFiles() {
    return settings.isAllowSkippedFiles();
  }

  public void setAllowSkippedFiles(Boolean allowSkippedFiles) {
    settings.setAllowSkippedFiles(allowSkippedFiles);
  }

  @Override
  public Integer getApiPort() {
    return settings.getApiPort();
  }

  public void setApiPort(Integer apiPort) {
    settings.setApiPort(apiPort);
  }

  @Override
  public Boolean getAutomaticRestart() {
    return settings.isAutomaticRestart();
  }

  public void setAutomaticRestart(Boolean automaticRestart) {
    settings.setAutomaticRestart(automaticRestart);
  }

  @Override
  public String getDevAppserverLogLevel() {
    return settings.getDevAppserverLogLevel();
  }

  public void setDevAppserverLogLevel(String devAppserverLogLevel) {
    settings.setDevAppserverLogLevel(devAppserverLogLevel);
  }

  @Override
  public Boolean getSkipSdkUpdateCheck() {
    return settings.isSkipSdkUpdateCheck();
  }

  public void setSkipSdkUpdateCheck(Boolean skipSdkUpdateCheck) {
    settings.setSkipSdkUpdateCheck(skipSdkUpdateCheck);
  }

  @Override
  public String getDefaultGcsBucketName() {
    return settings.getDefaultGcsBucketName();
  }

  public void setDefaultGcsBucketName(String defaultGcsBucketName) {
    settings.setDefaultGcsBucketName(defaultGcsBucketName);
  }

  @Override
  public String getJavaHomeDir() {
    return settings.getJavaHomeDir();
  }

  public Sdk getDevAppServerJdk() {
    return devAppServerJdk;
  }

  public void setDevAppServerJdk(Sdk devAppServerJdk) {
    this.devAppServerJdk = devAppServerJdk;
    settings.setJavaHomeDir(devAppServerJdk.getHomePath());
  }

  /**
   * This class is used to serialize run/debug config settings. It only supports basic types (e.g.,
   * int, String, etc.).
   *
   * <p>We use this class to store data and use {@link AppEngineServerModel} as an interface to get
   * that data. We need to interface some non-basic types (e.g., File, Path).
   * {@link AppEngineServerModel} translates stored data in its basic form to non-basic form.
   */
  private static class AppEngineModelSettings implements Cloneable {

    @Tag("artifact")
    private String artifact;

    @Tag("host")
    private String host;
    @Tag("port")
    private Integer port = 8080;
    @Tag("admin_host")
    private String adminHost;
    @Tag("admin_port")
    private Integer adminPort;
    @Tag("auth_domain")
    private String authDomain;
    @Tag("storage_path")
    private String storagePath;
    @Tag("log_level")
    private String logLevel;
    @Tag("max_module_instances")
    private Integer maxModuleInstances;
    @Tag("use_mtime_file_watcher")
    private boolean useMtimeFileWatcher;
    @Tag("threadsafe_override")
    private String threadsafeOverride;
    @Tag("python_startup_script")
    private String pythonStartupScript;
    @Tag("python_startup_args")
    private String pythonStartupArgs;
    @Tag("jvm_flags")
    private String jvmFlags;
    @Tag("custom_entrypoint")
    private String customEntrypoint;
    @Tag("runtime")
    private String runtime;
    @Tag("allow_skipped_files")
    private boolean allowSkippedFiles;
    @Tag("api_port")
    private Integer apiPort;
    @Tag("automatic_restart")
    private boolean automaticRestart;
    @Tag("devappserver_log_level")
    private String devAppserverLogLevel;
    @Tag("skip_sdk_update_check")
    private boolean skipSdkUpdateCheck;
    @Tag("default_gcs_bucket_name")
    private String defaultGcsBucketName;
    @Tag("java_home_directory")
    private String javaHomeDir;

    public String getArtifact() {
      return artifact;
    }

    public void setArtifact(String artifact) {
      this.artifact = artifact;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public String getAdminHost() {
      return adminHost;
    }

    public void setAdminHost(String adminHost) {
      this.adminHost = adminHost;
    }

    public Integer getAdminPort() {
      return adminPort;
    }

    public void setAdminPort(Integer adminPort) {
      this.adminPort = adminPort;
    }

    public String getAuthDomain() {
      return authDomain;
    }

    public void setAuthDomain(String authDomain) {
      this.authDomain = authDomain;
    }

    public String getStoragePath() {
      return storagePath;
    }

    public void setStoragePath(String storagePath) {
      this.storagePath = storagePath;
    }

    public String getLogLevel() {
      return logLevel;
    }

    public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }

    public Integer getMaxModuleInstances() {
      return maxModuleInstances;
    }

    public void setMaxModuleInstances(Integer maxModuleInstances) {
      this.maxModuleInstances = maxModuleInstances;
    }

    public boolean isUseMtimeFileWatcher() {
      return useMtimeFileWatcher;
    }

    public void setUseMtimeFileWatcher(boolean useMtimeFileWatcher) {
      this.useMtimeFileWatcher = useMtimeFileWatcher;
    }

    public String getThreadsafeOverride() {
      return threadsafeOverride;
    }

    public void setThreadsafeOverride(String threadsafeOverride) {
      this.threadsafeOverride = threadsafeOverride;
    }

    public String getPythonStartupScript() {
      return pythonStartupScript;
    }

    public void setPythonStartupScript(String pythonStartupScript) {
      this.pythonStartupScript = pythonStartupScript;
    }

    public String getPythonStartupArgs() {
      return pythonStartupArgs;
    }

    public void setPythonStartupArgs(String pythonStartupArgs) {
      this.pythonStartupArgs = pythonStartupArgs;
    }

    public String getJvmFlags() {
      return jvmFlags;
    }

    public void setJvmFlags(String jvmFlags) {
      this.jvmFlags = jvmFlags;
    }

    public String getCustomEntrypoint() {
      return customEntrypoint;
    }

    public void setCustomEntrypoint(String customEntrypoint) {
      this.customEntrypoint = customEntrypoint;
    }

    public String getRuntime() {
      return runtime;
    }

    public void setRuntime(String runtime) {
      this.runtime = runtime;
    }

    public boolean isAllowSkippedFiles() {
      return allowSkippedFiles;
    }

    public void setAllowSkippedFiles(boolean allowSkippedFiles) {
      this.allowSkippedFiles = allowSkippedFiles;
    }

    public Integer getApiPort() {
      return apiPort;
    }

    public void setApiPort(Integer apiPort) {
      this.apiPort = apiPort;
    }

    public boolean isAutomaticRestart() {
      return automaticRestart;
    }

    public void setAutomaticRestart(boolean automaticRestart) {
      this.automaticRestart = automaticRestart;
    }

    public String getDevAppserverLogLevel() {
      return devAppserverLogLevel;
    }

    public void setDevAppserverLogLevel(String devAppserverLogLevel) {
      this.devAppserverLogLevel = devAppserverLogLevel;
    }

    public boolean isSkipSdkUpdateCheck() {
      return skipSdkUpdateCheck;
    }

    public void setSkipSdkUpdateCheck(boolean skipSdkUpdateCheck) {
      this.skipSdkUpdateCheck = skipSdkUpdateCheck;
    }

    public String getDefaultGcsBucketName() {
      return defaultGcsBucketName;
    }

    public void setDefaultGcsBucketName(String defaultGcsBucketName) {
      this.defaultGcsBucketName = defaultGcsBucketName;
    }

    public String getJavaHomeDir() {
      return javaHomeDir;
    }

    public void setJavaHomeDir(String javaHomeDir) {
      this.javaHomeDir = javaHomeDir;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }
}
