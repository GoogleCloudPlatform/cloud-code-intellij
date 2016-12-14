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

package com.google.cloud.tools.intellij.appengine.cloud.executor;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploy;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleStage;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineHelper;

import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit tests for {@link AppEngineFlexibleDeployTask}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineFlexibleDeployTaskTest {

  private AppEngineFlexibleDeployTask task;
  @Mock
  AppEngineDeploy deploy;
  @Mock DeploymentOperationCallback callback;
  @Mock
  AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock
  AppEngineFlexibleStage stage;
  @Mock
  AppEngineHelper helper;
  @Mock ProcessStartListener startListener;

  @Before
  public void setUp() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class), anyString()))
        .thenReturn(Paths.get("myFile.jar"));
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(deploy.getHelper().stageCredentials(anyString())).thenReturn(Paths.get("/some/file"));

    task = new AppEngineFlexibleDeployTask(deploy, stage);
  }

  @Test
  public void testStageCredentials_error() {
    when(deploy.getHelper().stageCredentials(anyString())).thenReturn(null);
    task.execute(startListener);

    verify(callback, times(1))
        .errorOccurred("Failed to prepare credentials. Please make sure you are logged in with the correct account.");
  }

  @Test
  public void testCreateStagingDirectory_error() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class), anyString()))
        .thenThrow(new IOException());

    task.execute(startListener);
    verify(callback, times(1))
        .errorOccurred("There was an unexpected error creating the staging directory");
  }

  @Test
  public void stage_exception() {
    doThrow(new RuntimeException("myError")).when(stage).stage(Paths.get("myFile.jar"));
    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1))
          .errorOccurred("Deployment failed due to an exception while staging the project.");
      return;
    }

    failureExpected();
  }

  @Test
  public void deploy_success() {
    task.execute(startListener);

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void deploy_exception() {
    doThrow(new RuntimeException())
        .when(deploy)
        .deploy(any(Path.class), any(ProcessStartListener.class));

    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1))
          .errorOccurred("Deployment failed with an exception.\n"
          + "Please make sure that you are using the latest version of the Google Cloud SDK.\n"
          + "Run ''gcloud components update'' to update the SDK. "
          + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)");
      return;
    }

    failureExpected();
  }

  private void failureExpected() {
    fail("Expected exception due to log error level");
  }
}
