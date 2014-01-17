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
import com.intellij.psi.*;


/**
 * A visitor that has endpoint validation specific functionality.
 */
public class EndpointPsiElementVisitor extends JavaElementVisitor {
  // TODO: Add tests
  private static final String API_TRANSFORMER_ATTRIBUTE = "transformers";


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

    if (AnnotationUtil.isAnnotated(psiClass, GctConstants.APP_ENGINE_ANNOTATION_API, true) ||
        AnnotationUtil.isAnnotated(psiClass, GctConstants.APP_ENGINE_ANNOTATION_API_CLASS, true) ||
        AnnotationUtil.isAnnotated(psiClass, GctConstants.APP_ENGINE_ANNOTATION_API_REFERENCE, true) ) {
      return true;
    } else {
      return false;
    }
  }

  /**
   *  Returns true if the class containing <code>psiElement</code> has a transformer
   *  specified by using the @ApiTransformer annotation on a class or by
   *  using the transformer attribute of the @Api annotation. Returns false otherwise.
   * @param psiElement
   * @return  True if the class containing <code>psiElement</code> has a transformer
   * and false otherwise.
   */
  public boolean hasTransformer(PsiElement psiElement) {
    PsiClass psiClass = PsiUtils.findClass(psiElement);
    if(psiClass == null) {
      return false;
    }

    PsiModifierList modifierList = psiClass.getModifierList();
    if(modifierList == null) {
      return false;
    }

    // Check if class has @ApiTransformer to specify a transformer
    PsiAnnotation apiTransformerAnnotation =
      modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API_TRANSFORMER);
    if (apiTransformerAnnotation != null) {
      return true;
    }

    // Check if class utilizes the transformer attribute of the @Api annotation
    // to specify its transformer
    PsiAnnotation apiAnnotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API);
    if (apiAnnotation != null) {
      PsiAnnotationMemberValue transformerMember =
        apiAnnotation.findAttributeValue(API_TRANSFORMER_ATTRIBUTE);
      if(transformerMember != null) {
        if(!transformerMember.getText().equals("{}")) {
          return true;
        }
      }
    }

    return false;
  }

}