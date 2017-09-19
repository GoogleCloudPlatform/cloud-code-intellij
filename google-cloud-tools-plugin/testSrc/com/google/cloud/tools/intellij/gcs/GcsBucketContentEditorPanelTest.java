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

  private static final String DIR_NAME = "my_directory";
  private static final String BLOB_NAME = "my_blob.zip";
  private static final String BLOB_CONTENT_TYPE = "application/zip";
  private static final String NESTED_BLOB_FULL_NAME = "dir1/dir2/nested_blob.zip";
  private static final String NESTED_BLOB_NAME = "nested_blob.zip";

  private static final BiMap<Integer, String> INDEX_TO_COL_NAME =
      HashBiMap.create(ImmutableMap.of(0, "Name", 1, "Size", 2, "Type", 3, "Last Modified"));
  private static final Map<String, Integer> COL_NAME_TO_INDEX = INDEX_TO_COL_NAME.inverse();

  private GcsBucketContentEditorPanel editorPanel;
  private GcsBucketVirtualFile bucketVirtualFile;

  @Mock private Blob directoryBlob;
  @Mock private Blob binaryBlob;
  @Mock private Blob nestedBlob;

  @Before
  public void setUp() {
    bucketVirtualFile = GcsTestUtils.createVirtualFileWithBucketMocks();

    when(directoryBlob.isDirectory()).thenReturn(true);
    when(directoryBlob.getName()).thenReturn(DIR_NAME);

    when(binaryBlob.isDirectory()).thenReturn(false);
    when(binaryBlob.getName()).thenReturn(BLOB_NAME);
    when(binaryBlob.getSize()).thenReturn(1024L);
    when(binaryBlob.getContentType()).thenReturn(BLOB_CONTENT_TYPE);
    when(binaryBlob.getUpdateTime()).thenReturn(0L);

    when(nestedBlob.getName()).thenReturn(NESTED_BLOB_FULL_NAME);
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
                assertThat(INDEX_TO_COL_NAME.get(colIdx))
                    .isEqualTo(bucketTable.getColumnName(colIdx)));
  }

  @Test
  public void testBucketContent_singleDirectoryBlob() {
    initBlobEditor(directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Name"))).isEqualTo(DIR_NAME);
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("-");
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Type"))).isEqualTo("Folder");
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Last Modified"))).isEqualTo("-");
  }

  @Test
  public void testBucketContent_singleBinaryBlob() {
    initBlobEditor(binaryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Name"))).isEqualTo(BLOB_NAME);
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("1.0 KB");
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Type")))
        .isEqualTo(BLOB_CONTENT_TYPE);
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Last Modified")))
        .isEqualTo("1/1/70 12:00 AM");
  }

  @Test
  public void testBuckets_mixedBinaryAndDirectoryBlobs() {
    initBlobEditor(binaryBlob, directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(2);
  }

  @Test
  public void testBucketName_directoryPrefixIsTrimmed() {
    initBlobEditor(nestedBlob);
    editorPanel.updateTableModel("dir1/dir2/");

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Name")))
        .isEqualTo(NESTED_BLOB_NAME);
  }

  @Test
  public void testBlobSizeDisplay_Bytes() {
    when(binaryBlob.getSize()).thenReturn(100L);
    initBlobEditor(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100 B");
  }

  @Test
  public void testBlobSizeDisplay_KB() {
    when(binaryBlob.getSize()).thenReturn(102400L);
    initBlobEditor(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 KB");
  }

  @Test
  public void testBlobSizeDisplay_MB() {
    when(binaryBlob.getSize()).thenReturn(104857600L);
    initBlobEditor(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 MB");
  }

  @Test
  public void testBlobSizeDisplay_GB() {
    when(binaryBlob.getSize()).thenReturn(107374182400L);
    initBlobEditor(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 GB");
  }

  private void initBlobEditor(Blob... blobs) {
    List<Blob> blobList = Lists.newArrayList(blobs);
    Page<Blob> blobPage = bucketVirtualFile.getBucket().list();
    when(blobPage.iterateAll()).thenReturn(blobList);

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();
  }
}
