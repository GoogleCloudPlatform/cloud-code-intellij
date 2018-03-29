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
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceManager.CloudSdkStatusHandler;
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

/** Tests for {@link CloudSdkServiceManager}. */
public class CloudSdkServiceManagerTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkServiceManager mockCloudSdkServiceManager;
  @Mock private CloudSdkService mockSdkService;

  @Mock private Runnable mockRunnable;
  @Mock private Project mockProject;
  @Mock private CloudSdkStatusHandler mockStatusHandler;

  @Spy private CloudSdkServiceManager cloudSdkServiceManager;

  @Before
  public void setUp() {
    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);
    // empty error messages by default
    when(mockStatusHandler.getErrorMessage(any())).thenReturn("");
  }

  @Test
  public void installingSdk_then_readySdk_correctly_runs() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.READY);

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication().invokeAndWait(() -> verify(mockRunnable).run());
  }

  @Test
  public void waitFor_installingSdk_then_readySdk_noErrors() throws InterruptedException {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.READY);

    cloudSdkServiceManager.blockUntilSdkReady(mockProject, "", mockStatusHandler);

    ApplicationManager.getApplication()
        .invokeAndWait(() -> verify(mockStatusHandler, never()).onError(any()));
  }

  @Test
  public void installingSdk_then_stillInstalling_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INSTALLING);
    // mock cancel operation for incomplete install.
    doReturn(true).when(cloudSdkServiceManager).checkIfCancelled();

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void installingSdk_then_invalidSdk_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void waitFor_installingSdk_then_invalidSdk_reportsError() throws InterruptedException {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);

    cloudSdkServiceManager.blockUntilSdkReady(mockProject, "", mockStatusHandler);

    ApplicationManager.getApplication()
        .invokeAndWait(() -> verify(mockStatusHandler).onError(any()));
  }

  @Test
  public void installingSdk_then_notAvailableSdk_doesNotRun() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.NOT_AVAILABLE);

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication().invokeAndWait(() -> verifyNoMoreInteractions(mockRunnable));
  }

  @Test
  public void installingSdk_then_userCancel_doesNotShowWarningNotification() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INSTALLING);
    // mock cancel operation for incomplete install.
    doReturn(true).when(cloudSdkServiceManager).checkIfCancelled();

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                // explicit parameters are not relevant but need to be passed for spy to work.
                verify(cloudSdkServiceManager, never())
                    .showCloudSdkNotification("", NotificationType.WARNING));
  }

  @Test
  public void installingSdk_then_invalidSdk_showsErrorNotification() {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);
    when(mockStatusHandler.getErrorMessage(SdkStatus.INVALID))
        .thenReturn(GctBundle.message("appengine.deployment.error.sdk.invalid"));

    cloudSdkServiceManager.runWhenSdkReady(mockProject, mockRunnable, "", mockStatusHandler);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                verify(cloudSdkServiceManager)
                    .showCloudSdkNotification(
                        GctBundle.message("appengine.deployment.error.sdk.invalid"),
                        NotificationType.ERROR));
  }

  @Test
  public void waitFor_installingSdk_then_invalidSdk_showsErrorNotification()
      throws InterruptedException {
    mockSdkStatusChange(SdkStatus.INSTALLING, SdkStatus.INVALID);
    when(mockStatusHandler.getErrorMessage(SdkStatus.INVALID))
        .thenReturn("invalid SDK after waiting");

    cloudSdkServiceManager.blockUntilSdkReady(mockProject, "", mockStatusHandler);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                verify(cloudSdkServiceManager)
                    .showCloudSdkNotification("invalid SDK after waiting", NotificationType.ERROR));
  }

  private void mockSdkStatusChange(SdkStatus fromStatus, SdkStatus toStatus) {
    when(mockSdkService.getStatus()).thenReturn(fromStatus);
    when(mockSdkService.isInstallSupported()).thenReturn(true);
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
