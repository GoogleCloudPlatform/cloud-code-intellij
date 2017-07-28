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

package com.google.cloud.tools.intellij;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.net.UrlEscapers;
import com.intellij.ide.browsers.BrowserLauncher;
import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Tests for {@link CloudToolsFeedbackAction}. */
public class CloudToolsFeedbackActionTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService sdkService;
  @Mock @TestService private BrowserLauncher browserLauncher;

  @TestFile(name = "testCloudSdk")
  private File testCloudSdk;

  private CloudToolsFeedbackAction feedbackAction;

  private static ArgumentCaptor<String> urlArg = ArgumentCaptor.forClass(String.class);

  @Before
  public void setUp() {
    feedbackAction = new CloudToolsFeedbackAction();
  }

  @Test
  public void testDisplayableSdkVersion_isEmpty_whenSdkIsInvalid() {
    when(sdkService.getSdkHomePath()).thenReturn(testCloudSdk.toPath());

    feedbackAction.actionPerformed(null /*event*/);
    verify(browserLauncher).browse(urlArg.capture(), eq(null));

    String expectedEmptySdkVersionMessage = "- Google Cloud SDK version: \n";
    assertTrue(urlContainsMessage(expectedEmptySdkVersionMessage));
  }

  @Test
  public void testDisplayableOs() {
    feedbackAction.actionPerformed(null /*event*/);
    verify(browserLauncher).browse(urlArg.capture(), eq(null));

    String expectedOsMessage =
        "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n";
    assertTrue(urlContainsMessage(expectedOsMessage));
  }

  private static boolean urlContainsMessage(String message) {
    return urlArg.getValue().contains(UrlEscapers.urlFormParameterEscaper().escape(message));
  }
}
