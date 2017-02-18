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

import com.android.tools.idea.diagnostics.error.AnonymousFeedback;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.net.HttpConfigurable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Reports an error to Google Feedback in the background. */
public class GoogleAnonymousFeedbackTask extends Task.Backgroundable {

  @VisibleForTesting static final String CT4IJ_PRODUCT = "Cloud Tools for IntelliJ";
  @VisibleForTesting static final String CT4IJ_PACKAGE_NAME = "com.google.gct.idea";
  private static final FeedbackSender DEFAULT_FEEDBACK_SENDER = new NetworkFeedbackSender();
  private final Consumer<String> callback;
  private final Consumer<Exception> errorCallback;
  private final Throwable throwable;
  private final Map<String, String> params;
  private final String errorMessage;
  private final String errorDescription;
  private final String appVersion;
  private final FeedbackSender feedbackSender;

  public GoogleAnonymousFeedbackTask(
      @Nullable Project project,
      @NotNull String title,
      boolean canBeCancelled,
      @Nullable Throwable throwable,
      Map<String, String> params,
      String errorMessage,
      String errorDescription,
      String appVersion,
      final Consumer<String> callback,
      final Consumer<Exception> errorCallback) {
    this(
        project,
        title,
        canBeCancelled,
        throwable,
        params,
        errorMessage,
        errorDescription,
        appVersion,
        callback,
        errorCallback,
        DEFAULT_FEEDBACK_SENDER);
  }

  @VisibleForTesting
  GoogleAnonymousFeedbackTask(
      @Nullable Project project,
      @NotNull String title,
      boolean canBeCancelled,
      @Nullable Throwable throwable,
      Map<String, String> params,
      String errorMessage,
      String errorDescription,
      String appVersion,
      final Consumer<String> callback,
      final Consumer<Exception> errorCallback,
      FeedbackSender feedbackSender) {
    super(project, title, canBeCancelled);
    this.throwable = throwable;
    this.params = params;
    this.errorMessage = errorMessage;
    this.errorDescription = errorDescription;
    this.appVersion = appVersion;
    this.callback = callback;
    this.errorCallback = errorCallback;
    this.feedbackSender = feedbackSender;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    indicator.setIndeterminate(true);
    try {
      String token =
          feedbackSender.sendFeedback(
              CT4IJ_PRODUCT,
              CT4IJ_PACKAGE_NAME,
              throwable,
              errorMessage,
              errorDescription,
              appVersion,
              params);
      callback.consume(token);
    } catch (IOException ioe) {
      errorCallback.consume(ioe);
    } catch (RuntimeException re) {
      errorCallback.consume(re);
    }
  }

  /** Interface for sending feedback crash reports. */
  interface FeedbackSender {

    String sendFeedback(
        String feedbackProduct,
        String feedbackPackageName,
        Throwable cause,
        String errorMessage,
        String errorDescription,
        String applicationVersion,
        Map<String, String> keyValues)
        throws IOException;
  }

  private static class ProxyHttpConnectionFactory extends AnonymousFeedback.HttpConnectionFactory {

    @Override
    protected HttpURLConnection openHttpConnection(String path) throws IOException {
      return HttpConfigurable.getInstance().openHttpConnection(path);
    }
  }

  private static class NetworkFeedbackSender implements FeedbackSender {

    private static final AnonymousFeedback.HttpConnectionFactory connectionFactory =
        new ProxyHttpConnectionFactory();

    @Override
    public String sendFeedback(
        String feedbackProduct,
        String feedbackPackageName,
        Throwable cause,
        String errorMessage,
        String errorDescription,
        String applicationVersion,
        Map<String, String> keyValues)
        throws IOException {
      return AnonymousFeedback.sendFeedback(
          CT4IJ_PRODUCT,
          CT4IJ_PACKAGE_NAME,
          connectionFactory,
          cause,
          keyValues,
          errorMessage,
          errorDescription,
          applicationVersion);
    }
  }
}
