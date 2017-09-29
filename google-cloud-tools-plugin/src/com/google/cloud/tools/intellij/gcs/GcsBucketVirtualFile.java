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
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link VirtualFile} representation of a Google Cloud Storage bucket.
 *
 * <p>This is essentially just a wrapper for a {@link Bucket} needed in order to create a custom
 * editor window for exploring GCS bucket contents. Custom editors are created by providing {@link
 * FileEditorProvider} implementations which are backed by VirtualFiles.
 */
class GcsBucketVirtualFile extends VirtualFile {

  private final Bucket bucket;

  GcsBucketVirtualFile(@NotNull Bucket bucket) {
    this.bucket = bucket;
  }

  Bucket getBucket() {
    return bucket;
  }

  @NotNull
  @Override
  public String getName() {
    return bucket.getName();
  }

  @NotNull
  @Override
  public VirtualFileSystem getFileSystem() {
    return new DummyFileSystem();
  }

  @NotNull
  @Override
  public String getPath() {
    return "/" + bucket.getName();
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return new GcsBucketFileType();
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public VirtualFile getParent() {
    return null;
  }

  @Override
  public VirtualFile[] getChildren() {
    return new VirtualFile[0];
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(
      Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    return new ByteArrayOutputStream();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray() throws IOException {
    return new byte[0];
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {}

  @Override
  public InputStream getInputStream() throws IOException {
    return null;
  }

  @Override
  public long getModificationStamp() {
    return 0L;
  }

  private static final class GcsBucketFileType implements FileType {

    @NotNull
    @Override
    public String getName() {
      return "gcs-bucket-file";
    }

    @NotNull
    @Override
    public String getDescription() {
      return "GCS Bucket Content File Type";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
      return "";
    }

    @Nullable
    @Override
    public Icon getIcon() {
      return GoogleCloudToolsIcons.CLOUD_STORAGE_BUCKET;
    }

    @Override
    public boolean isBinary() {
      return false;
    }

    @Override
    public boolean isReadOnly() {
      return false;
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
      return null;
    }
  }
}
