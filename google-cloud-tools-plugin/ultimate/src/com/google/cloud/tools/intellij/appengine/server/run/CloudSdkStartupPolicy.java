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

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineExecutor;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineStandardRunTask;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.server.instance.AppEngineServerModel;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.run.localRun.EnvironmentHelper;
import com.intellij.javaee.run.localRun.ExecutableObject;
import com.intellij.javaee.run.localRun.ExecutableObjectStartupPolicy;
import com.intellij.javaee.run.localRun.ScriptHelper;
import com.intellij.javaee.run.localRun.ScriptsHelper;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Runs a Google App Engine Standard app locally with devappserver, through the tools lib.
 */
public class CloudSdkStartupPolicy implements ExecutableObjectStartupPolicy {

  // The startup process handler is kept so the process can be explicitly terminated, since we're
  // not delegating that to the framework.
  private OSProcessHandler startupProcessHandler;

  @Nullable
  @Override
  public ScriptHelper createStartupScriptHelper(ProgramRunner programRunner) {
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
              String workingDirectory, Map<String, String> envVariables) throws ExecutionException {
            CloudSdkService sdkService = CloudSdkService.getInstance();
            if (sdkService.getSdkHomePath() == null
                || sdkService.getSdkHomePath().toString().isEmpty()) {
              throw new ExecutionException(
                  GctBundle.message("appengine.cloudsdk.location.missing.message"));
            }

            if (!sdkService.isValid()) {
              throw new ExecutionException(
                  GctBundle.message("appengine.deployment.error.invalid.cloudsdk"));
            }

            AppEngineServerModel runConfiguration;

            try {
              // Getting the clone so the debug flags aren't added to the persisted settings.
              runConfiguration = (AppEngineServerModel) commonModel.getServerModel().clone();
            } catch (CloneNotSupportedException ee) {
              throw new ExecutionException(ee);
            }

            // This is the place we have access to the debug jvm flags provided by IJ in the
            // Startup/Shutdown tab. We need to add it here.
            String jvmDebugFlag = envVariables.get("");
            if (jvmDebugFlag != null) {
              runConfiguration.addAllJvmFlags(Arrays.asList(jvmDebugFlag.trim().split(" ")));
            }

            AppEngineStandardRunTask runTask =
                new AppEngineStandardRunTask(runConfiguration);
            AppEngineExecutor executor = new AppEngineExecutor(runTask);
            executor.run();

            startupProcessHandler = new OSProcessHandler(executor.getProcess(),
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

            ProcessBuilder dummyProcess = new ProcessBuilder("true");
            try {
              return new OSProcessHandler(dummyProcess.start(),
                  GctBundle.getString("appengine.run.shutdownscript"));
            } catch (IOException ioe) {
              throw new ExecutionException(ioe);
            }
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
