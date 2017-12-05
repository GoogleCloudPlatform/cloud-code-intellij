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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageException;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.DelayedSubmitExecutorServiceProxy;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.testing.TimeZoneRule;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.IntStream;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link GcsBucketContentEditorPanel}. */
public class GcsBucketContentEditorPanelTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @Rule public final TimeZoneRule timeZoneRule = new TimeZoneRule(TimeZone.getTimeZone("GMT"));

  private static final String DIR_NAME = "dir1/dir2/";
  private static final String BLOB_NAME = "my_blob.zip";
  private static final String BLOB_CONTENT_TYPE = "application/zip";
  private static final String NESTED_BLOB_FULL_NAME = "dir1/dir2/nested_blob.zip";
  private static final String NESTED_BLOB_NAME = "nested_blob.zip";

  private static final BiMap<Integer, String> INDEX_TO_COL_NAME =
      HashBiMap.create(ImmutableMap.of(0, "Name", 1, "Size", 2, "Type", 3, "Last Modified"));
  private static final Map<String, Integer> COL_NAME_TO_INDEX = INDEX_TO_COL_NAME.inverse();

  private GcsBucketContentEditorPanel editorPanel;

  @TestService @Mock private IntegratedGoogleLoginService loginService;
  @Mock private Blob binaryBlob;
  @Mock private Blob directoryBlob;
  @Mock private Blob binaryBlobInDirectory;
  @Mock private GcsBucketVirtualFile bucketVirtualFile;

  @Before
  public void setUp() {
    GcsTestUtils.setupVirtualFileWithBucketMocks(bucketVirtualFile);

    when(loginService.isLoggedIn()).thenReturn(true);

    when(directoryBlob.isDirectory()).thenReturn(true);
    when(directoryBlob.getName()).thenReturn(DIR_NAME);

    when(binaryBlob.isDirectory()).thenReturn(false);
    when(binaryBlob.getName()).thenReturn(BLOB_NAME);
    when(binaryBlob.getSize()).thenReturn(1024L);
    when(binaryBlob.getContentType()).thenReturn(BLOB_CONTENT_TYPE);
    when(binaryBlob.getUpdateTime()).thenReturn(0L);

    when(binaryBlobInDirectory.getName()).thenReturn(NESTED_BLOB_FULL_NAME);
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
  public void testLoginMessageShown_whenLoggedOut() {
    when(loginService.isLoggedIn()).thenReturn(false);
    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    assertTrue(editorPanel.getMessagePanel().isVisible());
    assertThat(editorPanel.getMessageLabel().getText())
        .isEqualTo("To view bucket contents log in to your Google Cloud Platform account");
  }

  @Test
  @SuppressWarnings("FutureReturnValueIgnored")
  public void testLoadingMessageShown_whenLoadingBuckets() {
    DelayedSubmitExecutorServiceProxy delayedExecutor = setDelayedExecutorService();

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    assertTrue(editorPanel.getLoadingPanel().isVisible());

    delayedExecutor.doSubmit();

    assertFalse(editorPanel.getLoadingPanel().isVisible());
  }

  @Test
  public void testBucketEditorColumnHeaders() {
    initEditorWithBlobs(directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(4);

    IntStream.range(0, bucketTable.getColumnCount())
        .forEach(
            colIdx ->
                assertThat(INDEX_TO_COL_NAME.get(colIdx))
                    .isEqualTo(bucketTable.getColumnName(colIdx)));
  }

  @Test
  public void testBucketBreadCrumbsInit() {
    setDelayedExecutorService();

    initEditorWithBlobs(directoryBlob);

    assertTrue(editorPanel.getBreadcrumbs().isVisible());
    assertFalse(editorPanel.getBreadcrumbs().getText().isEmpty());
  }

  @Test
  public void testBucketContent_singleDirectoryBlob() {
    initEditorWithBlobs(directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(1);

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Name"))).isEqualTo(DIR_NAME);
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("-");
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Type"))).isEqualTo("Folder");
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Last Modified"))).isEqualTo("-");
  }

  @Test
  public void testBucketContent_singleBinaryBlob() {
    initEditorWithBlobs(binaryBlob);

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
    initEditorWithBlobs(binaryBlob, directoryBlob);

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getRowCount()).isEqualTo(2);
  }

  @Test
  public void testBucketName_directoryPrefixIsTrimmed() {
    initEditorWithBlobs(binaryBlobInDirectory);
    editorPanel.updateTableModel("dir1/dir2/");

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Name")))
        .isEqualTo(NESTED_BLOB_NAME);
  }

  @Test
  public void testUpdateEmptyBucket_toNonEmptyBucket() {
    // setup empty bucket
    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    JScrollPane bucketScrollPane = editorPanel.getBucketContentScrollPane();
    JPanel noBlobsPanel = editorPanel.getMessagePanel();
    JLabel noBlobsLabel = editorPanel.getMessageLabel();

    assertFalse(bucketScrollPane.isVisible());
    assertTrue(noBlobsPanel.isVisible());
    assertThat(noBlobsLabel.getText()).isEqualTo("No files or directories found in this bucket");

    // add blobs and update bucket
    setBlobs(binaryBlob);
    editorPanel.updateTableModel("");

    assertTrue(bucketScrollPane.isVisible());
    assertFalse(noBlobsPanel.isVisible());
  }

  @Test
  public void testUpdateNonEmptyBucket_toEmptyBucket() {
    // setup bucket with blobs
    initEditorWithBlobs(binaryBlob);

    JScrollPane bucketScrollPane = editorPanel.getBucketContentScrollPane();
    JPanel noBlobsPanel = editorPanel.getMessagePanel();
    JLabel noBlobsLabel = editorPanel.getMessageLabel();

    assertTrue(bucketScrollPane.isVisible());
    assertFalse(noBlobsPanel.isVisible());

    // remove blobs and update
    setBlobs();
    editorPanel.updateTableModel("");

    assertFalse(bucketScrollPane.isVisible());
    assertTrue(noBlobsPanel.isVisible());
    assertThat(noBlobsLabel.getText()).isEqualTo("No files or directories found in this bucket");
  }

  @Test
  public void testUpdateEmptyDirectory_toNonEmptyDirectory() {
    // Create an empty directory and move to it
    initEditorWithBlobs(directoryBlob);
    editorPanel.updateTableModel(DIR_NAME);

    JScrollPane bucketScrollPane = editorPanel.getBucketContentScrollPane();
    JPanel noBlobsPanel = editorPanel.getMessagePanel();
    JLabel noBlobsLabel = editorPanel.getMessageLabel();

    assertFalse(bucketScrollPane.isVisible());
    assertTrue(noBlobsPanel.isVisible());
    assertThat(noBlobsLabel.getText()).isEqualTo("No files found in this directory");

    // Add a blob to the directory and update
    setBlobs(binaryBlobInDirectory);
    editorPanel.updateTableModel(DIR_NAME);

    assertTrue(bucketScrollPane.isVisible());
    assertFalse(noBlobsPanel.isVisible());
  }

  @Test
  public void testUpdateNonEmptyDirectory_toEmptyDirectory() {
    // setup bucket directory with blobs
    initEditorWithBlobs(directoryBlob, binaryBlobInDirectory);

    JScrollPane bucketScrollPane = editorPanel.getBucketContentScrollPane();
    JPanel noBlobsPanel = editorPanel.getMessagePanel();
    JLabel noBlobsLabel = editorPanel.getMessageLabel();

    assertTrue(bucketScrollPane.isVisible());
    assertFalse(noBlobsPanel.isVisible());

    // remove blobs from the directory and update
    setBlobs(directoryBlob);
    editorPanel.updateTableModel(DIR_NAME);

    assertFalse(bucketScrollPane.isVisible());
    assertTrue(noBlobsPanel.isVisible());
    assertThat(noBlobsLabel.getText()).isEqualTo("No files found in this directory");
  }

  @Test
  public void testBlobSizeDisplay_Bytes() {
    when(binaryBlob.getSize()).thenReturn(100L);
    initEditorWithBlobs(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100 B");
  }

  @Test
  public void testBlobSizeDisplay_KB() {
    when(binaryBlob.getSize()).thenReturn(102400L);
    initEditorWithBlobs(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 KB");
  }

  @Test
  public void testBlobSizeDisplay_MB() {
    when(binaryBlob.getSize()).thenReturn(104857600L);
    initEditorWithBlobs(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 MB");
  }

  @Test
  public void testBlobSizeDisplay_GB() {
    when(binaryBlob.getSize()).thenReturn(107374182400L);
    initEditorWithBlobs(binaryBlob);
    JTable bucketTable = editorPanel.getBucketContentTable();

    assertThat(bucketTable.getValueAt(0, COL_NAME_TO_INDEX.get("Size"))).isEqualTo("100.0 GB");
  }

  @Test
  public void testBucketListException_showsErrorMessage() {
    when(bucketVirtualFile.getBucket().list(any(BlobListOption.class), any(BlobListOption.class)))
        .thenThrow(StorageException.class);

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    JTable bucketTable = editorPanel.getBucketContentTable();
    assertThat(bucketTable.getColumnCount()).isEqualTo(0);
    assertThat(bucketTable.getRowCount()).isEqualTo(0);
    assertFalse(editorPanel.getMessagePanel().isVisible());
    assertFalse(editorPanel.getLoadingPanel().isVisible());
    assertTrue(editorPanel.getErrorPanel().isVisible());
  }

  @Test
  public void testErrorMessageIsCleared_afterSuccessfulBucketList() {
    when(bucketVirtualFile.getBucket().list(any(BlobListOption.class), any(BlobListOption.class)))
        .thenThrow(StorageException.class);

    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();

    assertTrue(editorPanel.getErrorPanel().isVisible());

    // Re-initialize mocks so the exception is not thrown and update the UI
    reset(bucketVirtualFile.getBucket());
    editorPanel.updateTableModel("");

    assertFalse(editorPanel.getErrorPanel().isVisible());
  }

  private void initEditorWithBlobs(Blob... blobs) {
    setBlobs(blobs);
    editorPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    editorPanel.initTableModel();
  }

  private void setBlobs(Blob... blobs) {
    List<Blob> blobList = Lists.newArrayList(blobs);
    Page<Blob> blobPage = bucketVirtualFile.getBucket().list();
    when(blobPage.iterateAll()).thenReturn(blobList);
  }

  private DelayedSubmitExecutorServiceProxy setDelayedExecutorService() {
    DelayedSubmitExecutorServiceProxy delayedExecutor = new DelayedSubmitExecutorServiceProxy();
    ThreadUtil.getInstance().setBackgroundExecutorService(delayedExecutor);
    return delayedExecutor;
  }
}
