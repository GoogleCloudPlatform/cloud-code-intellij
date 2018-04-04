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

package com.google.cloud.tools.intellij.appengine.sdk;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ThreadTracker;
import java.time.Clock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkUpdater}. */
public class ManagedCloudSdkUpdaterTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private ManagedCloudSdkService mockSdkService;
  @TestService @Mock private CloudSdkServiceManager mockSdkServiceManager;
  @Mock private Clock mockClock;

  @Spy private ManagedCloudSdkUpdater managedCloudSdkUpdater;

  @Before
  public void setUp() {
    // add timer thread to one not to be checked for 'leaks'
    ThreadTracker.longRunningThreadCreated(
        ApplicationManager.getApplication(), ManagedCloudSdkUpdater.SDK_UPDATER_THREAD_NAME);

    when(mockSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);

    doReturn(mockClock).when(managedCloudSdkUpdater).getClock();
  }

  @Test
  public void update_called_afterPeriod_elapses() {
    CloudSdkServiceUserSettings.getInstance().setLastAutomaticUpdateTimestamp(1);
    when(mockClock.millis()).thenReturn(ManagedCloudSdkUpdater.SDK_UPDATE_INTERVAL + 1);

    managedCloudSdkUpdater.activate();
  }
}
