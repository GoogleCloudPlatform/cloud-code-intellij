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
import com.google.gct.intellij.endpoints.util.EndpointBundle;
import com.google.gct.intellij.endpoints.util.EndpointUtilities;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementVisitor;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Inspection class for validating endpoint API name.
 */
public class ApiNameInspection extends EndpointInspectionBase {
  private static final String API_NAME_ATTRIBUTE = "name";
  private static final Pattern API_NAME_PATTERN = Pattern.compile("^[a-z]+[A-Za-z0-9]*$");

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("api.name.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("api.name.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("api.name.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final com.intellij.codeInspection.ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {

      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        if(!annotation.getQualifiedName().equals(GctConstants.APP_ENGINE_ANNOTATION_API)) {
          return;
        }

        PsiAnnotationMemberValue annotationMemberValue = annotation.findAttributeValue(API_NAME_ATTRIBUTE);
        if(annotationMemberValue == null) {
          return;
        }

        String nameValueWithQuotes = annotationMemberValue.getText();
        String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);

        if (!API_NAME_PATTERN.matcher(nameValue).matches()) {
          // TODO: Add quick fix.
          holder.registerProblem(annotation, "Invalid api name: it must start with a lower case letter and consists only of letter and digits",
            LocalQuickFix.EMPTY_ARRAY);
        }

      }
    };
  }
}

