/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.validation;

/**
 * Tests for {@link EndpointImplicitUsageProvider}
 */
public class EndpointImplicitUsageProviderTest  extends EndpointTestBase {

  public void ignore_testUnusedHighlighting_ClassWithoutApi() {
    doTest();
  }

  public void ignore_testUnusedHighlighting_ClassWithApi() {
    doTest();
  }

  public void ignore_testUnusedHighlighting_ClassWithApiClass() {
    doTest();
  }

  public void testThisClassTestsNothing() {
    // purely to avoid a no tests warning until we fix the real test
  }

  private void doTest() {
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testHighlighting(true, false, false, "inspections/highlighting/" + getTestName(false) + ".java");
  }

}
