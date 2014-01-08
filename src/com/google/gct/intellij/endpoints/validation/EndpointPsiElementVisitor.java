/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.gct.intellij.endpoints.validation;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.util.PsiUtils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;


/**
 * A visitor that has endpoint validation specific functionality.
 */
public class EndpointPsiElementVisitor extends JavaElementVisitor {
  // TODO: Add tests

  /**
   * Returns true if the class containing the psiElement has the @Api annotation
   *     (com.google.api.server.spi.config.Api). Returns false otherwise.
   * @param psiElement
   * @return true if the class containing to the psiElement has the @Api
   *     (com.google.api.server.spi.config.Api). Returns false otherwise.
   */
  public boolean isEndpointClass(PsiElement psiElement) {
    PsiClass psiClass = PsiUtils.findClass(psiElement);
    if(psiClass == null) {
      return false;
    }

    if (AnnotationUtil.isAnnotated(psiClass, GctConstants.APP_ENGINE_ANNOTATION_API, false)) {
      return true;
    } else {
      return false;
    }
  }

}