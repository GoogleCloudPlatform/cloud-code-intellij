package com.google.gct.idea.feedback;

import com.google.common.annotations.VisibleForTesting;

import com.android.tools.idea.diagnostics.error.AnonymousFeedback;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.net.HttpConfigurable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Reports an error to Google Feedback in the background.
 */
public class GoogleAnonymousFeedbackTask extends Task.Backgroundable {

  @VisibleForTesting
  static final String CT4IJ_PRODUCT = "Cloud Tools for IntelliJ";
  @VisibleForTesting
  static final String CT4IJ_PACKAGE_NAME = "com.google.gct.idea";
  private final Consumer<String> myCallback;
  private final Consumer<Exception> myErrorCallback;
  private final Throwable myThrowable;
  private final Map<String, String> myParams;
  private final String myErrorMessage;
  private final String myErrorDescription;
  private final String myAppVersion;
  private final FeedbackSender myFeedbackSender;
  private static final FeedbackSender DEFAULT_FEEDBACK_SENDER = new NetworkFeedbackSender();

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
    this(project, title, canBeCancelled, throwable, params, errorMessage, errorDescription,
        appVersion, callback, errorCallback, DEFAULT_FEEDBACK_SENDER);
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
    myThrowable = throwable;
    myParams = params;
    myErrorMessage = errorMessage;
    myErrorDescription = errorDescription;
    myAppVersion = appVersion;
    myCallback = callback;
    myErrorCallback = errorCallback;
    myFeedbackSender = feedbackSender;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    indicator.setIndeterminate(true);
    try {
      String token = myFeedbackSender.sendFeedback(
          CT4IJ_PRODUCT,
          CT4IJ_PACKAGE_NAME,
          myThrowable,
          myErrorMessage,
          myErrorDescription,
          myAppVersion,
          myParams
      );
      myCallback.consume(token);
    } catch (IOException e) {
      myErrorCallback.consume(e);
    } catch (RuntimeException re) {
      myErrorCallback.consume(re);
    }
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
    public String sendFeedback(String feedbackProduct, String feedbackPackageName, Throwable cause,
        String errorMessage, String errorDescription, String applicationVersion,
        Map<String, String> keyValues) throws IOException {
      return AnonymousFeedback.sendFeedback(CT4IJ_PRODUCT, CT4IJ_PACKAGE_NAME,
          connectionFactory, cause, keyValues,
          errorMessage, errorDescription, applicationVersion);
    }
  }

  /**
   * Interface for sending feedback crash reports.
   */
  interface FeedbackSender {

    String sendFeedback(
        String feedbackProduct,
        String feedbackPackageName,
        Throwable cause,
        String errorMessage,
        String errorDescription,
        String applicationVersion,
        Map<String, String> keyValues
    ) throws IOException;
  }
}
