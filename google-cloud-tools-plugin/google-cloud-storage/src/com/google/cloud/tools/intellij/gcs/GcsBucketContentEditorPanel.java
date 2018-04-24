/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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
import com.google.cloud.storage.StorageException;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerService;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.ui.CopyToClipboardActionListener;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import org.jetbrains.annotations.NotNull;

/** Defines the Google Cloud Storage bucket content browsing UI panel. */
public final class GcsBucketContentEditorPanel {
  private static final Logger log = Logger.getInstance(GcsBucketContentEditorPanel.class);

  private final Bucket bucket;

  private JPanel bucketContentEditorPanel;
  private JTable bucketContentTable;
  private JButton refreshButton;
  private GcsBreadcrumbsTextPane breadcrumbs;
  private JLabel messageLabel;
  private JPanel messagePanel;
  private JScrollPane bucketContentScrollPane;
  private JPanel loadingPanel;
  private JPanel errorPanel;
  private GcsBlobTableModel tableModel;

  GcsBucketContentEditorPanel(@NotNull Bucket bucket) {
    this.bucket = bucket;

    bucketContentTable.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2 && tableModel != null) {
              Blob selectedBlob =
                  tableModel.getBlobAt(bucketContentTable.rowAtPoint(event.getPoint()));

              if (selectedBlob != null && selectedBlob.isDirectory()) {
                updateTableModel(selectedBlob.getName());
              }
            }
          }
        });

    bucketContentTable.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent event) {
            if (SwingUtilities.isRightMouseButton(event)) {
              JTable source = (JTable) event.getSource();
              int row = source.rowAtPoint(event.getPoint());
              int col = source.columnAtPoint(event.getPoint());

              if (!source.isRowSelected(row)) {
                source.changeSelection(row, col, false, false);
              }

              showRightClickMenu(event);
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

    JTableHeader tableHeader = bucketContentTable.getTableHeader();
    Font tableHeaderFont = tableHeader.getFont();
    tableHeader.setFont(
        new Font(tableHeaderFont.getFontName(), Font.BOLD, tableHeaderFont.getSize()));
    tableHeader.setAlignmentX(JLabel.LEFT);
  }

  void initTableModel() {
    if (!Services.getLoginService().isLoggedIn()) {
      showMessage(
          GoogleCloudStorageMessageBundle.message("gcs.content.explorer.not.logged.in.text"));
      return;
    }

    UsageTrackerService.getInstance().trackEvent(GctTracking.GCS_BLOB_BROWSE).ping();

    Consumer<List<Blob>> afterLoad =
        blobs -> {
          if (blobs.isEmpty()) {
            showMessage(
                GoogleCloudStorageMessageBundle.message("gcs.content.explorer.empty.bucket.text"));
          } else {
            showBlobTable();
            tableModel = new GcsBlobTableModel();
            tableModel.setDataVector(blobs, "");
            bucketContentTable.setModel(tableModel);
          }
        };

    breadcrumbs.render(bucket.getName());
    loadBlobsStartingWith("", afterLoad);
  }

  void updateTableModel(String prefix) {
    if (!Services.getLoginService().isLoggedIn()) {
      showMessage(
          GoogleCloudStorageMessageBundle.message("gcs.content.explorer.not.logged.in.text"));
      return;
    }

    if (tableModel == null) {
      initTableModel();
      return;
    }

    UsageTrackerService.getInstance().trackEvent(GctTracking.GCS_BLOB_BROWSE).ping();

    tableModel.setRowCount(0);

    Consumer<List<Blob>> afterLoad =
        blobs -> {
          if (isEmptyDirectory(prefix, blobs)) {
            String message =
                prefix.isEmpty()
                    ? GoogleCloudStorageMessageBundle.message(
                        "gcs.content.explorer.empty.bucket.text")
                    : GoogleCloudStorageMessageBundle.message(
                        "gcs.content.explorer.empty.directory.text");
            showMessage(message);
          } else {
            showBlobTable();

            tableModel.setDataVector(blobs, prefix);
            tableModel.fireTableDataChanged();
          }
        };

    breadcrumbs.render(bucket.getName(), prefix);
    loadBlobsStartingWith(prefix, afterLoad);
  }

  private void showRightClickMenu(MouseEvent event) {
    JPopupMenu rightClickMenu = new JPopupMenu();
    JMenuItem copyBlobNameMenuItem =
        new JMenuItem(
            GoogleCloudStorageMessageBundle.message(
                "gcs.content.explorer.right.click.menu.copy.blob.text"));
    JMenuItem copyBucketNameMenuItem =
        new JMenuItem(
            GoogleCloudStorageMessageBundle.message(
                "gcs.content.explorer.right.click.menu.copy.bucket.text"));
    rightClickMenu.add(copyBlobNameMenuItem);
    rightClickMenu.add(copyBucketNameMenuItem);

    Blob selectedBlob = tableModel.getBlobAt(bucketContentTable.rowAtPoint(event.getPoint()));

    if (selectedBlob != null) {
      copyBlobNameMenuItem.addActionListener(
          e ->
              UsageTrackerService.getInstance()
                  .trackEvent(GctTracking.GCS_BLOB_BROWSE_ACTION_COPY_BLOB_NAME)
                  .ping());
      copyBlobNameMenuItem.addActionListener(
          new CopyToClipboardActionListener(selectedBlob.getName()));

      copyBucketNameMenuItem.addActionListener(
          e ->
              UsageTrackerService.getInstance()
                  .trackEvent(GctTracking.GCS_BLOB_BROWSE_ACTION_COPY_BUCKET_NAME)
                  .ping());
      copyBucketNameMenuItem.addActionListener(
          new CopyToClipboardActionListener(selectedBlob.getBucket()));

      rightClickMenu.show(event.getComponent(), event.getX(), event.getY());
    }
  }

  private void showMessage(String message) {
    bucketContentScrollPane.setVisible(false);
    messagePanel.setVisible(true);
    messageLabel.setText(message);
  }

  private void showBlobTable() {
    messagePanel.setVisible(false);
    bucketContentScrollPane.setVisible(true);
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
  @SuppressWarnings("FutureReturnValueIgnored")
  private void loadBlobsStartingWith(String prefix, Consumer<List<Blob>> afterLoad) {
    showLoader();
    hideError();
    ThreadUtil.getInstance()
        .executeInBackground(
            () -> {
              List<Blob> blobs;
              try {
                blobs =
                    Lists.newArrayList(
                        bucket
                            .list(BlobListOption.currentDirectory(), BlobListOption.prefix(prefix))
                            .iterateAll());
              } catch (StorageException se) {
                ApplicationManager.getApplication()
                    .invokeAndWait(
                        () -> {
                          hideLoader();
                          showError();

                          UsageTrackerService.getInstance()
                              .trackEvent(GctTracking.GCS_BLOB_BROWSE_EXCEPTION)
                              .ping();
                          log.warn(
                              "StorageException when performing GCS blob list operation, with message: "
                                  + se.getMessage());
                        });
                return;
              }

              final List<Blob> loadedBlobs = blobs;
              ApplicationManager.getApplication()
                  .invokeAndWait(
                      () -> {
                        hideLoader();
                        afterLoad.accept(loadedBlobs);
                      });
            });
  }

  /**
   * Tests if a given directory prefix is empty. A directory is empty if there are no blobs or if
   * the only blob matches the current directory prefix.
   */
  private static boolean isEmptyDirectory(String prefix, List<Blob> blobs) {
    return blobs.isEmpty() || (blobs.size() == 1 && blobs.get(0).getName().equals(prefix));
  }

  private void showLoader() {
    loadingPanel.setVisible(true);
    bucketContentScrollPane.setVisible(false);
    messagePanel.setVisible(false);
  }

  private void hideLoader() {
    loadingPanel.setVisible(false);
  }

  private void showError() {
    errorPanel.setVisible(true);
  }

  private void hideError() {
    errorPanel.setVisible(false);
  }

  JPanel getComponent() {
    return bucketContentEditorPanel;
  }

  @VisibleForTesting
  JTable getBucketContentTable() {
    return bucketContentTable;
  }

  @VisibleForTesting
  JScrollPane getBucketContentScrollPane() {
    return bucketContentScrollPane;
  }

  @VisibleForTesting
  JLabel getMessageLabel() {
    return messageLabel;
  }

  @VisibleForTesting
  JPanel getLoadingPanel() {
    return loadingPanel;
  }

  @VisibleForTesting
  JPanel getMessagePanel() {
    return messagePanel;
  }

  @VisibleForTesting
  JPanel getErrorPanel() {
    return errorPanel;
  }

  @VisibleForTesting
  public GcsBreadcrumbsTextPane getBreadcrumbs() {
    return breadcrumbs;
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
