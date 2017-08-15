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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploy;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardStage;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineStandardDeployTask} */
@RunWith(JUnit4.class)
public final class AppEngineStandardDeployTaskTest {

  private static final String DEPLOY_EXCEPTION_MSG =
      "Deployment failed with an exception.\n"
          + "Please make sure that you are using the latest version of the Google Cloud SDK.\n"
          + "Run ''gcloud components update'' to update the SDK. "
          + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)";

  private static final String STAGE_EXCEPTION_MSG =
      "Deployment failed due to an exception while staging the project.\n"
          + "Please make sure that you are using the latest version of the Google Cloud SDK.\n"
          + "Run ''gcloud components update'' to update the SDK. "
          + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)";

  private static final String JAVA_COMPONENTS_MISSING_FAIL_MSG =
      CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT.getMessage();

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private AppEngineDeploy deploy;
  @Mock private AppEngineStandardStage stage;
  @Mock private DeploymentOperationCallback callback;
  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock private AppEngineHelper helper;
  @Mock private ProcessStartListener startListener;

  private AppEngineStandardDeployTask task;

  @Before
  public void setUp() throws IOException {
    when(helper.createStagingDirectory(any(), any())).thenReturn(Paths.get("myFile"));
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(deploy.getHelper().stageCredentials(any()))
        .thenReturn(Optional.of(Paths.get("/some/file")));

    task = new AppEngineStandardDeployTask(deploy, stage, false);
  }

  @Test
  public void testStageCredentials_error() {
    when(deploy.getHelper().stageCredentials(any())).thenReturn(null);
    task.execute(startListener);

    verify(callback, times(1))
        .errorOccurred(
            "Failed to prepare credentials. Please make sure you are logged in with the correct account.");
  }

  @Test
  public void createStagingDirectory_exception() throws IOException {
    when(helper.createStagingDirectory(any(), any())).thenThrow(new IOException());

    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1))
          .errorOccurred("There was an unexpected error creating the staging directory");
      return;
    }

    failureExpected();
  }

  @Test
  public void stage_runtime_exception() {
    doThrow(new RuntimeException()).when(stage).stage(any(), any(), any());
    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1)).errorOccurred(STAGE_EXCEPTION_MSG);
      return;
    }

    failureExpected();
  }

  @Test
  public void stage_missingJavaComponents_error() {
    doThrow(new AppEngineJavaComponentsNotInstalledException(""))
        .when(stage)
        .stage(any(), any(), any());

    task.execute(startListener);
    verify(callback, times(1)).errorOccurred(JAVA_COMPONENTS_MISSING_FAIL_MSG);
  }

  @Test
  public void deploy_success() {
    task.deploy(Paths.get("myFile.jar"), startListener).onExit(0);

    verify(callback, never()).errorOccurred(any());
  }

  @Test
  public void deploy_exception() {
    doThrow(new RuntimeException()).when(deploy).deploy(any(), any());
    try {
      task.deploy(Paths.get("myFile.jar"), startListener).onExit(0);
    } catch (AssertionError ae) {
      verify(callback, times(1)).errorOccurred(DEPLOY_EXCEPTION_MSG);
      return;
    }

    failureExpected();
  }

  private void failureExpected() {
    fail("Expected exception due to log error level");
  }
}
