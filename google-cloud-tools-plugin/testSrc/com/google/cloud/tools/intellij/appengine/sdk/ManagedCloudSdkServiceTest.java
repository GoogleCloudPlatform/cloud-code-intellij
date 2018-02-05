/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.log.TestInMemoryLogger;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkService} */
public class ManagedCloudSdkServiceTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Spy private ManagedCloudSdkService sdkService;

  @Mock private ManagedCloudSdk mockManagedCloudSdk;

  @Mock private CloudSdkService.SdkStatusUpdateListener mockStatusUpdateListener;

  @Before
  public void setUp() throws UnsupportedOsException {
    doReturn(mockManagedCloudSdk).when(sdkService).createManagedSdk();
    sdkService.setLogger(new TestInMemoryLogger());
    // make sure everything in test is done synchronously
    ExecutorService directExecutorService = MoreExecutors.newDirectExecutorService();
    ThreadUtil.getInstance().setBackgroundExecutorService(directExecutorService);
  }

  @Test
  public void initial_service_notActivated_status_notAvailable() {
    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }

  @Test
  public void initial_service_notActivated_path_isNull() {
    assertThat((Object) sdkService.getSdkHomePath()).isNull();
  }

  @Test
  public void activate_service_sdkInstalled_status_ready() {
    Path mockSdkPath = Paths.get("/tools/gcloud");
    makeMockSdkInstalled(mockSdkPath);

    sdkService.activate();

    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.READY);
  }

  @Test
  public void activate_service_sdkInstalled_sdkPath_valid() {
    Path mockSdkPath = Paths.get("/tools/gcloud");
    makeMockSdkInstalled(mockSdkPath);

    sdkService.activate();

    assertThat((Object) sdkService.getSdkHomePath()).isEqualTo(mockSdkPath);
  }

  @Test
  public void install_isSupported() {
    Path mockSdkPath = Paths.get("/tools/gcloud");
    makeMockSdkInstalled(mockSdkPath);

    assertThat(sdkService.install()).isTrue();
  }

  @Test
  public void successful_install_changesSdkStatus_inProgress() {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);

    Path mockSdkPath = Paths.get("/tools/gcloud");
    emulateMockSdkInstallationProcess(mockSdkPath);
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void install_thenException_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    Path mockSdkPath = Paths.get("/tools/gcloud");
    emulateMockSdkInstallationProcess(mockSdkPath);
    SdkInstaller mockInstaller = mockManagedCloudSdk.newInstaller();
    when(mockInstaller.install(any())).thenThrow(new IOException("IO Error"));
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  @Test
  public void install_appEngineException_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    Path mockSdkPath = Paths.get("/tools/gcloud");
    emulateMockSdkInstallationProcess(mockSdkPath);
    SdkComponentInstaller mockComponentInstaller = mockManagedCloudSdk.newComponentInstaller();
    doThrow(new CommandExecutionException(new UnsupportedOperationException()))
        .when(mockComponentInstaller)
        .installComponent(any(), any());
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  /** Mocks managed SDK as if installed and having App Engine Component. */
  private void makeMockSdkInstalled(Path mockSdkPath) {
    try {
      when(mockManagedCloudSdk.isInstalled()).thenReturn(true);
      when(mockManagedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(true);
      when(mockManagedCloudSdk.getSdkHome()).thenReturn(mockSdkPath);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }

  private void emulateMockSdkInstallationProcess(Path mockSdkPath) {
    try {
      when(mockManagedCloudSdk.isInstalled()).thenReturn(false);
      SdkInstaller mockInstaller = mock(SdkInstaller.class);
      when(mockManagedCloudSdk.newInstaller()).thenReturn(mockInstaller);
      when(mockManagedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
      SdkComponentInstaller mockComponentInstaller = mock(SdkComponentInstaller.class);
      when(mockManagedCloudSdk.newComponentInstaller()).thenReturn(mockComponentInstaller);
      when(mockManagedCloudSdk.getSdkHome()).thenReturn(mockSdkPath);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }

  /*@Test
  public void realInstall() {
    ManagedCloudSdkService realService = new ManagedCloudSdkService();
    realService.setLogger(new TestInMemoryLogger());
    realService.activate();
    System.out.println(realService.getSdkHomePath());
  }*/
}