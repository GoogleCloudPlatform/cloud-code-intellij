/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.cloud.executor;

import static com.google.cloud.tools.intellij.testing.TestUtils.expectThrows;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploy;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.java.cloud.flexible.AppEngineFlexibleStage;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineFlexibleDeployTask} */
@RunWith(JUnit4.class)
public class AppEngineFlexibleDeployTaskTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private AppEngineDeploy deploy;
  @Mock private DeploymentOperationCallback callback;
  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock private AppEngineFlexibleStage stage;
  @Mock private AppEngineHelper helper;
  @Mock private ProcessStartListener startListener;

  private AppEngineFlexibleDeployTask task;

  @Before
  public void setUp() throws IOException {
    when(helper.createStagingDirectory(any(), any())).thenReturn(Paths.get("myFile.jar"));
    when(stage.stage(Paths.get("myFile.jar"))).thenReturn(true);
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(deploy.getHelper().stageCredentials(any()))
        .thenReturn(Optional.of(Paths.get("/some/file")));

    task = new AppEngineFlexibleDeployTask(deploy, stage);
  }

  @Test
  public void testStageCredentials_error() {
    when(deploy.getHelper().stageCredentials(any())).thenReturn(null);
    task.execute(startListener);

    verify(callback, times(1))
        .errorOccurred(
            "Failed to prepare credentials. Please make sure you are logged in with the correct "
                + "account.");
  }

  @Test
  public void testCreateStagingDirectory_error() throws IOException {
    when(helper.createStagingDirectory(any(), any())).thenThrow(new IOException());

    task.execute(startListener);
    verify(callback, times(1))
        .errorOccurred("There was an unexpected error creating the staging directory");
  }

  @Test
  public void stage_exception() throws IOException {
    when(stage.stage(Paths.get("myFile.jar"))).thenReturn(false);

    task.execute(startListener);

    verify(callback, times(1))
        .errorOccurred("Deployment failed due to an exception while staging the project.");
  }

  @Test
  public void stage_withIOException_throwsAssertionError() throws IOException {
    String exceptionMessage = "some exception";
    when(stage.stage(Paths.get("myFile.jar"))).thenThrow(new IOException(exceptionMessage));

    AssertionError e = expectThrows(AssertionError.class, () -> task.execute(startListener));

    assertThat(e).hasMessage(exceptionMessage);
    verify(callback)
        .errorOccurred("Deployment failed due to an exception while staging the project.");
  }

  @Test
  public void deploy_success() {
    task.execute(startListener);

    verify(callback, never()).errorOccurred(any());
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
          .errorOccurred(
              "Deployment failed with an exception.\n"
                  + "Please make sure that you are using the latest version of the Google Cloud "
                  + "SDK.\nRun ''gcloud components update'' to update the SDK. "
                  + "(See: https://cloud.google.com/sdk/gcloud/reference/components/update.)");
      return;
    }

    fail("Expected exception due to log error level");
  }
}
