/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.run;

import com.google.gct.idea.stats.UsageTracker;
import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.appengine.sdk.AppEngineSdk;

import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathsList;

import org.jdom.Element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/** A run configuration for App Engine modules, calls DevAppServerMain directly from the sdk bypassing KickStart*/
public class AppEngineRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {

  public static final String NAME = "App Engine DevAppServer";

  private String myServerAddress = "";
  private String mySdkPath = "";
  private String myServerPort = "";
  private Boolean mySyncWithGradle = false;
  private String myVmArgs = "";
  private String myWarPath = "";
  private Boolean myDisableUpdateCheck = false;

  private static final String KEY_SERVER_ADDRESS = "serverAddress";
  private static final String KEY_SERVER_PORT = "serverPort";
  private static final String KEY_SYNC = "sync";
  private static final String KEY_SDK_PATH = "sdkPath";
  private static final String KEY_VM_ARGS = "vmArgs";
  private static final String KEY_WAR_PATH = "warPath";
  private static final String KEY_DISABLE_UPDATE_CHECK = "disableUpdateCheck";

  public String getWarPath() {
    return myWarPath;
  }

  public void setWarPath(String warPath) {
    this.myWarPath = warPath;
  }

  public String getServerAddress() {
    return myServerAddress;
  }

  public void setServerAddress(String serverAddress) {
    myServerAddress = serverAddress;
  }

  public String getSdkPath() {
    return mySdkPath;
  }

  public void setSdkPath(String sdkPath) {
    this.mySdkPath = sdkPath;
  }

  public String getServerPort() {
    return myServerPort;
  }

  public void setServerPort(String serverPort) {
    this.myServerPort = serverPort;
  }

  public Boolean isSyncWithGradle() {
    return mySyncWithGradle;
  }

  public void setSyncWithGradle(Boolean syncWithGradle) {
    mySyncWithGradle = syncWithGradle;
  }

  public String getVmArgs() {
    return myVmArgs;
  }

  public void setVmArgs(String vmArgs) {
    this.myVmArgs = vmArgs;
  }


  public Boolean isDisableUpdateCheck() {
    return myDisableUpdateCheck;
  }

  public void setDisableUpdateCheck(Boolean disableUpdateCheck) {
    myDisableUpdateCheck = disableUpdateCheck;
  }


