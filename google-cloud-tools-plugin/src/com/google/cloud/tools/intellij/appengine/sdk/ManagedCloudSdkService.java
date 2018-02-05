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

import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.MessageListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import java.nio.file.Path;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

/**
 * {@link CloudSdkService} providing SDK that is managed and automatically installable, see {@link
 * com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk}
 */
// TODO(ivanporty) implementation coming in the next PR
public class ManagedCloudSdkService implements CloudSdkService {
  private Logger logger = Logger.getInstance(ManagedCloudSdkService.class);

  private ManagedCloudSdk managedCloudSdk;

  private SdkStatus sdkStatus = SdkStatus.NOT_AVAILABLE;

  private final Collection<SdkStatusUpdateListener> statusUpdateListeners = Lists.newArrayList();

  /** Called when this service becomes primary choice for serving Cloud SDK. */
  public void activate() {
    install();
  }

  @Nullable
  @Override
  public Path getSdkHomePath() {
    switch (sdkStatus) {
      case READY:
        return managedCloudSdk.getSdkHome();
      default:
        return null;
    }
  }

  @Override
  public void setSdkHomePath(String path) {
    /* unsupported, to be removed. */
  }

  @Override
  public SdkStatus getStatus() {
    return sdkStatus;
  }

  @Override
  public boolean install() {
    ThreadUtil.getInstance().executeInBackground(this::installSynchronously);
    return true;
  }

  @Override
  public void addStatusUpdateListener(SdkStatusUpdateListener listener) {
    statusUpdateListeners.add(listener);
  }

  @Override
  public void removeStatusUpdateListener(SdkStatusUpdateListener listener) {
    statusUpdateListeners.remove(listener);
  }

  @VisibleForTesting
  ManagedCloudSdk createManagedSdk() throws UnsupportedOsException {
    return ManagedCloudSdk.newManagedSdk();
  }

  @VisibleForTesting
  void setLogger(Logger logger) {
    this.logger = logger;
  }

  private void updateStatus(SdkStatus sdkStatus) {
    this.sdkStatus = sdkStatus;
    notifyListeners(this, sdkStatus);
  }

  private void notifyListeners(CloudSdkService sdkService, SdkStatus status) {
    statusUpdateListeners.forEach(listener -> listener.onSdkStatusChange(sdkService, status));
  }

  /** Checks for Managed Cloud SDK status and creates/installs it if necessary. */
  private void installSynchronously() {
    if (managedCloudSdk == null) {
      try {
        managedCloudSdk = createManagedSdk();
      } catch (UnsupportedOsException osex) {
        managedCloudSdk = null;
        updateStatus(SdkStatus.NOT_AVAILABLE);
        return;
      }
    }

    boolean managedSdkInstalled = false;
    try {
      managedSdkInstalled = managedCloudSdk.isInstalled();
    } catch (ManagedSdkVerificationException | ManagedSdkVersionMismatchException ex) {
      // TODO is recoverable?
      logger.error("Error while checking Cloud SDK status", ex);
    }

    if (!managedSdkInstalled) {
      SdkInstaller sdkInstaller;
      try {
        sdkInstaller = managedCloudSdk.newInstaller();
      } catch (UnsupportedOsException e) {
        updateStatus(SdkStatus.NOT_AVAILABLE);
        return;
      }

      MessageListener sdkInstallListener = logger::info;

      updateStatus(SdkStatus.INSTALLING);
      try {
        sdkInstaller.install(sdkInstallListener);
      } catch (Exception ex) {
        logger.error("Error while installing managed Cloud SDK", ex);
        updateStatus(SdkStatus.NOT_AVAILABLE);
        return;
      }
    }

    boolean hasAppEngineJava = false;
    try {
      hasAppEngineJava = managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA);
    } catch (ManagedSdkVerificationException ex) {
      // TODO is recoverable?
      logger.error("Error while checking Cloud SDK status", ex);
    }

    if (!hasAppEngineJava) {
      MessageListener appEngineInstallListener = logger::info;

      try {
        managedCloudSdk
            .newComponentInstaller()
            .installComponent(SdkComponent.APP_ENGINE_JAVA, appEngineInstallListener);
      } catch (Exception ex) {
        logger.error("Error while installing managed Cloud SDK App Engine Java component", ex);
        updateStatus(SdkStatus.NOT_AVAILABLE);
        return;
      }
    }

    updateStatus(SdkStatus.READY);
    logger.info("Managed Google Cloud SDK successfully installed.");
  }
}
