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

package com.google.cloud.tools.intellij.appengine.application;

import static org.mockito.Mockito.mock;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.PlatformTestCase;
import java.awt.Component;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link AppEngineApplicationCreateDialog}. */
public class AppEngineApplicationCreateDialogTest extends PlatformTestCase {
  private AppEngineApplicationCreateDialog dialog;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Component parent = new AppEngineApplicationInfoPanel();
    Credential credential = mock(Credential.class);
    ApplicationManager.getApplication()
        .invokeAndWait(() -> dialog = new AppEngineApplicationCreateDialog(parent, "projectId", credential));
  }

  //@Test
  public void testAllTextsAreSameSize() {
    int fontSize = dialog.getErrorIcon().getFont().getSize();
    // TODO: clean up, optimize
    Assert.assertEquals(fontSize, dialog.getInstructionsTextPane().getFont().getSize());
    Assert.assertEquals(fontSize, dialog.getRegionComboBox().getFont().getSize());
    Assert.assertEquals(fontSize, dialog.getRegionDetailPane().getFont().getSize());
    Assert.assertEquals(fontSize, dialog.getStatusPane().getFont().getSize());
  }
}
