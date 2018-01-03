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

package com.google.cloud.tools.intellij.apis;

/** Defines the management specification of a single GCP API for actions such as API enablement. */
class CloudApiManagementSpec {
  private boolean shouldEnable;

  CloudApiManagementSpec(boolean shouldEnable) {
    this.shouldEnable = shouldEnable;
  }

  boolean shouldEnable() {
    return shouldEnable;
  }

  void setShouldEnable(boolean shouldEnable) {
    this.shouldEnable = shouldEnable;
  }
}
