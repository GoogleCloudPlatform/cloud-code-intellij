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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import java.io.File;

public class SelectConfigDestinationFolderDialogTest extends PlatformTestCase {

  private File file;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    file = createTempFile("temp file", "");
  }

  public void testSuggestion() {
    SelectConfigDestinationFolderDialog dialog =
        new SelectConfigDestinationFolderDialog(null, file.getParentFile().getPath(), "");
    assertEquals(file.toPath().getParent(), dialog.getDestinationFolder());
    Disposer.dispose(dialog.getDisposable());
  }

  public void testSuggestion_nonExistingParent() {
    SelectConfigDestinationFolderDialog dialog =
        new SelectConfigDestinationFolderDialog(null, "I don't exist.", "");
    assertEquals("I don't exist.", dialog.getDestinationFolder().toString());
    Disposer.dispose(dialog.getDisposable());
  }

  public void testSuggestion_nullPath() {
    SelectConfigDestinationFolderDialog dialog =
        new SelectConfigDestinationFolderDialog(null, null, "");
    assertEquals("", dialog.getDestinationFolder().toString());
    Disposer.dispose(dialog.getDisposable());
  }
}
