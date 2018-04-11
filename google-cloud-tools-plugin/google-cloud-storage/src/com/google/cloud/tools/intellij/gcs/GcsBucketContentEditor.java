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

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Key;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Google Cloud Storage {@link FileEditor} implementation. Instantiates the UI panel that allows
 * browsing of GCS bucket contents.
 */
final class GcsBucketContentEditor implements FileEditor {

  private static final String GCS_FILE_EDITOR_NAME = "gcs-bucket-content";

  private final GcsBucketContentEditorPanel bucketContentPanel;
  private final GcsBucketVirtualFile bucketVirtualFile;

  GcsBucketContentEditor(@NotNull GcsBucketVirtualFile bucketVirtualFile) {
    this.bucketVirtualFile = bucketVirtualFile;
    bucketContentPanel = new GcsBucketContentEditorPanel(bucketVirtualFile.getBucket());
    bucketContentPanel.initTableModel();
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return bucketContentPanel.getComponent();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return getComponent();
  }

  @NotNull
  @Override
  public String getName() {
    return GCS_FILE_EDITOR_NAME;
  }

  @Override
  public void setState(@NotNull FileEditorState state) {}

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return bucketVirtualFile.isValid();
  }

  @Override
  public void selectNotify() {}

  @Override
  public void deselectNotify() {}

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Override
  public void dispose() {}

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {}
}
