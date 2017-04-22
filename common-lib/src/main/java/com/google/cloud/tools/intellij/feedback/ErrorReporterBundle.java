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

package com.google.cloud.tools.intellij.feedback;

import com.intellij.CommonBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/** Resource bundle for messages related to Google Feedback error reporting. */
public class ErrorReporterBundle {
  @NonNls private static final String BUNDLE_NAME = "messages.ErrorReporterBundle";
  private static Reference<ResourceBundle> bundleReference;

  private ErrorReporterBundle() {}

  private static synchronized ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(bundleReference);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE_NAME);
      bundleReference = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }

  public static String message(
      @NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }
}
