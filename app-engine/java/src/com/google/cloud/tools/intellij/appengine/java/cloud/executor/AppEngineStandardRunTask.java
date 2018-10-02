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

package com.google.cloud.tools.intellij.appengine.java.cloud.executor;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.LocalRun;
import com.google.cloud.tools.appengine.cloudsdk.process.LegacyProcessHandler;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessHandler;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerService;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkServiceUserSettings;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkVersionNotifier;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.common.base.Strings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Represents an App Engine Standard run task. (i.e., devappserver) */
public class AppEngineStandardRunTask extends AppEngineTask {

  private static final Logger logger = Logger.getInstance(AppEngineStandardRunTask.class);

  private static final NotificationGroup NOTIFICATION_GROUP =
      new NotificationGroup(
          new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
          NotificationDisplayType.BALLOON,
          true,
          null,
          GoogleCloudCoreIcons.CLOUD);

  private RunConfiguration runConfig;
  private Sdk javaSdk;
  private String runnerId;

  /**
   * {@link AppEngineStandardRunTask} constructor.
   *
   * @param runConfig local run configuration to be sent to the common library
   * @param javaSdk JRE to run devappserver with
   * @param runnerId typically "Run" or "Debug", to indicate type of local run. To be used in
   *     metrics
   */
  public AppEngineStandardRunTask(
      @NotNull RunConfiguration runConfig, @NotNull Sdk javaSdk, @Nullable String runnerId) {
    this.runConfig = runConfig;
    this.javaSdk = javaSdk;
    this.runnerId = runnerId;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    // show a warning notification if the cloud sdk version is not supported
    CloudSdkVersionNotifier.getInstance().notifyIfVersionOutOfDate();

    CloudSdkService instance = CloudSdkService.getInstance();
    CloudSdk.Builder sdkBuilder =
        new CloudSdk.Builder().sdkPath(instance != null ? instance.getSdkHomePath() : null);

    if (javaSdk.getHomePath() != null) {
      sdkBuilder.javaHome(Paths.get(javaSdk.getHomePath()));
    }

    ProcessHandler processHandler =
        LegacyProcessHandler.builder().async(true).setStartListener(startListener).build();

    try {
      LocalRun localRun = LocalRun.builder(sdkBuilder.build()).build();
      localRun.newDevAppServer1(processHandler).run(runConfig);

      UsageTrackerService.getInstance()
          .trackEvent(GctTracking.APP_ENGINE_RUN)
          .addMetadata(GctTracking.METADATA_LABEL_KEY, Strings.nullToEmpty(runnerId))
          .addMetadata(
              GctTracking.METADATA_SDK_KEY,
              CloudSdkServiceUserSettings.getInstance().getUserSelectedSdkServiceType().name())
          .ping();
    } catch (AppEngineException aee) {
      if (aee.getCause() instanceof NoSuchFileException) {
        Notification notification =
            NOTIFICATION_GROUP.createNotification(
                AppEngineMessageBundle.message("appengine.run.nosuchfileexception.title"),
                null /* subtitle */,
                AppEngineMessageBundle.message("appengine.run.nosuchfileexception.message"),
                NotificationType.ERROR);
        notification.notify(null /* project */);
        logger.warn(
            "FileNotFoundException during local run. Check for missing appengine-web.xml file.");
      } else {
        logger.error(aee);
      }
    } catch (Exception ex) {
      logger.error(ex);
    }
  }
}
