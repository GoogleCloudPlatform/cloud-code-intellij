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

package com.google.cloud.tools.intellij.testing;

import java.util.TimeZone;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** A custom {@link TestRule} for setting a time zone in unit tests. */
public final class TimeZoneRule implements TestRule {
  private final TimeZone timeZone;

  public TimeZoneRule(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
          TimeZone.setDefault(timeZone);
          base.evaluate();
        } finally {
          TimeZone.setDefault(defaultTimeZone);
        }
      }
    };
  }
}
