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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.cloud.tools.managedcloudsdk.ProgressListener;

public class ManagedCloudSdkProgressListener implements ProgressListener {

  @Override
  public void start(String message, long totalWork) {}

  @Override
  public void update(long workDone) {}

  @Override
  public void update(String message) {}

  @Override
  public void done() {}

  @Override
  public ProgressListener newChild(long allocation) {
    return null;
  }
}
