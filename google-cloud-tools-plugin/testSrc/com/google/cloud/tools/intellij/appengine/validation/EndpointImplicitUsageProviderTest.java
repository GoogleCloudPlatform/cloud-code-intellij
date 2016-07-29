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
package com.google.cloud.tools.intellij.appengine.validation;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Tests for {@link EndpointImplicitUsageProvider}.
 */
public class EndpointImplicitUsageProviderTest extends EndpointTestBase {

  private final String ASSERT_IMPLICIT_USAGE_TRUE = "assert_isImplicitUsage";
  private final String ASSERT_IMPLICIT_USAGE_FALSE = "assert_isNotImplicitUsage";

  private EndpointImplicitUsageProvider endpointUsageProvider;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    endpointUsageProvider = new EndpointImplicitUsageProvider();
  }

  public void testClassWithApi() throws IOException {
    doTest();
  }

  public void testClassWithApiClass() throws IOException {
    doTest();
  }

  public void testClassWithoutApi() throws IOException {
    doTest();
  }

  private void doTest() throws IOException {
    PsiClass psiClass = getPsiClassForTest();
    PsiMethod[] implicitUsageMethods = psiClass.findMethodsByName(ASSERT_IMPLICIT_USAGE_TRUE, false);
    PsiMethod[] notImplicitUsageMethods = psiClass.findMethodsByName(ASSERT_IMPLICIT_USAGE_FALSE, false);

    // Perform assertions on loaded test data, based on the presence of assertion keys in class and
    // method names.
    if (psiClass.getName().contains(ASSERT_IMPLICIT_USAGE_TRUE)) {
      assertTrue(endpointUsageProvider.isImplicitUsage(psiClass));
    } else if (psiClass.getName().contains(ASSERT_IMPLICIT_USAGE_FALSE)) {
      assertFalse(endpointUsageProvider.isImplicitUsage(psiClass));
    } else {
      fail("Class name must contain an assertion key. (Either '" +
          ASSERT_IMPLICIT_USAGE_TRUE+"' or '"+ASSERT_IMPLICIT_USAGE_FALSE+"')");
    }

    for (PsiMethod method : implicitUsageMethods) {
      assertTrue(endpointUsageProvider.isImplicitUsage(method));
    }
    for (PsiMethod method : notImplicitUsageMethods) {
      assertFalse(endpointUsageProvider.isImplicitUsage(method));
    }
  }

  private PsiClass getPsiClassForTest() throws IOException {
    String path = getTestDataPath() + "implicitUsageProviders/endpointImplicitUsageProvider/" + getTestName(false) + ".java";
    String classText = FileUtil.loadTextAndClose(new FileInputStream(path));
    return myFixture.addClass(classText);
  }

}
