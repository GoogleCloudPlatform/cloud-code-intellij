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

import com.google.gct.idea.appengine.util.EndpointBundle;

import com.google.gct.idea.appengine.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection to check that the return type of an API method is an entity (resource) type.
 */
public class MethodReturnTypeInspection extends EndpointInspectionBase{
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("method.return.type.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("method.return.type.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("method.return.type.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitMethod(PsiMethod method) {
        if (!EndpointUtilities.isEndpointClass(method)) {
          return;
        }

        if(hasTransformer(method)) {
          return;
        }

        if(!EndpointUtilities.isApiMethod(method)) {
          return;
        }

        PsiType returnType = method.getReturnType();
        if(returnType == null) {
          return;
        }

        if(returnType.isAssignableFrom(PsiType.VOID)) {
          return;
        }

        Project project;
        try {
          project = method.getContainingFile().getProject();
          if (project == null) {
            return;
          }
        } catch (PsiInvalidElementAccessException e) {
          LOG.error("Error getting project with method " + method.getText(), e);
          return;
        }

        if(!isEntityParameter(returnType, project)){
          holder.registerProblem(method.getReturnTypeElement(), "Invalid return type: " + returnType.getPresentableText() +
            ". Primitives and enums are not allowed.", LocalQuickFix.EMPTY_ARRAY);
        }
      }
    };
  }
}
