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
import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.server.run.CloudSdkStartupPolicy;
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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
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

  public static final String JVM_FLAG_DELIMITER = " ";
  private ArtifactPointer artifactPointer;
  private CommonModel commonModel;
  private Sdk devAppServerJdk;
  private ProjectSdksModel projectJdksModel;
  private AppEngineModelSettings settings = new AppEngineModelSettings();

  public AppEngineServerModel() {
    projectJdksModel = new ProjectSdksModel();
  }

  @Nullable
  private Sdk getCurrentProjectJdk() {
    projectJdksModel.reset(commonModel.getProject());
    return projectJdksModel.getProjectSdk();
  }

  private void initDefaultJdk() {
    Sdk projectJdk = getCurrentProjectJdk();
    if (projectJdk != null) {
      setDevAppServerJdk(projectJdk);
    }
  }

  @Override
  public J2EEServerInstance createServerInstance() throws ExecutionException {
    // TODO(alexsloan): This keeps the dev_appserver's jdk in sync with the project jdk on behalf of
    // the user. This behavior should be removed once
    // https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/926 is completed.
    initDefaultJdk();

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

    final AppEngineStandardFacet facet = AppEngineUtil
        .findAppEngineFacet(commonModel.getProject(), artifact);
    if (facet == null) {
      throw new RuntimeConfigurationWarning(
          "App Engine facet not found in '" + artifact.getName() + "' artifact");
    }

    if (!CloudSdkService.getInstance().isValidCloudSdk()) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.run.server.sdk.misconfigured.panel.message"));
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

  /**
   * This value is being ignored for the run configuration.
   * {@link CloudSdkStartupPolicy}
   */
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

  @Override
  public List<String> getJvmFlags() {
    return settings.getJvmFlags() != null
        ? Splitter.on(JVM_FLAG_DELIMITER).splitToList(settings.getJvmFlags())
        : new ArrayList<String>();
  }

  public void addAllJvmFlags(Collection<String> flags) {
    settings.setJvmFlags(settings.getJvmFlags() == null
        ? Joiner.on(JVM_FLAG_DELIMITER).join(flags)
        : settings.getJvmFlags() + JVM_FLAG_DELIMITER + Joiner.on(JVM_FLAG_DELIMITER).join(flags));
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

  public Boolean getClearDatastore() {
    return settings.isClearDatastore();
  }

  public void setClearDatastore(Boolean clearDatastore) {
    settings.setClearDatastore(clearDatastore);
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
    private String host = "localhost";
    @Tag("port")
    private Integer port = 8080;
    @Tag("admin_host")
    private String adminHost = "localhost";
    @Tag("admin_port")
    private Integer adminPort = 8000;
    @Tag("auth_domain")
    private String authDomain;
    @Tag("storage_path")
    private String storagePath;
    @Tag("log_level")
    private String logLevel = "info";
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
    private Boolean automaticRestart = true;
    @Tag("clear_datastore")
    private Boolean clearDatastore = false;
    @Tag("devappserver_log_level")
    private String devAppserverLogLevel;
    @Tag("skip_sdk_update_check")
    private Boolean skipSdkUpdateCheck;
    @Tag("default_gcs_bucket_name")
    private String defaultGcsBucketName;
    private String javaHomeDir;

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

    String getAdminHost() {
      return adminHost;
    }

    void setAdminHost(String adminHost) {
      this.adminHost = adminHost;
    }

    Integer getAdminPort() {
      return adminPort;
    }

    void setAdminPort(Integer adminPort) {
      this.adminPort = adminPort;
    }

    String getAuthDomain() {
      return authDomain;
    }

    void setAuthDomain(String authDomain) {
      this.authDomain = authDomain;
    }

    String getStoragePath() {
      return storagePath;
    }

    void setStoragePath(String storagePath) {
      this.storagePath = storagePath;
    }

    String getLogLevel() {
      return logLevel;
    }

    void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }

    Integer getMaxModuleInstances() {
      return maxModuleInstances;
    }

    void setMaxModuleInstances(Integer maxModuleInstances) {
      this.maxModuleInstances = maxModuleInstances;
    }

    Boolean isUseMtimeFileWatcher() {
      return useMtimeFileWatcher;
    }

    void setUseMtimeFileWatcher(boolean useMtimeFileWatcher) {
      this.useMtimeFileWatcher = useMtimeFileWatcher;
    }

    String getThreadsafeOverride() {
      return threadsafeOverride;
    }

    void setThreadsafeOverride(String threadsafeOverride) {
      this.threadsafeOverride = threadsafeOverride;
    }

    String getPythonStartupScript() {
      return pythonStartupScript;
    }

    void setPythonStartupScript(String pythonStartupScript) {
      this.pythonStartupScript = pythonStartupScript;
    }

    String getPythonStartupArgs() {
      return pythonStartupArgs;
    }

    void setPythonStartupArgs(String pythonStartupArgs) {
      this.pythonStartupArgs = pythonStartupArgs;
    }

    String getJvmFlags() {
      return jvmFlags;
    }

    void setJvmFlags(String jvmFlags) {
      this.jvmFlags = jvmFlags;
    }

    String getCustomEntrypoint() {
      return customEntrypoint;
    }

    void setCustomEntrypoint(String customEntrypoint) {
      this.customEntrypoint = customEntrypoint;
    }

    String getRuntime() {
      return runtime;
    }

    void setRuntime(String runtime) {
      this.runtime = runtime;
    }

    Boolean isAllowSkippedFiles() {
      return allowSkippedFiles;
    }

    void setAllowSkippedFiles(boolean allowSkippedFiles) {
      this.allowSkippedFiles = allowSkippedFiles;
    }

    Integer getApiPort() {
      return apiPort;
    }

    void setApiPort(Integer apiPort) {
      this.apiPort = apiPort;
    }

    Boolean isAutomaticRestart() {
      return automaticRestart;
    }

    void setAutomaticRestart(boolean automaticRestart) {
      this.automaticRestart = automaticRestart;
    }

    String getDevAppserverLogLevel() {
      return devAppserverLogLevel;
    }

    void setDevAppserverLogLevel(String devAppserverLogLevel) {
      this.devAppserverLogLevel = devAppserverLogLevel;
    }

    Boolean isSkipSdkUpdateCheck() {
      return skipSdkUpdateCheck;
    }

    void setSkipSdkUpdateCheck(boolean skipSdkUpdateCheck) {
      this.skipSdkUpdateCheck = skipSdkUpdateCheck;
    }

    String getDefaultGcsBucketName() {
      return defaultGcsBucketName;
    }

    void setDefaultGcsBucketName(String defaultGcsBucketName) {
      this.defaultGcsBucketName = defaultGcsBucketName;
    }

    String getJavaHomeDir() {
      return javaHomeDir;
    }

    void setJavaHomeDir(String javaHomeDir) {
      this.javaHomeDir = javaHomeDir;
    }

    Boolean isClearDatastore() {
      return clearDatastore;
    }

    void setClearDatastore(boolean clearDatastore) {
      this.clearDatastore = clearDatastore;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }
}
