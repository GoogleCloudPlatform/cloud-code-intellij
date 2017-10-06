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

package com.google.cloud.tools.intellij.gcs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link GcsBucketEditorProvider}. */
public class GcsBucketEditorProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  private GcsBucketEditorProvider bucketEditorProvider;
  @Mock private VirtualFile virtualFile;
  @Mock private GcsBucketVirtualFile gcsBucketVirtualFile;

  @Before
  public void setUp() {
    bucketEditorProvider = new GcsBucketEditorProvider();
    GcsTestUtils.setupVirtualFileWithBucketMocks(gcsBucketVirtualFile);
  }

  @Test
  public void testAcceptsCorrectFileType() {
    assertFalse(bucketEditorProvider.accept(testFixture.getProject(), virtualFile));
    assertTrue(bucketEditorProvider.accept(testFixture.getProject(), gcsBucketVirtualFile));
  }

  @Test
  public void testCreateEditor() {
    FileEditor editor =
        bucketEditorProvider.createEditor(testFixture.getProject(), gcsBucketVirtualFile);

    assertNotNull(editor);
    assertThat(editor).isInstanceOf(GcsBucketContentEditor.class);
  }
}
