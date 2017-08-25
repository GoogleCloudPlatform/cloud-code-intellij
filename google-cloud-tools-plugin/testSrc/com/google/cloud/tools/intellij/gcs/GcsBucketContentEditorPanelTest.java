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
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.swing.JTable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link GcsBucketContentEditorPanel}. */
public class GcsBucketContentEditorPanelTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private GcsBucketContentEditorPanel editorPanel;
  private GcsBucketVirtualFile bucketVirtualFile;
  @Mock private Blob blob;

  @Before
  public void setUp() {
    bucketVirtualFile = GcsTestUtils.createVirtualFileWithBucketMocks();
  }

  @Test
  public void testEmptyBucket_noBucketContentTable() {
    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(0);
    assertThat(bucketTable.getRowCount()).isEqualTo(0);
  }

  @Test
  public void testBucketContentTableInitialization() {
    List<Blob> blobs = Lists.newArrayList(blob);
    Page<Blob> blobPage = bucketVirtualFile.getBucket().list();
    when(blobPage.iterateAll()).thenReturn(blobs);
    when(blob.getName()).thenReturn("blobName");

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(4);
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    Map<Integer, String> indexToColName =
        ImmutableMap.of(0, "Name", 1, "Size", 2, "Type", 3, "Last Modified");
    IntStream.range(0, bucketTable.getColumnCount())
        .forEach(i -> assertThat(indexToColName.get(i)).isEqualTo(bucketTable.getColumnName(i)));
  }
}