  public AppEngineRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, new JavaRunConfigurationModule(project, false), factory);
  }

  @Override
  public Collection<Module> getValidModules() {
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();

    /* comment out for now since we don't have facet
    ArrayList<Module> res = new ArrayList<Module>();
    for (Module module : modules) {
      Facet[] facetList = FacetManager.getInstance(module).getAllFacets();
      for (Facet f : facetList) {
        if (f.getTypeId() == AppEngineGradleFacet.TYPE_ID) {
          res.add(module);
          break;
        }
      }
    }*/
    return Arrays.asList(modules);
  }

  @Override
  protected ModuleBasedConfiguration createInstance() {
    return new AppEngineRunConfiguration(getName(), getProject(), getFactory());
  }

  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new AppEngineRunConfigurationSettingsEditor(getProject());
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    final JavaCommandLineState state = new JavaApplicationCommandLineState(this, env);
    JavaRunConfigurationModule module = getConfigurationModule();
    state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), module.getSearchScope()));
    return state;
  }

  // Syncs a run configuration with information from build.gradle via the App Engine Gradle facet
  // See duplicate in AppEngineRunConfigurationSettingsEditor#syncWithBuildFileViaFacet
  protected void syncWithBuildFileViaFacet() {
    // clear it out first
    myServerPort = myServerAddress = mySdkPath = myWarPath = myVmArgs = "";
    myDisableUpdateCheck = false;

    Module module = getConfigurationModule().getModule();
    if (module != null) {
      AppEngineGradleFacet facet = AppEngineGradleFacet.getInstance(module);
      if (facet != null) {
        AppEngineConfigurationProperties model = facet.getConfiguration().getState();
        if (model != null) {
          myServerPort = model.HTTP_PORT.toString();
          myServerAddress = model.HTTP_ADDRESS;
          mySdkPath = model.APPENGINE_SDKROOT;
          myWarPath = model.WAR_DIR;
          myVmArgs = model.getJvmFlags();
          myDisableUpdateCheck = model.DISABLE_UPDATE_CHECK;
        }
      }
    }
  }

  @Override
  public final void checkConfiguration() throws RuntimeConfigurationException {
    JavaRunConfigurationModule configurationModule = getConfigurationModule();
    configurationModule.checkForWarning();
    Module module = configurationModule.getModule();

    if (module == null) {
      return;
    }
    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
    if (facet == null) {
      throw new RuntimeConfigurationError(GctBundle.message("appengine.run.no.facet"));
    }

    if (mySyncWithGradle) {
      if (facet.getConfiguration().getState() == null || StringUtil.isEmpty(facet.getConfiguration().getState().WEB_APP_DIR)) {
        throw new RuntimeConfigurationError(GctBundle.message("appengine.run.no.gradle.state"));
      }
    }

    if (mySyncWithGradle) {
      syncWithBuildFileViaFacet();
    }

    if (mySdkPath == null || mySdkPath.trim().isEmpty() || !new AppEngineSdk(mySdkPath).canRunDevAppServer()) {
      throw new RuntimeConfigurationError(GctBundle.message("appengine.run.invalid.sdk"));
    }

    if (myWarPath == null || myWarPath.trim().isEmpty()) {
      throw new RuntimeConfigurationError(GctBundle.message("appengine.run.invalid.war"));
    }

    if (StringUtil.containsWhitespaces(myServerAddress.trim())) {
      throw new RuntimeConfigurationError(GctBundle.message("appengine.run.invalid.server.address"));
    }

    if (!(myServerPort == null || myServerPort.trim().isEmpty())) {
      try {
        int value = Integer.parseInt(myServerPort);
        if (value < 1024 || value >  65535) {
          throw new RuntimeConfigurationError(GctBundle.message("appengine.run.invalid.server.port.range", 1024, 65535));
        }
      } catch (NumberFormatException nfe) {
        throw new RuntimeConfigurationError(GctBundle.message("appengine.run.invalid.server.port", 1024, 65535));
      }
    }
  }

  /** Class to configure command line state of the dev app server **/
  public static class JavaApplicationCommandLineState extends JavaCommandLineState {

    private final AppEngineRunConfiguration configuration;

    public JavaApplicationCommandLineState(@NotNull final AppEngineRunConfiguration configuration,
                                           final ExecutionEnvironment environment) {
      super(environment);
      this.configuration = configuration;
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
      final JavaParameters params = new JavaParameters();
      final JavaRunConfigurationModule module = configuration.getConfigurationModule();

      params.setMainClass(AppEngineSdk.DEV_APPSERVER_CLASS);

      AppEngineSdk sdk = new AppEngineSdk(configuration.mySdkPath);

      ParametersList vmParams = params.getVMParametersList();
      if (configuration.myVmArgs != null && !configuration.myVmArgs.trim().isEmpty()) {
        vmParams.addParametersString(configuration.myVmArgs);
      }
      sdk.addServerVmParams(vmParams);

      ParametersList programParams = params.getProgramParametersList();
      if (configuration.myServerAddress != null && !configuration.myServerAddress.trim().isEmpty()) {
        programParams.add("--address=" + configuration.myServerAddress);
      }
      if (configuration.myServerPort != null && !configuration.myServerPort.trim().isEmpty()) {
        programParams.add("--port=" + configuration.myServerPort);
      }
      if (configuration.myDisableUpdateCheck) {
        programParams.add("--disable_update_check");
      }

      String warPath = configuration.myWarPath;
      if (warPath == null) {
        throw new ExecutionException("War path is invalid");
      }
      programParams.add(warPath);

      params.setWorkingDirectory(warPath);
      PathsList classPath = params.getClassPath();

      classPath.add(sdk.getToolsApiJarFile().getAbsolutePath());

      Module appEngineModule = module.getModule();
      if (appEngineModule == null) {
        throw new ExecutionException("Module not defined");
      }

      //TODO : allow selectable alternate jre
      params.setJdk(JavaParameters.getModuleJdk(appEngineModule));

      UsageTracker.getInstance()
        .trackEvent(GctTracking.CATEGORY, GctTracking.RUN, configuration.isSyncWithGradle() ? "sync" : "custom", null);
      return params;
    }
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    PathMacroManager.getInstance(getProject()).expandPaths(element);
    super.readExternal(element);
    readModule(element);
    myServerAddress = StringUtil.notNullize(JDOMExternalizer.readString(element, KEY_SERVER_ADDRESS));
    mySdkPath = ExternalizablePath.localPathValue(JDOMExternalizer.readString(element, KEY_SDK_PATH));
    myServerPort = StringUtil.notNullize(JDOMExternalizer.readString(element, KEY_SERVER_PORT));
    myVmArgs = StringUtil.notNullize(JDOMExternalizer.readString(element, KEY_VM_ARGS));
    myWarPath = StringUtil.notNullize(JDOMExternalizer.readString(element, KEY_WAR_PATH));
    mySyncWithGradle = JDOMExternalizer.readBoolean(element, KEY_SYNC);
    myDisableUpdateCheck = JDOMExternalizer.readBoolean(element, KEY_DISABLE_UPDATE_CHECK);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    writeModule(element);
    JDOMExternalizer.write(element, KEY_SERVER_ADDRESS, myServerAddress);
    JDOMExternalizer.write(element, KEY_SDK_PATH, ExternalizablePath.urlValue(mySdkPath));
    JDOMExternalizer.write(element, KEY_SERVER_PORT, myServerPort);
    JDOMExternalizer.write(element, KEY_VM_ARGS, myVmArgs);
    JDOMExternalizer.write(element, KEY_WAR_PATH, myWarPath);
    JDOMExternalizer.write(element, KEY_SYNC, mySyncWithGradle);
    JDOMExternalizer.write(element, KEY_DISABLE_UPDATE_CHECK, myDisableUpdateCheck);
    PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
  }
}
