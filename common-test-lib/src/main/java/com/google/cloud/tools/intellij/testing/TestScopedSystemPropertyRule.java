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

package com.google.cloud.tools.intellij.testing;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

/**
 * A JUnit {@link TestRule} that will reset the given system property to its original value after
 * each test. This allows individual tests to set system properties for testing purposes.
 */
public class TestScopedSystemPropertyRule extends ExternalResource {

  private final String propertyKey;

  private String originalPropertyValue;

  public TestScopedSystemPropertyRule(String propertyKey) {
    this.propertyKey = propertyKey;
  }

  /** Store the original system property. */
  @Override
  protected void before() {
    originalPropertyValue = System.getProperty(propertyKey);
  }

  /** Restore the original system property. */
  @Override
  protected void after() {
    if (originalPropertyValue == null) {
      System.clearProperty(propertyKey);
    } else {
      System.setProperty(propertyKey, originalPropertyValue);
    }
  }
}
