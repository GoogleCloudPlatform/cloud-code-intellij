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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.JTextPane;
import org.apache.commons.lang.StringUtils;

/**
 * Custom {@link JTextPane} implementation for breadcrumb navigation representing the bucket folder
 * hierarchy leading up to the current directory. Renders the directory elements and navigable links
 * to individual directories.
 */
public class GcsBreadcrumbsTextPane extends JTextPane {
  private static final String BREADCRUMB_SEPARATOR = " / ";
  private static final String DIRECTORY_SEPARATOR = "/";

  private String currentDirectoryPath;

  /**
   * See {@link #render(String, String)}. Renders the root directory of the breadcrumb navigation
   * display.
   */
  void render(String bucketName) {
    render(bucketName, "");
  }

  /**
   * Renders the breadcrumb navigation display.
   *
   * <p>The display consists of the bucket name (linking to the root directory of the bucket)
   * followed by each directory leading up to the current directory. For example:
   *
   * <p>"bucket_name / parent_dir_1 / parent_dir_2 / current_dir"
   */
  void render(String bucketName, String directoryPath) {
    currentDirectoryPath = directoryPath;

    String labelText =
        "<html><font face='sans' size='-1'>"
            + getBucketNameForDisplay(bucketName, directoryPath)
            + getDirectoryPathForDisplay(directoryPath)
            + "</font></html>";
    setText(labelText);
  }

  String getCurrentDirectoryPath() {
    return currentDirectoryPath;
  }

  private static String getBucketNameForDisplay(String bucketName, String directoryPath) {
    return StringUtils.isNotEmpty(directoryPath) ? buildLink(bucketName, "") : bucketName;
  }

  private static String getDirectoryPathForDisplay(String directoryPath) {
    if (StringUtils.isEmpty(directoryPath)) {
      return "";
    }

    List<String> directoryPathList = Arrays.asList(directoryPath.split(DIRECTORY_SEPARATOR));
    return BREADCRUMB_SEPARATOR
        + IntStream.range(0, directoryPathList.size())
            .mapToObj(
                idx -> {
                  String directoryName = directoryPathList.get(idx);
                  if (idx == directoryPathList.size() - 1) {
                    return directoryName;
                  } else {
                    return buildLink(directoryName, getSubdirectoryPath(idx, directoryPathList));
                  }
                })
            .collect(Collectors.joining(BREADCRUMB_SEPARATOR));
  }

  private static String getSubdirectoryPath(int directoryIdx, List<String> fullPath) {
    return IntStream.range(0, directoryIdx + 1)
            .mapToObj(fullPath::get)
            .collect(Collectors.joining(DIRECTORY_SEPARATOR))
        + DIRECTORY_SEPARATOR;
  }

  private static String buildLink(String name, String link) {
    return "<a href=\"" + link + "\">" + name + "</a>";
  }
}
