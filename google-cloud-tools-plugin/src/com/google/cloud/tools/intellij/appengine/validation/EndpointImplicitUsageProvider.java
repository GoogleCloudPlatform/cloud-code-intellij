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

import com.google.cloud.tools.intellij.appengine.util.EndpointUtilities;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

/**
 * Disables highlighting of certain Endpoint class elements as unused because they might not be
 * referenced from the code but are referenced from the generated Endpoint APIs and client
 * libraries.
 */
public class EndpointImplicitUsageProvider implements ImplicitUsageProvider {

  @Override
  public boolean isImplicitUsage(PsiElement element) {
    // Checks that if a class is annotated as @Api/@ApiClass, it shouldn't be highlighted as unused
    if (element instanceof PsiClass) {
      return EndpointUtilities.isEndpointClass(element);
    }

    if (!EndpointUtilities.isEndpointClass(element)) {
      return false;
    }

    if (element instanceof PsiMethod) {
      // Checks that all public methods in a Endpoint class shouldn't be highlighted as unused
      // because all public methods (those with and without @ApiMethod) would be exposed in the API
      return EndpointUtilities.isApiMethod((PsiMethod) element);
    }

    return false;
  }

  @Override
  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
