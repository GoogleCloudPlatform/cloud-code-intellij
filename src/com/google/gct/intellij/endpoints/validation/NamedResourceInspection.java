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

import com.google.common.collect.Maps;
import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.util.EndpointBundle;
import com.google.gct.intellij.endpoints.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Inspection to check named resource names for a parameters for a single endpoint method are unique.
 */
public class NamedResourceInspection extends EndpointInspectionBase {
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("named.resource.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("named.resource.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("named.resource.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitMethod(PsiMethod psiMethod) {
        if (!isEndpointClass(psiMethod)) {
          return;
        }

        if(hasTransformer(psiMethod)) {
          return;
        }

        // Check if method is public or non-static
        if(!isApiMethod(psiMethod)) {
          return;
        }

        PsiParameterList parameterList = psiMethod.getParameterList();
        Map<String, PsiParameter> methodNames = Maps.newHashMap();

        for(PsiParameter aParameter : parameterList.getParameters())  {
          validateMethodNameUnique(aParameter, methodNames);
        }
      }

      private void validateMethodNameUnique(PsiParameter psiParameter, Map<String, PsiParameter> methodNames) {
        PsiModifierList modifierList = psiParameter.getModifierList();
        if(modifierList == null) {
          return;
        }

        PsiAnnotation annotation = modifierList.findAnnotation("javax.inject.Named");
        if(annotation == null) {
          annotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED );
          if(annotation == null) {
            return;
          }
        }

        PsiNameValuePair[] nameValuePairs = annotation.getParameterList().getAttributes();
        if(nameValuePairs.length != 1){
          return;
        }

        if(nameValuePairs[0] == null) {
          return;
        }

        PsiAnnotationMemberValue memberValue = nameValuePairs[0].getValue();
        if(memberValue == null) {
          return;
        }

        String nameValueWithQuotes = memberValue.getText();
        String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);

        PsiParameter seenParameter = methodNames.get(nameValue);
        if (seenParameter == null) {
          methodNames.put(nameValue, psiParameter);
        } else {
          holder.registerProblem(psiParameter, "Duplicate parameter name: " + nameValue +
            ". Parameter names must be unique.", LocalQuickFix.EMPTY_ARRAY);
        }
      }
    };
  }
}

