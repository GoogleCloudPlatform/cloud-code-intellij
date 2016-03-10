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

package com.google.gct.idea.appengine.util;

import com.google.gct.idea.appengine.GctConstants;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for endpoint validation checks.
 */
public class EndpointUtilities {
  /**
   * Removes the beginning and ending quotes on a string. If the input string
   * does not have quotes at the beginning and at the end, the input string is returned.
   * @param input String to be parsed
   * @return input string with quotes at the beginning and end removed if they both exist.
   */
  public static String removeBeginningAndEndingQuotes(String input) {
    if (input == null || !input.startsWith("\"") || !input.endsWith("\"")) {
      return input;
    }

    return input.substring(1,input.length() - 1);
  }

  /**
   * Returns true if the class containing the psiElement has the @Api annotation
   *     (com.google.api.server.spi.config.Api). Returns false otherwise.
   * @param psiElement
   * @return true if the class containing to the psiElement has the @Api
   *     (com.google.api.server.spi.config.Api). Returns false otherwise.
   */
  public static boolean isEndpointClass(PsiElement psiElement) {
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
   * Returns true is a method is public but not static. Returns false otherwise.
   * @param psiMethod PsiMethod to be parsed.
   * @return  Returns true is a method is public but not static. Returns false otherwise.
   */
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                      justification = "PsiMethod.getModifierList() is @NotNull")
  public static boolean isApiMethod(@NonNls PsiMethod psiMethod) {
    PsiModifierList psiModifierList = psiMethod.getModifierList();
    if(psiModifierList.hasModifierProperty(PsiModifier.PUBLIC) &&
       !psiModifierList.hasModifierProperty(PsiModifier.STATIC)) {
      return true;
    }
    return false;
  }

  /**
   * Replace sequence of dots with single dot.
   */
  public static String collapseSequenceOfDots(@NotNull String word){
    return word.replaceAll("[.]+",".");
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                      justification = "PsiMethod.getModifierList() is @NotNull")
  public static boolean isPublicNullaryConstructor(PsiMethod method) {
    if(!method.isConstructor()) {
      return false;
    }

    if(method.getParameterList().getParametersCount() > 0) {
      return false;
    }

    PsiModifierList modifierList = method.getModifierList();
    if(modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
      return true;
    }

    return false;
  }

}
