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

import static org.mockito.ArgumentMatchers.contains;
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
import org.mockito.Mock;

/** Tests for {@link CloudToolsFeedbackAction}. */
public class CloudToolsFeedbackActionTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService sdkService;
  @Mock @TestService private BrowserLauncher browserLauncher;

  @TestFile(name = "testCloudSdk")
  private File missingCloudSdk;

  private CloudToolsFeedbackAction feedbackAction;

  @Before
  public void setUp() {
    feedbackAction = new CloudToolsFeedbackAction();
  }

  @Test
  public void testDisplayableSdkVersion_isEmpty_whenSdkIsInvalid() {
    when(sdkService.getSdkHomePath()).thenReturn(missingCloudSdk.toPath());

    feedbackAction.actionPerformed(null /*event*/);

    String expected = "- Google Cloud SDK version: \n";
    verify(browserLauncher).browse(urlContains(expected), eq(null), eq(null));
  }

  @Test
  public void testDisplayableOs() {
    feedbackAction.actionPerformed(null /*event*/);

    String expected =
        String.format(
            "OS: %s %s\n", System.getProperty("os.name"), System.getProperty("os.version"));
    verify(browserLauncher).browse(urlContains(expected), eq(null), eq(null));
  }

  private static String urlContains(String unescapedMessage) {
    return contains(UrlEscapers.urlFormParameterEscaper().escape(unescapedMessage));
  }
}
