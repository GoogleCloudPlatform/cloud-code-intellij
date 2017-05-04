/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.debugger;

import com.google.cloud.tools.intellij.debugger.ui.LogoutDebugProcessDetacher;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.istack.NotNull;

import java.io.OutputStream;

/**
 * The CloudDebugProcessHandler handles attach and detach actions. It also acts as the container of
 * the process and returns its output stream.
 */
public class CloudDebugProcessHandler extends ProcessHandler {

  private static final Logger LOG = Logger.getInstance(CloudDebugProcessHandler.class);

  private final CloudDebugProcess process;

  /**
   * Initialize the cloud debug process handler.
   */
  public CloudDebugProcessHandler(@NotNull CloudDebugProcess process) {
    this.process = process;
    if (process.getProcessState() != null) {
      String userEmail = process.getProcessState().getUserEmail();
      if (userEmail != null) {
        final CredentialedUser user = Services.getLoginService().getAllUsers().get(userEmail);
        if (user.getGoogleLoginState() != null) {
          user.getGoogleLoginState()
              .addLoginListener(new LogoutDebugProcessDetacher<CloudDebugProcessHandler>(this));
        } else {
          LOG.error(
              "GoogleLoginState is null. To launch a debug session user needs to be logged in");
        }
      } else {
        LOG.error("userEmail is null. To launch a debug session user needs to be logged in");
      }
    } else {
      LOG.error("process state is null.");
    }
  }

  @VisibleForTesting
  CloudDebugProcessHandler() {
    this.process = null;
  }

  @Override
  protected void destroyProcessImpl() {
    notifyProcessDetached();
  }

  @Override
  public boolean detachIsDefault() {
    return true;
  }

  @Override
  protected void detachProcessImpl() {
    notifyProcessDetached();
  }

  @Override
  public boolean isSilentlyDestroyOnClose() {
    return true;
  }

  public CloudDebugProcess getProcess() {
    return process;
  }

  @Override
  public OutputStream getProcessInput() {
    return null;
  }
}
