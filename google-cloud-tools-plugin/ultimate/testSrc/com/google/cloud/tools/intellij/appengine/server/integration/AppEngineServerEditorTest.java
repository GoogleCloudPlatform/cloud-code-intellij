/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.server.integration;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import javax.swing.JLabel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * {@link AppEngineServerEditor} unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineServerEditorTest {

  private static String NO_DEVAPPSERVER_IN_GCLOUD =
      "Selected Cloud SDK does not contain the app-engine-java component.";

  @Test
  public void testInvalidPathChange() {
    AppEngineServerEditor editor = new AppEngineServerEditor();
    TextFieldWithBrowseButton sdkHomeField = editor.getSdkHomeField();
    JLabel warningMessage = editor.getWarningMessage();

    sdkHomeField.setText("/some/invalid/location");
    Assert.assertEquals(warningMessage.getText(), NO_DEVAPPSERVER_IN_GCLOUD);
  }
}
