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

package com.google.cloud.tools.intellij.appengine.run;

import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDevServer;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joaomartins on 7/21/16.
 */
public class CloudSdkProcessState implements RunProfileState {

  private ExecutionEnvironment environment;
  private CloudSdkOutputListener outputListener;
  private CloudSdk sdk;

  public CloudSdkProcessState(ExecutionEnvironment environment) {
    this.environment = environment;
    this.outputListener = new CloudSdkOutputListener(environment.getProject());
    this.sdk = new CloudSdk.Builder()
        .async(true)
        .addStdErrLineListener(outputListener)
        .addStdOutLineListener(outputListener)
        .build();
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner)
      throws ExecutionException {
    CloudSdkAppEngineDevServer devServer = new CloudSdkAppEngineDevServer(sdk);

    DefaultRunConfiguration sdkRunConfiguration = new DefaultRunConfiguration();
    List<File> appYamls = new ArrayList<>();
    Path appYamlPath = Paths.get(environment.getProject().getBasePath())
        .resolve("target/guestbook-1.0-SNAPSHOT/app.yaml");
    appYamls.add(appYamlPath.toFile());
    sdkRunConfiguration.setAppYamls(appYamls);
    sdkRunConfiguration.setPort(4577);

    ProcessHandler handler = new CloudSdkRunProcessHandler();

    devServer.run(sdkRunConfiguration);

    return new DefaultExecutionResult(outputListener.getConsoleView(), handler);
  }
}
