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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ThreadTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Clock;
import java.util.TimerTask;
import javax.swing.Timer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkUpdateService}. */
public class ManagedCloudSdkUpdateServiceTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private ManagedCloudSdkService mockSdkService;
  @TestService @Mock private CloudSdkServiceManager mockSdkServiceManager;
  @Mock private Clock mockClock;
  @Mock private Timer mockUiTimer;
  @Mock private ManagedCloudSdkServiceUiPresenter mockUiPresenter;
  @Mock private Notification mockNotification;

  @TestService @Mock private PluginInfoService mockPluginInfoService;

  @Spy private ManagedCloudSdkUpdateService managedCloudSdkUpdateService;

  @Before
  public void setUp() {
    // add timer thread to one not to be checked for 'leaks'
    ThreadTracker.longRunningThreadCreated(
        ApplicationManager.getApplication(), ManagedCloudSdkUpdateService.SDK_UPDATER_THREAD_NAME);

    when(mockPluginInfoService.shouldEnable(GctFeature.MANAGED_SDK_UPDATE)).thenReturn(true);

    when(mockSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);
    ManagedCloudSdkServiceUiPresenter.setInstance(mockUiPresenter);

    doReturn(mockClock).when(managedCloudSdkUpdateService).getClock();
    doReturn(mockUiTimer).when(managedCloudSdkUpdateService).createUiTimer(anyInt());

    when(mockUiPresenter.notifyManagedSdkUpdate(any(), any())).thenReturn(mockNotification);

    // directly execute scheduled tasks.
    doAnswer(
            invocationOnMock -> {
              ((TimerTask) invocationOnMock.getArgument(0)).run();
              return null;
            })
        .when(managedCloudSdkUpdateService)
        .schedule(any(), anyLong(), anyLong());
    // directly call task assigned to UI timer.
    doAnswer(
            invocationOnMock -> {
              ((ActionListener) invocationOnMock.getArgument(0))
                  .actionPerformed(mock(ActionEvent.class));
              return null;
            })
        .when(mockUiTimer)
        .addActionListener(any());

    CloudSdkServiceUserSettings.reset();
  }

  @Test
  public void update_scheduledNow_ifLastUpdate_elapsed() {
    CloudSdkServiceUserSettings.getInstance().setLastAutomaticUpdateTimestamp(1);
    when(mockClock.millis()).thenReturn(ManagedCloudSdkUpdateService.SDK_UPDATE_INTERVAL_MS + 2);

    managedCloudSdkUpdateService.activate();

    verify(managedCloudSdkUpdateService)
        .schedule(any(), eq(0L), eq(ManagedCloudSdkUpdateService.SDK_UPDATE_INTERVAL_MS));
  }

  @Test
  public void update_scheduledOnShorterTime_ifLastUpdate_fartherInterval() {
    CloudSdkServiceUserSettings.getInstance().setLastAutomaticUpdateTimestamp(1);
    when(mockClock.millis()).thenReturn(ManagedCloudSdkUpdateService.SDK_UPDATE_INTERVAL_MS / 2);

    managedCloudSdkUpdateService.activate();

    verify(managedCloudSdkUpdateService)
        .schedule(
            any(),
            eq((ManagedCloudSdkUpdateService.SDK_UPDATE_INTERVAL_MS / 2) + 1),
            eq(ManagedCloudSdkUpdateService.SDK_UPDATE_INTERVAL_MS));
  }

  @Test
  public void update_called_when_sdk_notUpToDate() {
    when(mockSdkService.isUpToDate()).thenReturn(false);

    managedCloudSdkUpdateService.activate();

    // managed SDK is UI thread only,
    ApplicationManager.getApplication().invokeAndWait(() -> verify(mockSdkService).update());
  }

  @Test
  public void update_notCalled_when_sdk_upToDate() {
    when(mockSdkService.isUpToDate()).thenReturn(true);

    managedCloudSdkUpdateService.activate();

    // managed SDK is UI thread only,
    ApplicationManager.getApplication()
        .invokeAndWait(() -> verify(mockSdkService, never()).update());
  }

  @Test
  public void notification_shown_beforeUpdate() {
    when(mockSdkService.isUpToDate()).thenReturn(false);

    managedCloudSdkUpdateService.activate();

    ApplicationManager.getApplication()
        .invokeAndWait(() -> verify(mockUiPresenter).notifyManagedSdkUpdate(any(), any()));
  }

  @Test
  public void notification_disableUpdates_updatesSettings() {
    when(mockSdkService.isUpToDate()).thenReturn(false);
    CloudSdkServiceUserSettings.getInstance().setEnableAutomaticUpdates(true);

    managedCloudSdkUpdateService.activate();

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              ArgumentCaptor<ActionListener> disableListener =
                  ArgumentCaptor.forClass(ActionListener.class);
              verify(mockUiPresenter).notifyManagedSdkUpdate(any(), disableListener.capture());
              disableListener.getValue().actionPerformed(mock(ActionEvent.class));

              assertThat(CloudSdkServiceUserSettings.getInstance().isAutomaticUpdateEnabled())
                  .isFalse();
            });
  }
}
