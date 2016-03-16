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

package com.google.gct.idea.debugger.ui;

import com.google.gdt.eclipse.login.common.LoginListener;

import com.intellij.execution.process.ProcessHandler;

/**
 * Detaches a Cloud Debug sessions represented by an instance of ProcessHandler on a logout event
 */
public class LogoutDebugProcessDetacher<H extends ProcessHandler> implements LoginListener {

  private H processHandler;

  public LogoutDebugProcessDetacher(H processHandler) {
    this.processHandler = processHandler;
  }

  @Override
  public void statusChanged(boolean loggedIn) {
    if (!loggedIn) {
      detachCloudDebugProcess();
    }
  }

  private void detachCloudDebugProcess() {
    processHandler.detachProcess();
  }
}
