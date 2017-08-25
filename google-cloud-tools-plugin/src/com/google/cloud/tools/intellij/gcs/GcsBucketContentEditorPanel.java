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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NotNull;

/** Defines the Google Cloud Storage bucket content browsing UI panel. */
// TODO(eshaul) work in progress
final class GcsBucketContentEditorPanel {

  private JPanel bucketContentEditorPanel;
  private JPanel bucketContentToolbarPanel;
  private JPanel breadCrumbsPanel;
  private JTable bucketContentTable;

  private static final List<String> TABLE_COL_HEADER =
      Arrays.asList("Name", "Size", "Type", "Last Modified");

  void setTableModel(@NotNull Iterable<Blob> blobs) {
    if (Iterators.size(blobs.iterator()) != 0) {
      bucketContentTable.setModel(new GcsBucketTableModel(blobs));
    }
  }

  JPanel getComponent() {
    return bucketContentEditorPanel;
  }

  @VisibleForTesting
  JTable getBucketContentTable() {
    return bucketContentTable;
  }

  private final class GcsBucketTableModel extends DefaultTableModel {

    GcsBucketTableModel(Iterable<Blob> blobs) {
      super();
      setDataVector(buildDataVector(blobs), new Vector<>(TABLE_COL_HEADER));
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }

    // TODO(eshaul) this is a placeholder implementation; need to complete.
    private Vector<Vector<String>> buildDataVector(Iterable<Blob> blobs) {
      return StreamSupport.stream(blobs.spliterator(), false)
          .map(
              blob -> {
                Vector<String> rowData = new Vector<>();
                rowData.add(blob.getName());
                rowData.add("--");
                rowData.add("--");
                rowData.add("--");
                return rowData;
              })
          .collect(Collectors.toCollection(Vector::new));
    }
  }
}
