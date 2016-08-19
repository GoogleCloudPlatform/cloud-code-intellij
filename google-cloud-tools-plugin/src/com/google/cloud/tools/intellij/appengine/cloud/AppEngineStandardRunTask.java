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

import org.jetbrains.annotations.NotNull;

/**
 * Represents an App Engine Standard run task. (i.e., devappserver)
 */
public class AppEngineStandardRunTask extends AppEngineTask {

  private RunConfiguration runConfig;

  public AppEngineStandardRunTask(@NotNull RunConfiguration runConfig) {
    this.runConfig = runConfig;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    CloudSdk.Builder sdkBuilder = new CloudSdk.Builder()
        .sdkPath(CloudSdkService.getInstance().getCloudSdkHomePath())
        .async(true)
        .startListener(startListener);

    CloudSdkAppEngineDevServer devServer = new CloudSdkAppEngineDevServer(sdkBuilder.build());
    devServer.run(runConfig);
  }
}
