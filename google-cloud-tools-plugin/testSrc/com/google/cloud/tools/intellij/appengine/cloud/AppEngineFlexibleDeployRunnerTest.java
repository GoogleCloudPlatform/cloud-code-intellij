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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploy.AppEngineDeployException;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineFlexibleStage.AppEngineFlexibleStageException;

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
 * Unit tests for {@link AppEngineFlexibleDeployRunner}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineFlexibleDeployRunnerTest {

  private AppEngineFlexibleDeployRunner deployRunner;
  @Mock AppEngineDeploy deploy;
  @Mock DeploymentOperationCallback callback;
  @Mock AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock AppEngineFlexibleStage stage;
  @Mock AppEngineHelper helper;

  private static final String STAGE_DIR_ERROR
      = "There was an unexpected error creating the staging directory";

  @Before
  public void setUp() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class))).thenReturn(new File("myFile.jar"));
    when(deploy.getHelper()).thenReturn(helper);
    when(deploy.getCallback()).thenReturn(callback);
    when(deploy.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);

    deployRunner = new AppEngineFlexibleDeployRunner(deploy, stage);
  }

  @Test
  public void testCreateStagingDirectory_Error() throws IOException {
    when(helper.createStagingDirectory(any(LoggingHandler.class))).thenThrow(new IOException());
    deployRunner.run();

    verify(callback, times(1)).errorOccurred(STAGE_DIR_ERROR);
  }

  @Test
  public void stage_Error() {
    doThrow(new AppEngineFlexibleStageException("myError")).when(stage).stage(new File("myFile.jar"));
    deployRunner.run();

    verify(callback, times(1)).errorOccurred("myError");
  }

  @Test
  public void deploy_Success() {
    deployRunner.run();

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void deploy_Error() {
    doThrow(new AppEngineDeployException("myError")).when(deploy).deploy(new File("myFile.jar"));
    deployRunner.run();

    verify(callback, times(1)).errorOccurred("myError");
  }

}
