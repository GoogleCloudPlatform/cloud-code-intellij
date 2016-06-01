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

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;

import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link AppEngineStopRunner}
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineStopRunnerTest {

  private AppEngineStopRunner stopRunner;
  @Mock AppEngineStop stop;
  @Mock UndeploymentTaskCallback callback;

  @Before
  public void setUp() {
    when(stop.getCallback()).thenReturn(callback);

    stopRunner = new AppEngineStopRunner(stop, "myModule", "myVersion");
  }

  @Test
  public void testStop_Success() {
    stopRunner.run();

    verify(callback, never()).errorOccurred(anyString());
  }

  @Test
  public void testStop_Error() {
    doThrow(new RuntimeException("myError"))
        .when(stop)
        .stop(anyString(), anyString(), any(ProcessStartListener.class));
    try {
      stopRunner.run();
      fail("Expected exception due to logging error level.");
    } catch (AssertionError ae) {
      verify(callback, times(1))
          .errorOccurred("Stop application failed due to an unexpected error.");
    }
  }

}
