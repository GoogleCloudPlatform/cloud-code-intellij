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

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClient;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import java.util.Optional;

/** Holds utility methods that work with {@link CloudLibrary} classes. */
final class CloudLibraryUtils {

  private static final String JAVA_CLIENT_LANGUAGE = "java";

  /** Prevents instantiation. */
  private CloudLibraryUtils() {}

  /**
   * Returns the first Java {@link CloudLibraryClient} in the given {@link CloudLibrary}, or {@link
   * Optional#empty()} if none exists.
   *
   * @param library the {@link CloudLibrary} to return the first Java client for
   */
  static Optional<CloudLibraryClient> getJavaClient(CloudLibrary library) {
    if (library.getClients() == null) {
      return Optional.empty();
    }

    return library
        .getClients()
        .stream()
        .filter(client -> JAVA_CLIENT_LANGUAGE.equals(client.getLanguage()))
        .findFirst();
  }

  /**
   * Returns the {@link CloudLibraryClientMavenCoordinates} for the first Java client in the given
   * {@link CloudLibrary}, or {@link Optional#empty()} if none exists.
   *
   * <p>For details on how the {@link CloudLibraryClient} is found, see {@link
   * #getJavaClient(CloudLibrary)}.
   *
   * @param library the {@link CloudLibrary} to return Maven coordinates for
   */
  static Optional<CloudLibraryClientMavenCoordinates> getJavaClientMavenCoordinates(
      CloudLibrary library) {
    return getJavaClient(library).map(CloudLibraryClient::getMavenCoordinates);
  }
}
