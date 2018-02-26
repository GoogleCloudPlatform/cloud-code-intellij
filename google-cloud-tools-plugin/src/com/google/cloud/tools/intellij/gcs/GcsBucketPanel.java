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

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.ui.CopyToClipboardActionListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;

/**
 * UI definition for the Google Cloud Storage panel. Displays the panel components and invokes the
 * Storage API to load project buckets.
 */
final class GcsBucketPanel {

  private static final Logger log = Logger.getInstance(GcsBucketPanel.class);

  private final Project project;

  private JPanel gcsBucketPanel;
  private ProjectSelector projectSelector;
  private JPanel notificationPanel;
  private JPanel bucketListPanel;
  private JLabel notificationLabel;
  private JList<Bucket> bucketList;
  private DefaultListModel<Bucket> bucketListModel;
  private Future<?> bucketLoadExecution;

  GcsBucketPanel(@NotNull Project project) {
    this.project = project;
    bucketListModel = new DefaultListModel<>();
    bucketList.setModel(bucketListModel);
    bucketList.setCellRenderer(new GcsBucketCellRenderer());
    bucketList.setFixedCellHeight(25);
    bucketList.setBackground(bucketListPanel.getBackground());

    projectSelector.addProjectSelectionListener((selected) -> refresh());

    bucketList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
              int index = bucketList.locationToIndex(event.getPoint());
              Bucket clickedBucket = bucketListModel.getElementAt(index);

              if (clickedBucket != null) {
                loadBucketContents(clickedBucket);
              }
            }
          }
        });

    bucketList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent event) {
            if (SwingUtilities.isRightMouseButton(event)) {
              JList source = (JList) event.getSource();
              source.setSelectedIndex(source.locationToIndex(event.getPoint()));

              showRightClickMenu(event);
            }
          }
        });

    projectSelector.loadActiveCloudProject();
    refresh();
  }

  void refresh() {
    showNotificationPanel();

    if (!Services.getLoginService().isLoggedIn()) {
      notificationLabel.setText(GctBundle.message("gcs.panel.bucket.listing.not.logged.in"));
    } else if (projectSelector.getSelectedProject() == null) {
      notificationLabel.setText(GctBundle.message("gcs.panel.bucket.listing.no.project.selected"));
    } else {
      CloudProject cloudProject = projectSelector.getSelectedProject();
      Optional<CredentialedUser> user =
          Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());

      if (user.isPresent()) {
        loadAndDisplayBuckets(cloudProject.projectId(), user.get());
      } else {
        notificationLabel.setText(
            GctBundle.message("gcs.panel.bucket.listing.error.loading.buckets"));
        log.warn("Cloud not load credentialed user for GCS operation. User may not be logged.");
      }
    }
  }

  @NotNull
  JPanel getComponent() {
    return gcsBucketPanel;
  }

  @VisibleForTesting
  ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  @VisibleForTesting
  JPanel getNotificationPanel() {
    return notificationPanel;
  }

  @VisibleForTesting
  JPanel getBucketListPanel() {
    return bucketListPanel;
  }

  @VisibleForTesting
  JLabel getNotificationLabel() {
    return notificationLabel;
  }

  @VisibleForTesting
  void setProjectSelector(ProjectSelector projectSelector) {
    this.projectSelector = projectSelector;
  }

  private void createUIComponents() {
    projectSelector = new ProjectSelector(project);
  }

  private void loadAndDisplayBuckets(String projectId, CredentialedUser credentialedUser) {
    if (bucketLoadExecution != null && !bucketLoadExecution.isDone()) {
      return;
    }

    UsageTrackerProvider.getInstance().trackEvent(GctTracking.GCS_BUCKET_LIST).ping();

    bucketListModel.clear();
    notificationLabel.setText(GctBundle.message("gcs.panel.bucket.listing.loading.text"));

    bucketLoadExecution =
        ApplicationManager.getApplication()
            .executeOnPooledThread(
                () -> {
                  Storage storage =
                      GoogleApiClientFactory.getInstance()
                          .getCloudStorageApiClient(projectId, credentialedUser);

                  try {
                    Iterable<Bucket> buckets = storage.list().iterateAll();

                    if (Iterators.size(buckets.iterator()) == 0) {
                      notificationLabel.setText(
                          GctBundle.message("gcs.panel.bucket.listing.no.buckets.found"));
                      return;
                    }

                    for (Bucket bucket : buckets) {
                      bucketListModel.addElement(bucket);
                    }

                    showBucketListPanel();
                  } catch (StorageException se) {
                    notificationLabel.setText(
                        GctBundle.message("gcs.panel.bucket.listing.error.loading.buckets"));

                    UsageTrackerProvider.getInstance()
                        .trackEvent(GctTracking.GCS_BUCKET_LIST_EXCEPTION)
                        .ping();
                    log.warn(
                        "StorageException when performing GCS bucket list operation, with message: "
                            + se.getMessage());
                  }
                });
  }

  private void loadBucketContents(@NotNull Bucket bucket) {
    FileEditorManager editorManager = FileEditorManager.getInstance(project);

    editorManager.openEditor(
        new OpenFileDescriptor(
            project,
            findOpenGcsVirtualFile(bucket).orElseGet(() -> new GcsBucketVirtualFile(bucket)),
            1 /*offset*/),
        true /*focusEditor*/);
  }

  /**
   * Searches for an open editor who's {@link VirtualFile} matches the bucket that was opened. This
   * prevents the same editor window being opened multiple times for the same bucket.
   */
  private Optional<GcsBucketVirtualFile> findOpenGcsVirtualFile(@NotNull Bucket bucket) {
    FileEditorManager editorManager = FileEditorManager.getInstance(project);

    return Arrays.stream(editorManager.getOpenFiles())
        .filter(openFile -> openFile instanceof GcsBucketVirtualFile)
        .map(openGcsFile -> (GcsBucketVirtualFile) openGcsFile)
        .filter(openGcsFile -> openGcsFile.getBucket().equals(bucket))
        .findFirst();
  }

  private void showNotificationPanel() {
    notificationPanel.setVisible(true);
    bucketListPanel.setVisible(false);
  }

  private void showBucketListPanel() {
    bucketListPanel.setVisible(true);
    notificationPanel.setVisible(false);
  }

  private void showRightClickMenu(MouseEvent event) {
    JPopupMenu rightClickMenu = new JPopupMenu();
    JMenuItem copyBucketNameMenuItem =
        new JMenuItem(GctBundle.message("gcs.content.explorer.right.click.menu.copy.bucket.text"));
    rightClickMenu.add(copyBucketNameMenuItem);

    int index = bucketList.locationToIndex(event.getPoint());
    Bucket bucket = bucketListModel.getElementAt(index);

    if (bucket != null) {
      copyBucketNameMenuItem.addActionListener(
          e ->
              UsageTrackerProvider.getInstance()
                  .trackEvent(GctTracking.GCS_BUCKET_LIST_ACTION_COPY_BUCKET_NAME)
                  .ping());
      copyBucketNameMenuItem.addActionListener(new CopyToClipboardActionListener(bucket.getName()));
      rightClickMenu.show(event.getComponent(), event.getX(), event.getY());
    }
  }
}
