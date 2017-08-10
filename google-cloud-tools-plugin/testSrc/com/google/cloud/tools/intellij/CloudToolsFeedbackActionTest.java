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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.net.UrlEscapers;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Tests for {@link CloudToolsFeedbackAction}. */
@RunWith(JUnit4.class)
public final class CloudToolsFeedbackActionTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService sdkService;
  @Mock @TestService private BrowserLauncher browserLauncher;

  @TestFile(name = "testCloudSdk")
  private File missingCloudSdk;

  private CloudToolsFeedbackAction feedbackAction;

  @Before
  public void setUp() throws Exception {
    feedbackAction = new CloudToolsFeedbackAction();
    maybeStubBrowserLauncher();
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

  /**
   * Stubs the {@link BrowserLauncher#browse(String, WebBrowser)} to call the {@link
   * BrowserLauncher#browse(String, WebBrowser, Project)} method with a {@code null} {@link Project}
   * if the method is non-final.
   *
   * <p>This required for compatibility reasons between the previous non-Kotlin source for {@link
   * BrowserLauncher} and the new Kotlin source.
   *
   * <p><b>Explanation:</b> The non-Kotlin source declares {@link BrowserLauncher#browse(String,
   * WebBrowser)} as abstract; the Kotlin source declares this method as final, whose implementation
   * calls into the {@link BrowserLauncher#browse(String, WebBrowser, Project)} method. Since
   * Mockito cannot stub final methods, the real implementation will be invoked if the underlying
   * source is the Kotlin version.
   *
   * <p>To support verifying the {@link BrowserLauncher#browse} method was called by the class under
   * test, the tests verify the 3-parameter method was called and this method will forward
   * invocations of the 2-parameter method if needed.
   */
  private void maybeStubBrowserLauncher() throws NoSuchMethodException {
    Method browseMethod = BrowserLauncher.class.getMethod("browse", String.class, WebBrowser.class);
    if (!Modifier.isFinal(browseMethod.getModifiers())) {
      doAnswer(
              invocation -> {
                String url = (String) invocation.getArguments()[0];
                browserLauncher.browse(url, null, null);
                return null;
              })
          .when(browserLauncher)
          .browse(anyString(), any());
    }
  }

  private static String urlContains(String unescapedMessage) {
    return contains(UrlEscapers.urlFormParameterEscaper().escape(unescapedMessage));
  }
}
