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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;

import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for {@link AppEngineStandardDeployTask}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineStandardDeployTaskTest {

  private AppEngineStandardDeployTask task;
  @Mock AppEngineDeploy deploy;
  @Mock AppEngineStandardStage stage;
  @Mock DeploymentOperationCallback callback;
  @Mock AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock AppEngineHelper helper;
  @Mock ProcessStartListener startListener;

  private static final String DEPLOY_FAIL_MSG =
      "Deployment failed due to an unexpected error.\n"
          + "Please make sure that you are using the latest version of the Google Cloud SDK.\n"
          + "Run ''gcloud components update'' to update the SDK. "
          + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)";

  private static final String STAGE_FAIL_MSG =
      "Deployment failed due to an unexpected error while staging the project.\n"
          + "Please make sure that you are using the latest version of the Google Cloud SDK.\n"
          + "Run ''gcloud components update'' to update the SDK. "
          + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)";

  @Before
  public void setUp() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class), anyString()))
        .thenReturn(new File("myFile"));
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);

    task = new AppEngineStandardDeployTask(deploy, stage);
  }

  @Test
  public void createStagingDirectory_error() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class), anyString()))
        .thenThrow(new IOException());

    task.execute(startListener);
    verify(callback, times(1))
        .errorOccurred("There was an unexpected error creating the staging directory");
  }

  @Test
  public void stage_error() {
    doThrow(new RuntimeException())
        .when(stage)
        .stage(any(File.class), any(ProcessStartListener.class), any(ProcessExitListener.class));
    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1)).errorOccurred(STAGE_FAIL_MSG);
      return;
    }

    failureExpected();
  }

  @Test
  public void deploy_success() {
    task.deploy(new File("myFile.jar"), startListener).onExit(0);

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void deploy_error() {
    doThrow(new RuntimeException())
        .when(deploy).deploy(any(File.class), any(ProcessStartListener.class));
    try {
      task.deploy(new File("myFile.jar"), startListener).onExit(0);
    } catch (AssertionError ae) {
      verify(callback, times(1)).errorOccurred(DEPLOY_FAIL_MSG);
      return;
    }

    failureExpected();
  }

  private void failureExpected() {
    fail("Expected exception due to log error level");
  }
}
