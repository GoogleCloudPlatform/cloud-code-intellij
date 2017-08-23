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

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.tools.intellij.googleapis.GoogleApiFactory;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * UI definition for the Google Cloud Storage panel. Displays the panel components and invokes the
 * Storage API to load project buckets.
 */
final class GcsBucketPanel {

  private JPanel gcsBucketPanel;
  private ProjectSelector projectSelector;
  private JPanel notificationPanel;
  private JPanel bucketListPanel;
  private JLabel notificationLabel;
  private JList<Bucket> bucketList;
  private DefaultListModel<Bucket> bucketListModel;

  GcsBucketPanel(@NotNull Project project) {
    bucketListModel = new DefaultListModel<>();
    bucketList.setModel(bucketListModel);
    bucketList.setCellRenderer(new GcsBucketCellRenderer());
    bucketList.setFixedCellHeight(25);
    bucketList.setBackground(bucketListPanel.getBackground());

    projectSelector.getDocument().addDocumentListener(new GcsProjectChangeListener());
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

  private class GcsProjectChangeListener extends DocumentAdapter {

    @Override
    protected void textChanged(DocumentEvent event) {
      showNotificationPanel(true);

      if (StringUtils.isEmpty(projectSelector.getText())) {
        notificationLabel.setText(
            GctBundle.message("gcs.panel.bucket.listing.no.project.selected"));
      } else {
        String projectId = projectSelector.getText();
        CredentialedUser user = projectSelector.getSelectedUser();

        if (user != null) {
          loadAndDisplayBuckets(projectId, user.getCredential());
        } else {
          notificationLabel.setText(
              GctBundle.message("gcs.panel.bucket.listing.error.loading.buckets"));
        }
      }
    }

    private void loadAndDisplayBuckets(String projectId, Credential credential) {
      bucketListModel.clear();
      notificationLabel.setText(GctBundle.message("gcs.panel.bucket.listing.loading.text"));

      ApplicationManager.getApplication()
          .executeOnPooledThread(
              () -> {
                Storage storage =
                    GoogleApiFactory.getInstance().newStorageApi(projectId, credential);

                try {
                  Iterable<Bucket> buckets = storage.list().iterateAll();
                  for (Bucket bucket : buckets) {
                    bucketListModel.addElement(bucket);
                  }

                  showNotificationPanel(false);
                } catch (StorageException se) {
                  notificationLabel.setText(
                      GctBundle.message("gcs.panel.bucket.listing.error.loading.buckets"));
                }
              });
    }

    private void showNotificationPanel(boolean show) {
      notificationPanel.setVisible(show);
      bucketListPanel.setVisible(!show);
    }


  }
}
