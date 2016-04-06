/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.util;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * Endpoint messages bundle.
 */
public class EndpointBundle {
  @NonNls
  private static final String BUNDLE_NAME = "messages.EndpointBundle";
  private static Reference<ResourceBundle> bundleReference;

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = null;
    if (bundleReference != null) {
      bundle = bundleReference.get();
    }
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE_NAME);
      bundleReference = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }

  private EndpointBundle() {
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }
}
