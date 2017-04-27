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

package com.google.cloud.tools.intellij.appengine.server.run;

import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineExecutor;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineStandardRunTask;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.server.instance.AppEngineServerModel;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.collect.Maps;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.localRun.EnvironmentHelper;
import com.intellij.javaee.run.localRun.ExecutableObject;
import com.intellij.javaee.run.localRun.ExecutableObjectStartupPolicy;
import com.intellij.javaee.run.localRun.ScriptHelper;
import com.intellij.javaee.run.localRun.ScriptsHelper;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

/**
 * Runs a Google App Engine Standard app locally with devappserver, through the tools lib.
 */
public class CloudSdkStartupPolicy implements ExecutableObjectStartupPolicy {

  // The startup process handler is kept so the process can be explicitly terminated, since we're
  // not delegating that to the framework.
  private OSProcessHandler startupProcessHandler;
  private static final String JVM_DEBUG_FLAGS_ENVIRONMENT_KEY = "";

  @Nullable
  @Override
  public ScriptHelper createStartupScriptHelper(final ProgramRunner programRunner) {
    return new ScriptHelper() {
      @Nullable
      @Override
      public ExecutableObject getDefaultScript(final CommonModel commonModel) {
        return new ExecutableObject() {
          @Override
          public String getDisplayString() {
            return GctBundle.getString("appengine.run.startupscript.name");
          }

          @Override
          public OSProcessHandler createProcessHandler(
              String workingDirectory, Map<String, String> configuredEnvironment) throws ExecutionException {

            if (!CloudSdkService.getInstance().isValidCloudSdk()) {
              throw new ExecutionException(
                  GctBundle.message("appengine.run.server.sdk.misconfigured.message"));
            }

            Sdk javaSdk = ProjectRootManager.getInstance(commonModel.getProject()).getProjectSdk();
            if (javaSdk == null || javaSdk.getHomePath() == null) {
              throw new ExecutionException(GctBundle.message("appengine.run.server.nosdk"));
            }

            AppEngineServerModel runConfiguration;

            try {
              // Getting the clone so the debug flags aren't added to the persisted settings.
              runConfiguration = (AppEngineServerModel) commonModel.getServerModel().clone();
            } catch (CloneNotSupportedException ee) {
              throw new ExecutionException(ee);
            }

            Map<String, String> environment = Maps.newHashMap(configuredEnvironment);

            // This is the place we have access to the debug jvm flags provided by IJ in the
            // Startup/Shutdown tab. We need to add them here.
            String jvmDebugFlag = environment.get(JVM_DEBUG_FLAGS_ENVIRONMENT_KEY);
            if (jvmDebugFlag != null) {
              runConfiguration.setJvmFlags(Arrays.asList(jvmDebugFlag.trim().split(" ")));
            }
            // We don't want to pass the jvm flags to the dev server environment
            environment.remove(JVM_DEBUG_FLAGS_ENVIRONMENT_KEY);

            runConfiguration.setEnvironment(environment);

            AppEngineStandardRunTask runTask =
                new AppEngineStandardRunTask(
                    runConfiguration, javaSdk, programRunner.getRunnerId());
            AppEngineExecutor executor = new AppEngineExecutor(runTask);
            executor.run();

            Process devappserverProcess = executor.getProcess();
            startupProcessHandler = new OSProcessHandler(devappserverProcess,
                GctBundle.getString("appengine.run.startupscript"));
            return startupProcessHandler;
          }
        };
      }
    };
  }

  @Nullable
  @Override
  public ScriptHelper createShutdownScriptHelper(ProgramRunner programRunner) {
    return new ScriptHelper() {
      @Nullable
      @Override
      public ExecutableObject getDefaultScript(CommonModel commonModel) {
        return new ExecutableObject() {
          @Override
          public String getDisplayString() {
            return "";
          }

          /**
           * We declare a dummy script here so there is no warning in the IJ Run/Debug configuration
           * window about "missing shutdown script".
           */
          @Override
          public OSProcessHandler createProcessHandler(
              String workingDirectory, Map<String, String> envVariables) throws ExecutionException {
            startupProcessHandler.destroyProcess();
            return null;
          }
        };
      }
    };
  }

  @Nullable
  @Override
  public EnvironmentHelper getEnvironmentHelper() {
    return null;
  }

  @Nullable
  @Override
  public ScriptsHelper getStartupHelper() {
    return null;
  }

  @Nullable
  @Override
  public ScriptsHelper getShutdownHelper() {
    return null;
  }
}
