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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.sdk.ManagedCloudSdkService.ManagedSdkJobResult;
import com.google.cloud.tools.intellij.appengine.sdk.ManagedCloudSdkService.ManagedSdkJobType;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.log.TestInMemoryLogger;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.components.SdkUpdater;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.common.util.concurrent.MoreExecutors;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ThreadTracker;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
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

  private final Path MOCK_SDK_PATH = Paths.get("/tools/gcloud");

  @Spy private ManagedCloudSdkService sdkService;

  @Mock private ManagedCloudSdk mockManagedCloudSdk;

  @Mock private CloudSdkService.SdkStatusUpdateListener mockStatusUpdateListener;

  @Mock private ManagedCloudSdkServiceUiPresenter mockUiPresenter;

  @Mock private ProgressListener mockProgressListener;

  @Before
  public void setUp() throws UnsupportedOsException {
    // add timer thread to one not to be checked for 'leaks'
    ThreadTracker.longRunningThreadCreated(
        ApplicationManager.getApplication(), ManagedCloudSdkUpdater.SDK_UPDATER_THREAD_NAME);

    doReturn(mockManagedCloudSdk).when(sdkService).createManagedSdk();
    // TODO(ivanporty) remove once test logging system is done via CloudToolsRule
    sdkService.setLogger(new TestInMemoryLogger());
    // make sure everything in test is done synchronously
    ExecutorService directExecutorService = MoreExecutors.newDirectExecutorService();
    ThreadUtil.getInstance().setBackgroundExecutorService(directExecutorService);
    // run UI updates synchronously
    doAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(0)).run();
              return null;
            })
        .when(sdkService)
        .invokeOnApplicationUIThread(any());
    // replace UI presenter for verifications
    ManagedCloudSdkServiceUiPresenter.setInstance(mockUiPresenter);
    when(mockUiPresenter.createProgressListener(any())).thenReturn(mockProgressListener);
    // init SDK, most tests require initialized state.
    sdkService.initManagedSdk();
  }

  @Test
  public void initial_service_notActivated_status_notAvailable() {
    assertThat(new ManagedCloudSdkService().getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }

  @Test
  public void initial_service_notActivated_path_isNull() {
    assertThat((Object) new ManagedCloudSdkService().getSdkHomePath()).isNull();
  }

  @Test
  public void activate_service_sdkInstalled_status_ready() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    sdkService.activate();

    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.READY);
  }

  @Test
  public void activate_service_sdkInstalled_sdkPath_valid() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    sdkService.activate();

    assertThat((Object) sdkService.getSdkHomePath()).isEqualTo(MOCK_SDK_PATH);
  }

  @Test
  public void install_isSupported() {
    makeMockSdkInstalled(MOCK_SDK_PATH);

    assertThat(sdkService.isInstallSupported()).isTrue();
  }

  @Test
  public void removeListener_does_remove() {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.removeStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);

    verifyNoMoreInteractions(mockStatusUpdateListener);
  }

  @Test
  public void successful_install_returnsValidSdkPath() {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    sdkService.install();

    assertThat((Object) sdkService.getSdkHomePath()).isEqualTo(MOCK_SDK_PATH);
  }

  @Test
  public void successful_install_changesSdkStatus_inProgress() {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);

    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void successful_install_showsNotification() {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    sdkService.install();

    verify(mockUiPresenter)
        .notifyManagedSdkJobSuccess(ManagedSdkJobType.INSTALL, ManagedSdkJobResult.PROCESSED);
  }

  @Test
  public void sdkUpToDate_install_passes_valid_jobSuccessResult() {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    sdkService.install();

    verify(mockUiPresenter)
        .notifyManagedSdkJobSuccess(ManagedSdkJobType.INSTALL, ManagedSdkJobResult.UP_TO_DATE);
  }

  @Test
  public void failed_install_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller mockInstaller = mockManagedCloudSdk.newInstaller();
    when(mockInstaller.install(any(), any())).thenThrow(new IOException("IO Error"));
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  @Test
  public void failed_install_showsErrorNotification() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller mockInstaller = mockManagedCloudSdk.newInstaller();
    IOException ioException = new IOException("IO Error");
    when(mockInstaller.install(any(), any())).thenThrow(ioException);

    sdkService.install();

    verify(mockUiPresenter)
        .notifyManagedSdkJobFailure(ManagedSdkJobType.INSTALL, ioException.toString());
  }

  @Test
  public void failed_install_removesProgressIndicator() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller mockInstaller = mockManagedCloudSdk.newInstaller();
    IOException ioException = new IOException("IO Error");
    when(mockInstaller.install(any(), any())).thenThrow(ioException);

    sdkService.install();

    verify(mockProgressListener, atLeastOnce()).done();
  }

  @Test
  public void failed_install_appEngineException_changesSdkStatus_inProgress() throws Exception {
    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkComponentInstaller mockComponentInstaller = mockManagedCloudSdk.newComponentInstaller();
    doThrow(new CommandExecutionException(new UnsupportedOperationException()))
        .when(mockComponentInstaller)
        .installComponent(any(), any(), any());
    sdkService.install();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE));
  }

  @Test
  public void interruptedInstall_status_notAvailable() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller sdkInstaller = mockManagedCloudSdk.newInstaller();
    when(sdkInstaller.install(any(), any())).thenThrow(new InterruptedException());
    when(mockManagedCloudSdk.newInstaller()).thenReturn(sdkInstaller);

    sdkService.install();

    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }

  @Test
  public void interruptedInstall_showsCancelNotification() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller sdkInstaller = mockManagedCloudSdk.newInstaller();
    when(sdkInstaller.install(any(), any())).thenThrow(new InterruptedException());
    when(mockManagedCloudSdk.newInstaller()).thenReturn(sdkInstaller);

    sdkService.install();

    verify(mockUiPresenter).notifyManagedSdkJobCancellation(ManagedSdkJobType.INSTALL);
  }

  @Test
  public void cancelledInstall_showsCancelNotification() throws Exception {
    emulateMockSdkInstallationProcess(MOCK_SDK_PATH);
    SdkInstaller sdkInstaller = mockManagedCloudSdk.newInstaller();
    when(sdkInstaller.install(any(), any())).thenThrow(new CancellationException());
    when(mockManagedCloudSdk.newInstaller()).thenReturn(sdkInstaller);

    sdkService.install();

    verify(mockUiPresenter).notifyManagedSdkJobCancellation(ManagedSdkJobType.INSTALL);
  }

  @Test
  public void successful_update_changesSdkStatus_inProgress() {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();

    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.update();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void successful_update_showsNotification() {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();

    sdkService.update();

    verify(mockUiPresenter)
        .notifyManagedSdkJobSuccess(ManagedSdkJobType.UPDATE, ManagedSdkJobResult.PROCESSED);
  }

  @Test
  public void upToDate_sdk_passes_valid_jobSuccessResult() throws ManagedSdkVerificationException {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    when(mockManagedCloudSdk.isUpToDate()).thenReturn(true);

    sdkService.update();

    verify(mockUiPresenter)
        .notifyManagedSdkJobSuccess(ManagedSdkJobType.UPDATE, ManagedSdkJobResult.UP_TO_DATE);
  }

  @Test
  public void interrupted_update_keepsSdkStatus_available() throws Exception {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();
    SdkUpdater mockUpdater = mockManagedCloudSdk.newUpdater();
    doThrow(new InterruptedException()).when(mockUpdater).update(any(), any());

    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.update();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void cancelled_update_keepsSdkStatus_available() throws Exception {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();
    SdkUpdater mockUpdater = mockManagedCloudSdk.newUpdater();
    doThrow(new CancellationException()).when(mockUpdater).update(any(), any());

    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.update();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void cancelled_update_showsNotification() throws Exception {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();
    SdkUpdater mockUpdater = mockManagedCloudSdk.newUpdater();
    doThrow(new CancellationException()).when(mockUpdater).update(any(), any());

    sdkService.update();

    verify(mockUiPresenter).notifyManagedSdkJobCancellation(ManagedSdkJobType.UPDATE);
  }

  @Test
  public void failed_update_validSdk_sdkStatus_available() throws Exception {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();
    SdkUpdater mockUpdater = mockManagedCloudSdk.newUpdater();
    doThrow(new CommandExitException(-1, "")).when(mockUpdater).update(any(), any());

    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.update();

    ArgumentCaptor<SdkStatus> statusCaptor = ArgumentCaptor.forClass(SdkStatus.class);
    verify(mockStatusUpdateListener, times(2)).onSdkStatusChange(any(), statusCaptor.capture());

    assertThat(statusCaptor.getAllValues())
        .isEqualTo(Arrays.asList(SdkStatus.INSTALLING, SdkStatus.READY));
  }

  @Test
  public void failed_update_invalidSdk_makesSdkStatus_notAvailable() throws Exception {
    makeMockSdkInstalled(MOCK_SDK_PATH);
    emulateMockSdkUpdateProcess();
    SdkUpdater mockUpdater = mockManagedCloudSdk.newUpdater();
    doThrow(new CommandExitException(-1, "")).when(mockUpdater).update(any(), any());
    // update breaks SDK
    when(mockManagedCloudSdk.isInstalled()).thenReturn(false);

    sdkService.addStatusUpdateListener(mockStatusUpdateListener);
    sdkService.update();

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

  /** Mocks successful installation process with all steps included (SDK, App Engine Java). */
  private void emulateMockSdkInstallationProcess(Path mockSdkPath) {
    try {
      when(mockManagedCloudSdk.isInstalled()).thenReturn(false);
      SdkInstaller mockInstaller = mock(SdkInstaller.class);
      when(mockManagedCloudSdk.newInstaller()).thenReturn(mockInstaller);
      when(mockInstaller.install(any(), any())).thenReturn(mockSdkPath);

      when(mockManagedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)).thenReturn(false);
      SdkComponentInstaller mockComponentInstaller = mock(SdkComponentInstaller.class);
      when(mockManagedCloudSdk.newComponentInstaller()).thenReturn(mockComponentInstaller);

      when(mockManagedCloudSdk.getSdkHome()).thenReturn(mockSdkPath);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }

  /** Mocks out-of-date SDK and update process. */
  private void emulateMockSdkUpdateProcess() {
    try {
      when(mockManagedCloudSdk.isUpToDate()).thenReturn(false);
      SdkUpdater mockUpdater = mock(SdkUpdater.class);
      when(mockManagedCloudSdk.newUpdater()).thenReturn(mockUpdater);
    } catch (Exception ex) {
      // shouldn't happen in the tests.
      throw new AssertionError(ex);
    }
  }
}
