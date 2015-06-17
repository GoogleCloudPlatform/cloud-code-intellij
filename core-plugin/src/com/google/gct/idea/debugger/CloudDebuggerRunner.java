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

import com.google.gct.idea.debugger.ui.CloudAttachDialog;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunCanceledByUserException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import git4idea.DialogManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    CloudDebugProcessState cloudState = null;
    if (state instanceof CloudDebugProcessState) {
      cloudState = (CloudDebugProcessState)state;
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
                                                              @NotNull final ExecutionEnvironment environment)
    throws ExecutionException {
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
          CloudAttachDialog attachDialog = new CloudAttachDialog(session.getProject());
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

          if (environment.getRunnerAndConfigurationSettings() != null &&
              environment.getRunnerAndConfigurationSettings()
                .getConfiguration() instanceof CloudDebugRunConfiguration) {
            CloudDebugRunConfiguration config =
              (CloudDebugRunConfiguration)environment.getRunnerAndConfigurationSettings().getConfiguration();
            // State is only stored in the run config between active sessions.
            // Otherwise, the background watcher may hit a check during debug session startup.
            config.setProcessState(null);
          }

          CloudDebugProcess process = new CloudDebugProcess(session);
          process.initialize(state);
          return process;
        }
      });
    return debugSession.getRunContentDescriptor();
  }
}
