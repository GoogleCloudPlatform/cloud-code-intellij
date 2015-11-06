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
package com.google.gct.idea.appengine.deploy;

import com.google.common.base.Strings;
import com.google.gct.idea.appengine.gradle.GradleInvoker;
import com.google.gct.idea.appengine.sdk.AppEngineSdk;
import com.google.gct.login.stats.UsageTrackerService;
import com.google.gct.idea.util.GctTracking;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineBuilder;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Compiles and deploys a module to AppEngine using AppCfg.
 *
 * @author benwu
 */
class AppEngineUpdater {
  private static final Logger LOG = Logger.getInstance("#com.google.gct.idea.appengine.deploy.AppEngineUpdater");
  private final Project myProject;
  private final Module myModule;
  private final String myExplodedWarPath;
  private final String mySdkPath;
  private final String myClientSecret;
  private final String myClientId;
  private final String myRefreshToken;
  private final String myVersion;
  private final String myAppEngineProject;

  AppEngineUpdater(Project project,
                   Module module,
                   String sdkPath,
                   String explodedWarPath,
                   String appEngineProject,
                   String version,
                   String clientSecret,
                   String clientId,
                   String refreshToken) {
    myProject = project;
    myModule = module;
    mySdkPath = sdkPath;
    myExplodedWarPath = explodedWarPath;
    myClientSecret = clientSecret;
    myClientId = clientId;
    myRefreshToken = refreshToken;
    myVersion = version;
    myAppEngineProject = appEngineProject;
  }

  /**
   * Starts the compile and upload async process.
   */
  void startUploading() {
    FileDocumentManager.getInstance().saveAllDocuments();
    ProgressManager.getInstance().run(new Task.Backgroundable(myModule.getProject(), "Deploying application", true, null) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        compileAndUpload();
      }
    });
  }

  private void compileAndUpload() {
    GradleInvoker.executeTask(":assemble", myModule, new TaskCallback() {
      @Override
      public void onSuccess() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            startUploadingProcess();
          }
        });
      }

      @Override
      public void onFailure() {
        LOG.warn("unable to upload the module because it did not build successfully.");
      }
    }, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
  }

  private void startUploadingProcess() {
    final Process process;
    final GeneralCommandLine commandLine;

    try {
      JavaParameters parameters = new JavaParameters();
      parameters.configureByModule(myModule, JavaParameters.JDK_ONLY);
      parameters.setMainClass("com.google.appengine.tools.admin.AppCfg");
      AppEngineSdk mySdk = new AppEngineSdk(mySdkPath);
      if (mySdk.getToolsApiJarFile() == null) {
        Messages.showErrorDialog("Cannot start uploading: The tools sdk jar could not be located.", "Error");
        return;
      }
      parameters.getClassPath().add(mySdk.getToolsApiJarFile().getAbsolutePath());

      final List<KeyValue<String, String>> list = HttpConfigurable.getJvmPropertiesList(false, null);
      final ParametersList vmParametersList = parameters.getVMParametersList();
      if (!list.isEmpty()) {
        for (KeyValue<String, String> value : list) {
          vmParametersList.defineProperty(value.getKey(), value.getValue());
        }
      }
      // Set user-agent for appcfg
      vmParametersList.defineProperty(UserAgentStrings.TRACKING_KEY, UserAgentStrings.CT4AS);

      final ParametersList programParameters = parameters.getProgramParametersList();
      programParameters.add("--application=" + myAppEngineProject);
      if (!Strings.isNullOrEmpty(myVersion)) {
        programParameters.add("--version=" + myVersion);
      }
      programParameters.add("--oauth2");
      programParameters.add("--oauth2_client_secret=" + myClientSecret);
      programParameters.add("--oauth2_client_id=" + myClientId);
      programParameters.add("--oauth2_refresh_token=" + myRefreshToken);
      programParameters.add("update");
      programParameters.add(FileUtil.toSystemDependentName(myExplodedWarPath));

      commandLine = CommandLineBuilder.createFromJavaParameters(parameters);

      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      final String message = e.getMessage();
      LOG.error("Cannot start uploading: " + message);

      if (!EventQueue.isDispatchThread()) {
        EventQueue.invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog("Cannot start uploading: " + message, "Error");
          }
        });
      }
      else {
        Messages.showErrorDialog("Cannot start uploading: " + message, "Error");
      }

      return;
    }

    UsageTrackerService.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.DEPLOY, "upload.app", null);

    final ProcessHandler processHandler = new FilteredOSProcessHandler(process, commandLine.getCommandLineString(),
                                                                       new String[]{myRefreshToken, myClientSecret, myClientId});
    final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    final ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(myModule.getProject()).getConsole();
    final RunnerLayoutUi ui = RunnerLayoutUi.Factory.getInstance(myModule.getProject())
      .create("Deploy", "Deploy to AppEngine", "Deploy Application", myModule.getProject());
    final DefaultActionGroup group = new DefaultActionGroup();
    ui.getOptions().setLeftToolbar(group, ActionPlaces.UNKNOWN);
    ui.addContent(ui.createContent("upload", console.getComponent(), "Deploy Application", null, console.getPreferredFocusableComponent()));

    console.attachToProcess(processHandler);
    final RunContentDescriptor contentDescriptor =
      new RunContentDescriptor(console, processHandler, ui.getComponent(), "Deploy to AppEngine");
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM));
    group.add(new CloseAction(executor, contentDescriptor, myModule.getProject()));

    ExecutionManager.getInstance(myModule.getProject()).getContentManager().showRunContent(executor, contentDescriptor);
    processHandler.startNotify();
  }

  private class FilteredOSProcessHandler extends OSProcessHandler {
    String[] tokensToFilter;

    FilteredOSProcessHandler(@NotNull final Process process, @Nullable final String commandLine, String[] filteredTokens) {
      super(process, commandLine);
      tokensToFilter = filteredTokens;
    }

    @Override
    public void notifyTextAvailable(final String text, final Key outputType) {
      String newText = text;
      if (tokensToFilter != null) {
        for (String token : tokensToFilter) {
          newText = newText.replace(token, "*****");
        }
      }
      super.notifyTextAvailable(newText, outputType);
    }
  }
}
