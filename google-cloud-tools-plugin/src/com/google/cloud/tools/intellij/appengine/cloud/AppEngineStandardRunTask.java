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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDevServer;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.Balloon.Position;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * Represents an App Engine Standard run task. (i.e., devappserver)
 */
public class AppEngineStandardRunTask extends AppEngineTask {

  private RunConfiguration runConfig;
  private Project project;

  public AppEngineStandardRunTask(@NotNull RunConfiguration runConfig, @NotNull Project project) {
    this.runConfig = runConfig;
    this.project = project;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    CloudSdkService sdkService = CloudSdkService.getInstance();
    if (sdkService.getSdkHomePath() == null || sdkService.getSdkHomePath().toString().isEmpty()) {
      return;
    }

    CloudSdk.Builder sdkBuilder = new CloudSdk.Builder()
        .sdkPath(sdkService.getSdkHomePath())
        .async(true)
        .startListener(startListener);

    CloudSdkAppEngineDevServer devServer = new CloudSdkAppEngineDevServer(sdkBuilder.build());
    devServer.run(runConfig);
  }
}
