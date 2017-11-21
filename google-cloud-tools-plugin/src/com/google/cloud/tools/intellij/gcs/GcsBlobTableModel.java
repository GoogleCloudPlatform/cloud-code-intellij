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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.Nullable;

/**
 * Table model representation of a Google Cloud Storage blob row. Handles display of blob name and
 * metadata in the GCS content browser.
 */
final class GcsBlobTableModel extends DefaultTableModel {

  private List<Blob> blobs;

  private static final List<String> TABLE_COL_HEADER =
      Arrays.asList("Name", "Size", "Type", "Last Modified");

  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

  @Nullable
  Blob getBlobAt(int index) {
    if (blobs == null) {
      throw new IllegalStateException("Data vector with blob values not initialized.");
    }
    try {
      return blobs.get(index);
    } catch (IndexOutOfBoundsException ex) {
      return null;
    }
  }

  @SuppressWarnings("JdkObsolete")
  void setDataVector(List<Blob> blobs, String directoryPrefix) {
    this.blobs =
        blobs
            .stream()
            .filter(blob -> !blob.getName().equals(directoryPrefix))
            .collect(Collectors.toList());

    super.setDataVector(
        buildDataVector(this.blobs, directoryPrefix), new Vector<>(TABLE_COL_HEADER));
  }

  @SuppressWarnings("JdkObsolete")
  private static Vector<Vector<String>> buildDataVector(List<Blob> blobs, String directoryPrefix) {
    return blobs
        .stream()
        .map(
            blob -> {
              Vector<String> rowData = new Vector<>();
              rowData.add(getDirectoryTrimmedBlobName(blob.getName(), directoryPrefix));
              rowData.add(getBlobSize(blob));
              rowData.add(getBlobType(blob));
              rowData.add(getLastModifiedTimestamp(blob));
              return rowData;
            })
        .collect(Collectors.toCollection(Vector::new));
  }

  /**
   * Given an full blob name such as "dir1/dir2/blob.zip" and a directory prefix indicating which
   * directory we are examining such as "dir1/dir2/", this method will trim off the prefix and
   * return the blob name: "blob.zip".
   *
   * @param fullName the full path to the blob.
   * @param directoryPrefix the path indicating which virtual directory the blob is in.
   */
  private static String getDirectoryTrimmedBlobName(String fullName, String directoryPrefix) {
    String prefix = fullName.substring(0, directoryPrefix.length());
    if (!prefix.isEmpty() && prefix.equals(directoryPrefix)) {
      return fullName.substring(prefix.length());
    }
    return fullName;
  }

  private static String getBlobType(Blob blob) {
    return blob.isDirectory() ? "Folder" : blob.getContentType();
  }

  private static String getBlobSize(Blob blob) {
    return blob.isDirectory() ? "-" : toHumanReadableByteSize(blob.getSize());
  }

  private static String getLastModifiedTimestamp(Blob blob) {
    if (blob.isDirectory()) {
      return "-";
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy h:mm a");
    return dateFormat.format(new Date(blob.getUpdateTime()));
  }

  private static String toHumanReadableByteSize(long bytes) {
    int unit = 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
}
