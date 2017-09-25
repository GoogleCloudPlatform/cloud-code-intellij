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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import org.jetbrains.annotations.NotNull;

/** Defines the Google Cloud Storage bucket content browsing UI panel. */
final class GcsBucketContentEditorPanel {

  private final Bucket bucket;

  private JPanel bucketContentEditorPanel;
  private JTable bucketContentTable;
  private JButton refreshButton;
  private GcsBreadcrumbsTextPane breadcrumbs;
  private GcsBlobTableModel tableModel;

  private static final Color MEDIUM_GRAY = new Color(96, 96, 96);

  GcsBucketContentEditorPanel(@NotNull Bucket bucket) {
    this.bucket = bucket;

    bucketContentTable.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2 && tableModel != null) {
              Blob selectedBlob =
                  tableModel.getBlobAt(bucketContentTable.rowAtPoint(event.getPoint()));

              if (selectedBlob.isDirectory()) {
                updateTableModel(selectedBlob.getName());
              }
            }
          }
        });

    refreshButton.addActionListener(
        event -> updateTableModel(breadcrumbs.getCurrentDirectoryPath()));

    breadcrumbs.addHyperlinkListener(
        event -> {
          if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String prefix = event.getDescription();
            updateTableModel(prefix);
          }
        });

    breadcrumbs.setBackground(bucketContentEditorPanel.getBackground());

    bucketContentTable.setRowHeight(23);
    bucketContentTable.setForeground(MEDIUM_GRAY);

    JTableHeader tableHeader = bucketContentTable.getTableHeader();
    Font tableHeaderFont = tableHeader.getFont();
    tableHeader.setFont(
        new Font(tableHeaderFont.getFontName(), Font.BOLD, tableHeaderFont.getSize()));
    tableHeader.setForeground(MEDIUM_GRAY);
    tableHeader.setAlignmentX(JLabel.LEFT);
  }

  void initTableModel() {
    List<Blob> blobs = getBlobsStartingWith("");
    if (!blobs.isEmpty()) {
      tableModel = new GcsBlobTableModel();
      tableModel.setDataVector(blobs, "");
      bucketContentTable.setModel(tableModel);
    }
    breadcrumbs.render(bucket.getName());
  }

  void updateTableModel(String prefix) {
    tableModel.setRowCount(0);
    tableModel.setDataVector(getBlobsStartingWith(prefix), prefix);
    tableModel.fireTableDataChanged();
    breadcrumbs.render(bucket.getName(), prefix);
  }

  /**
   * Returns the list of {@link Blob blobs} that start with the given prefix.
   *
   * <p>In the GCS backend there are no directories, just blobs with names. Implicit in the name,
   * however, is a directory structure (e.g. "dir1/dir2/blob.zip"). Therefore, in order to create a
   * directory browsing experience, we need to "unflatten" the blobs into a directory structure.
   *
   * <p>{@link Bucket#list(BlobListOption...)} provides options to help simulate directories by
   * supplying the {@link BlobListOption#currentDirectory()} and {@link
   * BlobListOption#prefix(String)} options. The prefix acts as the current directory for the blobs
   * we wish to fetch.
   */
  // TODO(eshaul) should be done asynchronously and show loader in the UI
  private List<Blob> getBlobsStartingWith(String prefix) {
    return Lists.newArrayList(
        bucket.list(BlobListOption.currentDirectory(), BlobListOption.prefix(prefix)).iterateAll());
  }

  JPanel getComponent() {
    return bucketContentEditorPanel;
  }

  @VisibleForTesting
  JTable getBucketContentTable() {
    return bucketContentTable;
  }

  private void createUIComponents() {
    bucketContentTable =
        new JTable() {
          @Override
          public TableCellRenderer getCellRenderer(int row, int column) {
            if (column == 0) {
              return new GcsBlobNameCellRenderer();
            }
            return super.getCellRenderer(row, column);
          }
        };
  }
}
