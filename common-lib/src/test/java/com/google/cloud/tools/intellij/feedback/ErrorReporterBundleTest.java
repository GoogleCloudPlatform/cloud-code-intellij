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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/** Mundane test to make sure the resource loading is working correctly. */
public class ErrorReporterBundleTest {

  @Test
  public void testMessage() throws Exception {
    final String errorString = ErrorReporterBundle.message("error.googlefeedback.message");
    assertNotNull(errorString);
  }
}
