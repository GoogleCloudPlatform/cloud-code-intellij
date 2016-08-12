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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.DebuggableRunConfiguration;

import java.net.InetSocketAddress;

/**
 * Created by joaomartins on 7/21/16.
 */
public class CloudSdkRunConfiguration extends RunConfigurationBase implements
    DebuggableRunConfiguration {

  public CloudSdkRunConfiguration(Project project, ConfigurationFactory factory) {
    super(project, factory, "name");
    setShowConsoleOnStdOut(true);
    setShowConsoleOnStdErr(true);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CloudSdkRunSettingsEditor();
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {

  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor,
      @NotNull ExecutionEnvironment environment) {
    return new CloudSdkProcessState(environment);
  }

  @NotNull
  @Override
  public InetSocketAddress computeDebugAddress() throws ExecutionException {
    return new InetSocketAddress("localhost", 4577);
  }

  @NotNull
  @Override
  public XDebugProcess createDebugProcess(@NotNull InetSocketAddress socketAddress,
      @NotNull XDebugSession session, @Nullable ExecutionResult executionResult,
      @NotNull ExecutionEnvironment environment) throws ExecutionException {
    return null;
  }
}
