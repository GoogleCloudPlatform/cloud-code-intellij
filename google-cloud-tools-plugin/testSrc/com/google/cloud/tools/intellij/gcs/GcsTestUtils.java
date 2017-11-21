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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage.BlobListOption;
import java.util.Iterator;

/** Utilities for GCS related tests. */
final class GcsTestUtils {

  static GcsBucketVirtualFile setupVirtualFileWithBucketMocks(
      GcsBucketVirtualFile gcsBucketVirtualFile) {
    Bucket bucket = mock(Bucket.class);
    Page<Blob> page = mock(Page.class);
    Iterable<Blob> blobIterable = mock(Iterable.class);
    Iterator<Blob> blobIterator = mock(Iterator.class);

    when(gcsBucketVirtualFile.getBucket()).thenReturn(bucket);
    when(bucket.list()).thenReturn(page);
    when(bucket.list(any(BlobListOption.class), any(BlobListOption.class))).thenReturn(page);
    when(page.iterateAll()).thenReturn(blobIterable);
    when(blobIterable.iterator()).thenReturn(blobIterator);

    return gcsBucketVirtualFile;
  }
}
