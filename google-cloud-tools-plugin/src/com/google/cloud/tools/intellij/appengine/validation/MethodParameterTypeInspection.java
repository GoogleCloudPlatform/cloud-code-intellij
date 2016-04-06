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

import com.google.cloud.tools.intellij.appengine.util.EndpointBundle;
import com.google.cloud.tools.intellij.appengine.util.EndpointUtilities;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection to check that parameter types do not contain multiple levels of collections or arrays.
 */
public class MethodParameterTypeInspection extends EndpointInspectionBase {
  // TODO: check if class has a transformer and add check that only parameter and entity types have @Named

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("method.parameter.type.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("method.parameter.type.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("method.parameter.type.short.name");
  }

  /**
   * Returns false if <code>type</code> is a multiple levels of collections or arrays.
   * Returns true otherwise.
   * @param type The PsiType been validated.
   * @param project The project that has the PsiElement associated with <code>type</code>.
   * @return
   */
  public boolean isValidArrayOrPrimitiveType(PsiType type, Project project) {
    if (type instanceof PsiArrayType) {
      PsiArrayType arrayType = (PsiArrayType) type;
      if (arrayType.getComponentType() instanceof PsiPrimitiveType) {
        return true;
      } else {
        return isValidInnerArrayType(arrayType.getComponentType(), project);
      }
    }

    // Check if type is a Collection
    PsiClassType collectionType =
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");
    if (collectionType.isAssignableFrom(type)) {
      assert (type instanceof PsiClassType);
      PsiClassType classType = (PsiClassType) type;
      PsiType[] typeParams = classType.getParameters();
      assert (typeParams.length > 0);
      return  isValidInnerArrayType(typeParams[0], project);
    }

    return true;
  }

  /**
   * Returns false is <code>type</code> is an array or a java.util.Collection
   * or one of its subtypes. Returns true otherwise.
   * @param type The PsiType been validated.
   * @param project The project that has the PsiElement associated with  <code>type</code>.
   * @return
   */
  public boolean isValidInnerArrayType(PsiType type, Project project) {
    if (type instanceof PsiArrayType) {
      return false;
    }

    // Check if type is a Collection
    PsiClassType collectionType = JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");
    if (collectionType.isAssignableFrom(type)) {
      return false;
    }

    return true;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitParameter (PsiParameter psiParameter){
        if (!EndpointUtilities.isEndpointClass(psiParameter)) {
          return;
        }

        if(hasTransformer(psiParameter)) {
          return;
        }

        PsiElement psiElement = psiParameter.getDeclarationScope();
        if (psiElement instanceof PsiMethod) {
          if(!EndpointUtilities.isApiMethod((PsiMethod)psiElement)) {
            return;
          }
        } else {
          return;
        }

        Project project;
        try {
          project = psiParameter.getContainingFile().getProject();
          if (project == null) {
            return;
          }
        } catch (PsiInvalidElementAccessException e) {
          LOG.error("Error getting project with parameter " + psiParameter.getText(), e);
          return;
        }

        if(!isValidArrayOrPrimitiveType(psiParameter.getType(), project)) {
          holder.registerProblem(psiParameter, "Illegal nested collection type " + psiParameter.getType().getPresentableText() + ".", LocalQuickFix.EMPTY_ARRAY);
        }
      }
    };
  }
}

