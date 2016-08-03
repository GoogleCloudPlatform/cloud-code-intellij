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

package com.intellij.appengine.server.run;

import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineExecutor;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineRunTask;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by joaomartins on 7/27/16.
 */
public class CloudSdkStartupPolicy implements ExecutableObjectStartupPolicy {

  // The startup process handler is kept so the process can be explicitly terminated, since we're
  // not delegating that to the framework.
  OSProcessHandler startupProcessHandler;

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
            return "App Engine Plugins Core library";
          }

          @Override
          public OSProcessHandler createProcessHandler(String s, Map<String, String> map)
              throws ExecutionException {
            DefaultRunConfiguration runConfig = new DefaultRunConfiguration();
            List<File> appYamls = new ArrayList<>();
            appYamls.add(new File("/usr/local/google/home/joaomartins/IdeaProjects/"
                + "JoaoDiscoProject/out/artifacts/main_war_exploded/app.yaml"));
            runConfig.setAppYamls(appYamls);

            List<String> jvmFlags = new ArrayList<>();
            jvmFlags.add(map.get("").trim());
            runConfig.setJvmFlags(jvmFlags);
            runConfig.setPort(4577);

            AppEngineRunTask runTask = new AppEngineRunTask(runConfig, null, null, null);
            AppEngineExecutor executor = new AppEngineExecutor(runTask);
            executor.run();

            startupProcessHandler = new OSProcessHandler(executor.getProcess());
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

          @Override
          public OSProcessHandler createProcessHandler(String s, Map<String, String> map)
              throws ExecutionException {
            startupProcessHandler.destroyProcess();

            ProcessBuilder dummyProcess = new ProcessBuilder("true");
            try {
              return new OSProcessHandler(dummyProcess.start());
            } catch (IOException ioe) {
              return null;
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
