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
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineStop;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineStopTask;

import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Unit tests for {@link AppEngineStopTask}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineStopTaskTest {

  private AppEngineStopTask task;
  @Mock
  AppEngineStop stop;
  @Mock
  AppEngineDeploymentConfiguration configuration;
  @Mock
  AppEngineHelper helper;
  @Mock UndeploymentTaskCallback callback;
  @Mock ProcessStartListener startListener;

  @Before
  public void setUp() {
    when(stop.getCallback()).thenReturn(callback);
    when(stop.getHelper()).thenReturn(helper);
    when(stop.getDeploymentConfiguration()).thenReturn(configuration);
    when(stop.getHelper().stageCredentials(anyString())).thenReturn(Optional.of(Paths.get("/some/file")));

    task = new AppEngineStopTask(stop, "myModule", "myVersion");
  }

  @Test
  public void testStageCredentials_error() {
    when(stop.getHelper().stageCredentials(anyString())).thenReturn(null);
    task.execute(startListener);

    verify(callback, times(1))
        .errorOccurred("Failed to prepare credentials. Please make sure you are logged in with the correct account.");
  }

  @Test
  public void testStop_success() {
    task.execute(startListener);

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void testStop_error() {
    doThrow(new RuntimeException("myError"))
        .when(stop)
        .stop(anyString(), anyString(), any(ProcessStartListener.class));
    try {
      task.execute(startListener);
    } catch (AssertionError ae) {
      verify(callback, times(1))
          .errorOccurred("Stop application failed due to an unexpected error.");
      return;
    }

    fail("Expected exception due to logging error level.");
  }

}
