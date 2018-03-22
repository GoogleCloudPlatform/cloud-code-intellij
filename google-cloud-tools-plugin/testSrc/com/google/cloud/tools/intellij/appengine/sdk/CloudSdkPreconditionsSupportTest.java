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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatusUpdateListener;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceManager.CloudSdkPreconditionCheckCallback;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link CloudSdkPreconditionsSupport}. */
public class CloudSdkPreconditionsSupportTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkServiceManager mockCloudSdkServiceManager;
  @Mock private CloudSdkService mockSdkService;

  @Mock private Runnable mockRunnable;
  @Mock private Project mockProject;
  @Mock private CloudSdkPreconditionCheckCallback mockCallback;

  @Spy private CloudSdkPreconditionsSupport cloudSdkPreconditionsSupport;

  @Before
  public void setUp() {
    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);
  }

  @Test
  public void installingSdk_then_readySdk_correctly_runs() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.READY);

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication().invokeAndWait(() -> verify(mockRunnable).run());
  }

  @Test
  public void installingSdk_then_stillInstalling_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INSTALLING);
    // mock cancel operation for incomplete install.
    doReturn(true).when(cloudSdkPreconditionsSupport).checkIfCancelled();

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void installingSdk_then_invalidSdk_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void installingSdk_then_notAvailableSdk_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE);

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void installingSdk_then_userCancel_doesNotShowWarningNotification() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INSTALLING);
    // mock cancel operation for incomplete install.
    doReturn(true).when(cloudSdkPreconditionsSupport).checkIfCancelled();

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                // explicit parameters are not relevant but need to be passed for spy to work.
                verify(cloudSdkPreconditionsSupport, never())
                    .showCloudSdkNotification("", NotificationType.WARNING, false));
  }

  @Test
  public void installingSdk_then_invalidSdk_showsErrorNotification() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);

    cloudSdkPreconditionsSupport.runAfterCloudSdkPreconditionsMet(
        mockProject, mockRunnable, mockCallback);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                verify(cloudSdkPreconditionsSupport)
                    .showCloudSdkNotification(
                        GctBundle.message("appengine.deployment.error.sdk.invalid"),
                        NotificationType.ERROR,
                        true));
  }

  private void mockSdkStatusChange(SdkStatus fromStatus, SdkStatus toStatus) {
    when(mockSdkService.getStatus()).thenReturn(fromStatus);
    when(mockSdkService.isInstallReady()).thenReturn(true);
    // the only way to enable READY status before blocking on the same thread test thread starts.
    doAnswer(
            invocation -> {
              ((SdkStatusUpdateListener) invocation.getArgument(0))
                  .onSdkStatusChange(mockSdkService, toStatus);
              when(mockSdkService.getStatus()).thenReturn(toStatus);
              return null;
            })
        .when(mockSdkService)
        .addStatusUpdateListener(any());
  }
}
