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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.util.List;

/** A service that fetches the list of available {@link CloudLibrary libraries}. */
public class CloudLibrariesService {
  private static final Logger logger = Logger.getInstance(CloudLibrariesService.class);

  static CloudLibrariesService getInstance() {
    return ServiceManager.getService(CloudLibrariesService.class);
  }

  /** Returns an immutable list of {@link CloudLibrary libraries}. */
  List<CloudLibrary> getCloudLibraries() {
    try {
      return ImmutableList.copyOf(CloudLibraries.getCloudLibraries());
    } catch (IOException e) {
      logger.error(e);
      return ImmutableList.of();
    }
  }
}
