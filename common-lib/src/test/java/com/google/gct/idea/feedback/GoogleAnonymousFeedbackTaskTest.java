package com.google.gct.idea.feedback;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.gct.idea.feedback.GoogleAnonymousFeedbackTask.FeedbackSender;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Map;

/**
 * Test cases for {@link GoogleAnonymousFeedbackTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAnonymousFeedbackTaskTest {

  @Mock
  private Consumer<String> mockResultConsumer;

  @Mock
  private Consumer<Exception> mockExceptionConsumer;

  @Mock
  private FeedbackSender mockFeedbackSender;

  @Mock
  private Project mockProject;
  private final Throwable cause = new Throwable("cause");
  private final Map<String, String> keyValues = ImmutableMap.of("key", "value");
  private final String errorMessage = "test message";
  private final String errorDescription = "test description";
  private final String version = "test version";

  private GoogleAnonymousFeedbackTask feedbackTask;

  @Before
  public void setUp() {
    feedbackTask = new GoogleAnonymousFeedbackTask(
        mockProject,
        "test title",
        true,
        cause,
        keyValues,
        errorMessage,
        errorDescription,
        version,
        mockResultConsumer,
        mockExceptionConsumer,
        mockFeedbackSender
    );
  }

  @Test
  public void testRun_consumesResult() throws Exception {
    String result = "result";
    when(mockFeedbackSender
            .sendFeedback(
                GoogleAnonymousFeedbackTask.CT4IJ_PRODUCT,
                GoogleAnonymousFeedbackTask.CT4IJ_PACKAGE_NAME,
                cause,
                errorMessage,
                errorDescription,
                version,
                keyValues)
    ).thenReturn(result);
    feedbackTask.run(mock(ProgressIndicator.class));
    verify(mockResultConsumer).consume(result);
    verify(mockExceptionConsumer, never()).consume(any(Exception.class));
  }

  @Test
  public void testRun_consumesError() throws Exception {
    IOException exception = new IOException("test");
    when(mockFeedbackSender
            .sendFeedback(
                GoogleAnonymousFeedbackTask.CT4IJ_PRODUCT,
                GoogleAnonymousFeedbackTask.CT4IJ_PACKAGE_NAME,
                cause,
                errorMessage,
                errorDescription,
                version,
                keyValues)
    ).thenThrow(exception);
    feedbackTask.run(mock(ProgressIndicator.class));
    verify(mockExceptionConsumer).consume(exception);
    verify(mockResultConsumer, never()).consume(any(String.class));
  }
}