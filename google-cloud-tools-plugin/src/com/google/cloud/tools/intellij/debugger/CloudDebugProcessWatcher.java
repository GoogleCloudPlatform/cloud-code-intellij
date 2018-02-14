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

package com.google.cloud.tools.intellij.debugger;

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Checks for updates on Cloud Debugger targets in the background. When it detects a change (a new
 * breakpoint has entered final state), it shows a notification.
 *
 * <p>It enumerates all open projects and their run configurations looking for {@link
 * CloudDebugRunConfiguration} objects.
 *
 * <p>If it finds one that is marked for watching, it attempts to update its state.
 *
 * <p>The watcher is not started at all unless a {@link CloudDebugRunConfiguration} is created.
 *
 * <p>The poll interval is currently set at 10 seconds.
 */
public class CloudDebugProcessWatcher implements CloudBreakpointListener {

  private static final CloudDebugProcessWatcher instance = new CloudDebugProcessWatcher();
  private CloudDebugGlobalPoller poller = null;

  private CloudDebugProcessWatcher() {}

  @NotNull
  public static CloudDebugProcessWatcher getInstance() {
    return instance;
  }

  /** Creates a background listener. */
  public synchronized void ensureWatcher() {
    if (poller == null) {
      poller = new CloudDebugGlobalPoller();
      poller.addListener(this);
      poller.startBackgroundListening();
    }
  }

  /** Stop the background listener. */
  public synchronized void removeWatcher() {
    if (poller != null) {
      poller.stopBackgroundListening();
      poller = null;
    }
  }

  @Override
  public void onBreakpointListChanged(final CloudDebugProcessState state) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            NotificationGroup notificationGroup =
                NotificationGroup.balloonGroup("Cloud Debugger watcher");
            String message =
                GctBundle.getString(
                    "clouddebug.balloonnotification.message", state.getProjectName());
            UsageTrackerProvider.getInstance()
                .trackEvent(GctTracking.CLOUD_DEBUGGER_NOTIFY_BREAKPOINT_LIST_CHANGE)
                .ping();
            notificationGroup
                .createNotification(
                    "",
                    message,
                    NotificationType.INFORMATION,
                    new NotificationListener() {
                      @Override
                      public void hyperlinkUpdate(
                          @NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        notification.expire();
                        RunnerAndConfigurationSettings targetConfig = null;
                        RunManager manager = RunManager.getInstance(state.getProject());
                        for (final RunnerAndConfigurationSettings config :
                            manager.getAllSettings()) {
                          if (config.getConfiguration() instanceof CloudDebugRunConfiguration
                              && ((CloudDebugRunConfiguration) config.getConfiguration())
                                      .getProcessState()
                                  == state) {
                            targetConfig = config;
                          }
                        }

                        if (targetConfig != null) {
                          Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
                          ProgramRunnerUtil.executeConfiguration(
                              state.getProject(), targetConfig, executor);
                        }
                      }
                    })
                .notify(state.getProject());
          }
        });
  }
}
