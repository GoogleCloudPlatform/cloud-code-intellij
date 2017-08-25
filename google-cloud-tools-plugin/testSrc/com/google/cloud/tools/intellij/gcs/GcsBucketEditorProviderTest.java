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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.Bucket;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;
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
  @Mock private Bucket bucket;

  @Before
  public void setUp() {
    bucketEditorProvider = new GcsBucketEditorProvider();
  }

  @Test
  public void testAcceptsCorrectFileType() {
    assertFalse(bucketEditorProvider.accept(testFixture.getProject(), new StubVirtualFile()));
    assertTrue(
        bucketEditorProvider.accept(testFixture.getProject(), new GcsBucketVirtualFile(bucket)));
  }
}
