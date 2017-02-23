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

package com.google.cloud.tools.intellij.feedback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.intellij.diagnostic.AbstractMessage;
import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.diagnostic.ReportMessages;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.SystemProperties;
import java.awt.Component;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class hooks into IntelliJ's error reporting framework. It's based off of <a
 * href="https://android.googlesource.com/platform/tools/adt/idea/+/studio-master-dev/android/src/com/android/tools/idea/diagnostics/error/ErrorReporter.java">
 * ErrorReporter.java </a> in Android Studio.
 */
public class GoogleFeedbackErrorReporter extends ErrorReportSubmitter {

  @VisibleForTesting static final String NONE_STRING = "__NONE___";
  @VisibleForTesting static final String ERROR_MESSAGE_KEY = "error.message";
  @VisibleForTesting static final String ERROR_STACKTRACE_KEY = "error.stacktrace";
  @VisibleForTesting static final String ERROR_DESCRIPTION_KEY = "error.description";
  @VisibleForTesting static final String LAST_ACTION_KEY = "last.action";
  @VisibleForTesting static final String OS_NAME_KEY = "os.name";
  @VisibleForTesting static final String JAVA_VERSION_KEY = "java.version";
  @VisibleForTesting static final String JAVA_VM_VENDOR_KEY = "java.vm.vendor";
  @VisibleForTesting static final String APP_NAME_KEY = "app.name";
  @VisibleForTesting static final String APP_CODE_KEY = "app.code";
  @VisibleForTesting static final String APP_NAME_VERSION_KEY = "app.name.version";
  @VisibleForTesting static final String APP_EAP_KEY = "app.eap";
  @VisibleForTesting static final String APP_INTERNAL_KEY = "app.internal";
  @VisibleForTesting static final String APP_VERSION_MAJOR_KEY = "app.version.major";
  @VisibleForTesting static final String APP_VERSION_MINOR_KEY = "app.version.minor";
  @VisibleForTesting static final String PLUGIN_VERSION = "plugin.version";

  private static boolean doSubmit(
      final IdeaLoggingEvent event,
      final Component parentComponent,
      final Consumer<SubmittedReportInfo> callback,
      final ErrorBean error,
      final String description) {
    error.setDescription(description);
    error.setMessage(event.getMessage());

    configureErrorFromEvent(event, error);

    ApplicationNamesInfo intelliJAppNameInfo = ApplicationNamesInfo.getInstance();
    ApplicationInfoEx intelliJAppExtendedInfo = ApplicationInfoEx.getInstanceEx();

    Map<String, String> params =
        buildKeyValuesMap(
            error,
            intelliJAppNameInfo,
            intelliJAppExtendedInfo,
            ApplicationManager.getApplication());

    DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    Consumer<String> successCallback =
        new Consumer<String>() {
          @Override
          public void consume(String token) {
            final SubmittedReportInfo reportInfo =
                new SubmittedReportInfo(
                    null, "Issue " + token, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
            callback.consume(reportInfo);

            ReportMessages.GROUP
                .createNotification(
                    ReportMessages.ERROR_REPORT, "Submitted", NotificationType.INFORMATION, null)
                .setImportant(false)
                .notify(project);
          }
        };

    Consumer<Exception> errorCallback =
        new Consumer<Exception>() {
          @Override
          public void consume(Exception ex) {
            String message =
                ErrorReporterBundle.message("error.googlefeedback.error", ex.getMessage());
            ReportMessages.GROUP
                .createNotification(
                    ReportMessages.ERROR_REPORT,
                    message,
                    NotificationType.ERROR,
                    NotificationListener.URL_OPENING_LISTENER)
                .setImportant(false)
                .notify(project);
          }
        };
    GoogleAnonymousFeedbackTask task =
        new GoogleAnonymousFeedbackTask(
            project,
            "Submitting error report",
            true,
            event.getThrowable(),
            params,
            error.getMessage(),
            error.getDescription(),
            ApplicationInfo.getInstance().getFullVersion(),
            successCallback,
            errorCallback);
    if (project == null) {
      task.run(new EmptyProgressIndicator());
    } else {
      ProgressManager.getInstance().run(task);
    }
    return true;
  }

  private static void configureErrorFromEvent(IdeaLoggingEvent event, ErrorBean error) {
    Throwable throwable = event.getThrowable();
    if (throwable != null) {
      PluginId pluginId = IdeErrorsDialog.findPluginId(throwable);
      if (pluginId != null) {
        IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
        if (ideaPluginDescriptor != null && !ideaPluginDescriptor.isBundled()) {
          error.setPluginName(ideaPluginDescriptor.getName());
          error.setPluginVersion(ideaPluginDescriptor.getVersion());
        }
      }
    }

    Object data = event.getData();

    if (data instanceof AbstractMessage) {
      error.setAttachments(((AbstractMessage) data).getIncludedAttachments());
    }
  }

  @VisibleForTesting
  static Map<String, String> buildKeyValuesMap(
      @NotNull ErrorBean error,
      @NotNull ApplicationNamesInfo intelliJAppNameInfo,
      @NotNull ApplicationInfoEx intelliJAppExtendedInfo,
      @NotNull Application application) {
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            // required parameters
            .put(ERROR_MESSAGE_KEY, nullToNone(error.getMessage()))
            .put(ERROR_STACKTRACE_KEY, nullToNone(error.getStackTrace()))
            // end or required parameters
            .put(ERROR_DESCRIPTION_KEY, nullToNone(error.getDescription()))
            .put(LAST_ACTION_KEY, nullToNone(error.getLastAction()))
            .put(OS_NAME_KEY, SystemProperties.getOsName())
            .put(JAVA_VERSION_KEY, SystemProperties.getJavaVersion())
            .put(JAVA_VM_VENDOR_KEY, SystemProperties.getJavaVmVendor())
            .put(APP_NAME_KEY, intelliJAppNameInfo.getFullProductName())
            .put(APP_CODE_KEY, intelliJAppExtendedInfo.getPackageCode())
            .put(APP_NAME_VERSION_KEY, intelliJAppExtendedInfo.getVersionName())
            .put(APP_EAP_KEY, Boolean.toString(intelliJAppExtendedInfo.isEAP()))
            .put(APP_INTERNAL_KEY, Boolean.toString(application.isInternal()))
            .put(APP_VERSION_MAJOR_KEY, intelliJAppExtendedInfo.getMajorVersion())
            .put(APP_VERSION_MINOR_KEY, intelliJAppExtendedInfo.getMinorVersion())
            .put(PLUGIN_VERSION, error.getPluginVersion())
            .build();

    return params;
  }

  static String nullToNone(@Nullable String possiblyNullString) {
    return possiblyNullString == null ? NONE_STRING : possiblyNullString;
  }

  @Override
  public String getReportActionText() {
    return ErrorReporterBundle.message("error.googlefeedback.message");
  }

  @Override
  public boolean submit(
      @NotNull IdeaLoggingEvent[] events,
      String additionalInfo,
      @NotNull Component parentComponent,
      @NotNull Consumer<SubmittedReportInfo> consumer) {
    ErrorBean errorBean = new ErrorBean(events[0].getThrowable(), IdeaLogger.ourLastActionId);
    return doSubmit(events[0], parentComponent, consumer, errorBean, additionalInfo);
  }
}
