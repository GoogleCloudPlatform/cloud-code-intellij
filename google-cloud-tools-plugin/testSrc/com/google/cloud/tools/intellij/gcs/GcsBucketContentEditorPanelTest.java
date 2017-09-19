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
import com.google.cloud.tools.intellij.testing.TimeZoneRule;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.IntStream;
import javax.swing.JTable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link GcsBucketContentEditorPanel}. */
public class GcsBucketContentEditorPanelTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @Rule public final TimeZoneRule timeZoneRule = new TimeZoneRule(TimeZone.getTimeZone("GMT"));

  private GcsBucketContentEditorPanel editorPanel;
  private GcsBucketVirtualFile bucketVirtualFile;

  @Mock private Blob directoryBlob;
  @Mock private Blob binaryBlob;

  private BiMap<Integer, String> indexToColName =
      HashBiMap.create(ImmutableMap.of(0, "Name", 1, "Size", 2, "Type", 3, "Last Modified"));
  private Map<String, Integer> colNameToIndex = indexToColName.inverse();

  @Before
  public void setUp() {
    bucketVirtualFile = GcsTestUtils.createVirtualFileWithBucketMocks();

    when(directoryBlob.isDirectory()).thenReturn(true);
    when(directoryBlob.getName()).thenReturn("my_directory");

    when(binaryBlob.isDirectory()).thenReturn(false);
    when(binaryBlob.getName()).thenReturn("my_blob.zip");
    when(binaryBlob.getSize()).thenReturn(1024L);
    when(binaryBlob.getContentType()).thenReturn("application/zip");
    when(binaryBlob.getUpdateTime()).thenReturn(0L);
  }

  @Test
  public void testEmptyBucket_noBucketContentTable() {
    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(0);
    assertThat(bucketTable.getRowCount()).isEqualTo(0);
  }

  @Test
  public void testBucketEditorColumnHeaders() {
    initBlobEditor(directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(4);

    IntStream.range(0, bucketTable.getColumnCount())
        .forEach(
            colIdx ->
                assertThat(indexToColName.get(colIdx))
                    .isEqualTo(bucketTable.getColumnName(colIdx)));
  }

  @Test
  public void testBucketContent_singleDirectoryBlob() {
    initBlobEditor(directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Name"))).isEqualTo("my_directory");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Size"))).isEqualTo("-");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Type"))).isEqualTo("Folder");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Last Modified"))).isEqualTo("-");
  }

  @Test
  public void testBucketContent_singleBinaryBlob() {
    initBlobEditor(binaryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Name"))).isEqualTo("my_blob.zip");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Size"))).isEqualTo("1.0 KB");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Type"))).isEqualTo("application/zip");
    assertThat(bucketTable.getValueAt(0, colNameToIndex.get("Last Modified")))
        .isEqualTo("1/1/70 12:00 AM");
  }

  @Test
  public void testBuckets_mixedBinaryAndDirectoryBlobs() {
    initBlobEditor(binaryBlob, directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(2);
  }

  private void initBlobEditor(Blob... blobs) {
    List<Blob> blobList = Lists.newArrayList(blobs);
    Page<Blob> blobPage = bucketVirtualFile.getBucket().list();
    when(blobPage.iterateAll()).thenReturn(blobList);

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();
  }
}
