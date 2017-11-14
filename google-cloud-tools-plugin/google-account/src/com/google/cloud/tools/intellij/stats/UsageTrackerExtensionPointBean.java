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

package com.google.cloud.tools.intellij.stats;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * Class that defines extensions points in plugin.xml, Extensions point will implement
 * {@link UsageTrackerProvider}
 */
public class UsageTrackerExtensionPointBean extends AbstractExtensionPointBean implements
    KeyedLazyInstance<UsageTracker> {

  // TODO : when changing the package root for this plugin, update this
  static final ExtensionPointName<UsageTracker> EP_NAME =
      new ExtensionPointName<UsageTracker>("com.google.gct.core.usageTracker");

  @Attribute("key")
  public String key;

  @Attribute("implementationClass")
  public String implementationClass;

  private final LazyInstance<UsageTracker> handler = new LazyInstance<UsageTracker>() {
    @Override
    protected Class<UsageTracker> getInstanceClass() throws ClassNotFoundException {
      return findClass(implementationClass);
    }
  };

  @NotNull
  @Override
  public UsageTracker getInstance() {
    return handler.getValue();
  }

  @Override
  public String getKey() {
    return key;
  }
}
