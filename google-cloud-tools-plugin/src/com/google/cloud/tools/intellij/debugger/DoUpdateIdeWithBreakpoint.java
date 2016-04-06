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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;

import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;

import java.util.Map;

class DoUpdateIdeWithBreakpoint implements Runnable {

  private final XBreakpointManager manager;
  private final VirtualFile file;
  private final int line;
  private final CloudLineBreakpointProperties properties;
  private final Breakpoint serverBreakpoint;
  private Map<String, XBreakpoint> ideBreakpoints;
  private CloudDebugProcess debugProcess;

  public DoUpdateIdeWithBreakpoint(XBreakpointManager manager,
      VirtualFile file,
      int line,
      CloudLineBreakpointProperties properties,
      Breakpoint serverBreakpoint,
      Map<String, XBreakpoint> ideBreakpoints,
      CloudDebugProcess debugProcess) {
    this.manager = manager;
    this.file = file;
    this.line = line;
    this.properties = properties;
    this.serverBreakpoint = serverBreakpoint;
    this.ideBreakpoints = ideBreakpoints;
    this.debugProcess = debugProcess;
  }

  @Override
  public void run() {
    XLineBreakpoint<CloudLineBreakpointProperties> newXIdeBreakpoint =
        manager.addLineBreakpoint(
            CloudLineBreakpointType.getInstance(), file.getUrl(), line, properties);

    newXIdeBreakpoint.putUserData(CloudBreakpointHandler.CLOUD_ID, serverBreakpoint.getId());
    ideBreakpoints.put(serverBreakpoint.getId(), newXIdeBreakpoint);

    //condition, watches
    if (!Strings.isNullOrEmpty(serverBreakpoint.getCondition())) {
      newXIdeBreakpoint.setCondition(serverBreakpoint.getCondition());
    }

    if (serverBreakpoint.getExpressions() != null
        && serverBreakpoint.getExpressions().size() > 0) {
      newXIdeBreakpoint.getProperties().setWatchExpressions(
          serverBreakpoint.getExpressions().toArray(
              new String[serverBreakpoint.getExpressions().size()]));
    }

    // after this, changes in the UI will cause a re-register on the server.
    newXIdeBreakpoint.getProperties().setCreatedByServer(false);
    com.intellij.debugger.ui.breakpoints.Breakpoint cloudIdeBreakpoint =
        BreakpointManager.getJavaBreakpoint(newXIdeBreakpoint);
    if (cloudIdeBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
      CloudLineBreakpointType.CloudLineBreakpoint cloudIdeLineBreakpoint =
          (CloudLineBreakpointType.CloudLineBreakpoint) cloudIdeBreakpoint;
      cloudIdeLineBreakpoint.setVerified(true);
      cloudIdeLineBreakpoint.setErrorMessage(null);
      debugProcess.updateBreakpointPresentation(cloudIdeLineBreakpoint);
    }
  }
}
