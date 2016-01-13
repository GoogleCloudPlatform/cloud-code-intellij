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

import com.intellij.execution.process.ProcessHandler;

import java.io.OutputStream;

/**
 * The CloudDebugProcessHandler handles attach and detach actions. It also acts as the container of the process and
 * returns its output stream.
 */
public class CloudDebugProcessHandler extends ProcessHandler {
  private final CloudDebugProcess myProcess;

  public CloudDebugProcessHandler(CloudDebugProcess process) {
    myProcess = process;
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
    return myProcess;
  }

  @Override
  public OutputStream getProcessInput() {
    return null;
  }
}
