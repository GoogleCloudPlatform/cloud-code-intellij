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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.feedback.GoogleAnonymousFeedbackTask.FeedbackSender;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Test cases for {@link GoogleAnonymousFeedbackTask}. */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAnonymousFeedbackTaskTest {

  private final Throwable cause = new Throwable("cause");
  private final Map<String, String> keyValues = ImmutableMap.of("key", "value");
  private final String errorMessage = "test message";
  private final String errorDescription = "test description";
  private final String version = "test version";
  @Mock private Consumer<String> mockResultConsumer;
  @Mock private Consumer<Exception> mockExceptionConsumer;
  @Mock private FeedbackSender mockFeedbackSender;
  @Mock private Project mockProject;
  private GoogleAnonymousFeedbackTask feedbackTask;

  @Before
  public void setUp() {
    feedbackTask =
        new GoogleAnonymousFeedbackTask(
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
            mockFeedbackSender);
  }

  @Test
  public void testRun_consumesResult() throws Exception {
    String result = "result";
    when(mockFeedbackSender.sendFeedback(
            GoogleAnonymousFeedbackTask.CT4IJ_PRODUCT,
            GoogleAnonymousFeedbackTask.CT4IJ_PACKAGE_NAME,
            cause,
            errorMessage,
            errorDescription,
            version,
            keyValues))
        .thenReturn(result);
    feedbackTask.run(mock(ProgressIndicator.class));
    verify(mockResultConsumer).consume(result);
    verify(mockExceptionConsumer, never()).consume(any(Exception.class));
  }

  @Test
  public void testRun_consumesError() throws Exception {
    IOException exception = new IOException("test");
    when(mockFeedbackSender.sendFeedback(
            GoogleAnonymousFeedbackTask.CT4IJ_PRODUCT,
            GoogleAnonymousFeedbackTask.CT4IJ_PACKAGE_NAME,
            cause,
            errorMessage,
            errorDescription,
            version,
            keyValues))
        .thenThrow(exception);
    feedbackTask.run(mock(ProgressIndicator.class));
    verify(mockExceptionConsumer).consume(exception);
    verify(mockResultConsumer, never()).consume(any(String.class));
  }
}
