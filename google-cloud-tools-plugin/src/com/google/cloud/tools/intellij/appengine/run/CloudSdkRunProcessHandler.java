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

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.devserver.DefaultStopConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDevServer;

import com.intellij.execution.process.ProcessHandler;

import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.net.ConnectException;

/**
 * Created by joaomartins on 7/22/16.
 */
public class CloudSdkRunProcessHandler extends ProcessHandler {

  @Override
  protected void destroyProcessImpl() {
    CloudSdk sdk = new CloudSdk.Builder().build();
    CloudSdkAppEngineDevServer devServer = new CloudSdkAppEngineDevServer(sdk);
    try {
      devServer.stop(new DefaultStopConfiguration());
    } catch (AppEngineException aee) {
      // In some rare cases, the start run/debug button causes the sdk to error out, but the IDE
      // can't see the error and thinks the process is running. When stop is hit, it won't be
      // possible to connect to devappserver, but we still want to terminate the process from the
      // IDE point of view.
      if (!(aee.getCause() instanceof ConnectException)) {
        throw aee;
      }
    }
    notifyProcessTerminated(0);
  }

  @Override
  protected void detachProcessImpl() {

  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Nullable
  @Override
  public OutputStream getProcessInput() {
    return null;
  }
}
