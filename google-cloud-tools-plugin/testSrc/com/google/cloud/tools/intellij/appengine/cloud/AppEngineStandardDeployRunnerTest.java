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

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;

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
 * Unit tests for {@link AppEngineStandardDeployRunner}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineStandardDeployRunnerTest {

  private AppEngineStandardDeployRunner deployRunner;
  @Mock AppEngineDeploy deploy;
  @Mock AppEngineStandardStage stage;
  @Mock DeploymentOperationCallback callback;
  @Mock AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock AppEngineHelper helper;

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
    when(helper.createStagingDirectory(any(LoggingHandler.class))).thenReturn(new File("myFile"));
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);

    deployRunner = new AppEngineStandardDeployRunner(deploy, stage);
  }

  @Test
  public void createStagingDirectory_Error() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class)))
        .thenThrow(new IOException());
    try {
      deployRunner.run();
      failureExpected();
    } catch (Throwable t) {
      verify(callback, times(1)).errorOccurred(STAGE_FAIL_MSG);
    }
  }

  @Test
  public void stage_Error() {
    doThrow(new RuntimeException())
        .when(stage)
        .stage(any(File.class), any(ProcessStartListener.class), any(ProcessExitListener.class));
    try {
      deployRunner.run();
      failureExpected();
    } catch (Throwable t) {
      verify(callback, times(1)).errorOccurred(STAGE_FAIL_MSG);
    }
  }

  @Test
  public void deploy_Success() {
    deployRunner.deploy(new File("myFile.jar")).exit(0);

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void deploy_Error() {
    doThrow(new RuntimeException())
        .when(deploy).deploy(any(File.class), any(ProcessStartListener.class));
    try {
      deployRunner.deploy(new File("myFile.jar")).exit(0);
      failureExpected();
    } catch (Throwable t) {
      verify(callback, times(1)).errorOccurred(DEPLOY_FAIL_MSG);
    }
  }

  private void failureExpected() {
    fail("Expected throwable due to log error level");
  }
}
