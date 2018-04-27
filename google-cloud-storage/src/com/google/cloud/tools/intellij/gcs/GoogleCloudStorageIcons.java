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

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class GoogleCloudStorageIcons {

  public static final Icon CLOUD_STORAGE = load("/icons/cloudStorage.png");
  public static final Icon CLOUD_STORAGE_BUCKET = load("/icons/cloudStorageBucket.png");

  private GoogleCloudStorageIcons() {
    // Not for instantiation.
  }

  private static Icon load(String path) {
    return IconLoader.getIcon(path, GoogleCloudStorageIcons.class);
  }

  private static Icon loadGif(String path) {
    return new ImageIcon(GoogleCloudStorageIcons.class.getResource(path));
  }
}
