/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.api.client.util.Lists;
import com.google.gct.idea.debugger.ui.CloudAttachDialog;
import com.google.gct.idea.ui.GoogleCloudToolsIcons;
import com.google.gct.idea.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunCanceledByUserException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.impl.RunnerContentUi;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import git4idea.DialogManager;

/**
 * The CloudDebuggerRunner shows the attach dialog, creates a cloud process representation and returns a content
 * descriptor once logically "attached".
 */
public class CloudDebuggerRunner extends DefaultProgramRunner {

  private static final String EXECUTOR_TARGET = "Debug";
  @NonNls
  private static final String ID = "CloudDebuggerRunner";

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return profile instanceof CloudDebugRunConfiguration && EXECUTOR_TARGET.equals(executorId);
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env)
      throws ExecutionException {

    ensureSingleDebugSession(env.getProject());

    CloudDebugProcessState cloudState = null;
    if (state instanceof CloudDebugProcessState) {
      cloudState = (CloudDebugProcessState) state;
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    state.execute(env.getExecutor(), this);

    return createContentDescriptor(cloudState, env);
  }

  @NotNull
  @Override
  public String getRunnerId() {
    return ID;
  }

  private static RunContentDescriptor createContentDescriptor(@Nullable final CloudDebugProcessState processState,
      @NotNull final ExecutionEnvironment environment) throws ExecutionException {

    final XDebugSession debugSession =
        XDebuggerManager.getInstance(environment.getProject()).startSession(environment, new XDebugProcessStarter() {
          @NotNull
          @Override
          public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {

            // Clear out the stash state which is queried on debug exit.
            if (processState != null) {
              ProjectRepositoryState.fromProcessState(processState).clearForNextSession();
            }

            CloudDebugProcessState state = processState;
            CloudAttachDialog attachDialog = new CloudAttachDialog(session.getProject(), null);
            attachDialog.setInputState(state);
            DialogManager.show(attachDialog);
            state = attachDialog.getResultState();

            ProjectRepositoryValidator validator = null;
            if (state != null) {
              validator = new ProjectRepositoryValidator(state);
            }
            if (!attachDialog.isOK() || state == null || !validator.isValidDebuggee()) {
              throw new RunCanceledByUserException();
            }

            RunnerAndConfigurationSettings runnerAndConfig = environment.getRunnerAndConfigurationSettings();
            if (runnerAndConfig != null &&
                runnerAndConfig.getConfiguration() instanceof CloudDebugRunConfiguration) {
              CloudDebugRunConfiguration config =
                  (CloudDebugRunConfiguration) runnerAndConfig.getConfiguration();
              // State is only stored in the run config between active sessions.
              // Otherwise, the background watcher may hit a check during debug session startup.
              config.setProcessState(null);
            }

            CloudDebugProcess process = new CloudDebugProcess(session);
            process.initialize(state);
            return process;
          }
        });

    RunnerLayoutUi ui = debugSession.getUI();
    if (ui instanceof DataProvider) {
      final RunnerContentUi contentUi = (RunnerContentUi) ((DataProvider) ui)
          .getData(RunnerContentUi.KEY.getName());
      final Project project = debugSession.getProject();

      if (contentUi != null) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            if (project.isOpen() && !project.isDisposed()) {
              contentUi.restoreLayout();
            }
          }
        });
      }
    }

    return debugSession.getRunContentDescriptor();
  }

  private void ensureSingleDebugSession(Project project) throws RunCanceledByUserException {

    List<CloudDebugProcessState> backgroundSessions = getBackgroundDebugStates(project);
    if (backgroundSessions.size() > 0) {
        for (CloudDebugProcessState cdps : backgroundSessions) {
          cdps.setListenInBackground(false);
        }
    }

    List<CloudDebugProcess> activeDebugProcesses = getActiveDebugProcesses(project);
    if (activeDebugProcesses.size() > 0) {
      int result = Messages.showOkCancelDialog(project,
          GctBundle.getString("clouddebug.stop.and.create.new.session"),
          GctBundle.getString("clouddebug.message.title"), GoogleCloudToolsIcons.CLOUD);
      if (result == Messages.OK) {
        for (CloudDebugProcess cdb : activeDebugProcesses) {
          cdb.getProcessHandler().detachProcess();
        }
      }
      else {
        throw new RunCanceledByUserException();
      }
    }
  }

  private List<CloudDebugProcessState> getBackgroundDebugStates(Project project) {

    List<CloudDebugProcessState> states = Lists.newArrayList();

    RunManager manager = RunManager.getInstance(project);
    for (final RunnerAndConfigurationSettings config : manager.getAllSettings()) {
      if (config.getConfiguration() instanceof CloudDebugRunConfiguration) {
        final CloudDebugRunConfiguration cloudConfig = (CloudDebugRunConfiguration)config.getConfiguration();
        CloudDebugProcessState processState = cloudConfig.getProcessState();
        if (processState != null && processState.isListenInBackground()) {
          states.add(processState);
        }
      }
    }
    return states;
  }

  private List<CloudDebugProcess> getActiveDebugProcesses(Project project) {

    List<CloudDebugProcess> processes = Lists.newArrayList();

    for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
      if (session.getDebugProcess() instanceof CloudDebugProcess) {
        processes.add((CloudDebugProcess) session.getDebugProcess());
      }
    }
    return processes;
  }
}
