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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * An extension point for implementing build-system aware strategies for providing the path to the
 * appengine-web.xml directory.
 */
public interface BuildSystemAppEngineWebXmlDirectoryProvider {

  ExtensionPointName<BuildSystemAppEngineWebXmlDirectoryProvider> EP_NAME =
      ExtensionPointName.create("com.google.gct.core.appEngineWebXmlDirectoryProvider");

  /**
   * Returns, optionally, the path to the appengine-web.xml directory for the given module.
   *
   * <p>Will return {@link Optional#empty()} if module is not applicable to the type of build system
   * it represents.
   */
  Optional<String> getAppEngineWebXmlDirectoryPath(@NotNull Module module);
}
