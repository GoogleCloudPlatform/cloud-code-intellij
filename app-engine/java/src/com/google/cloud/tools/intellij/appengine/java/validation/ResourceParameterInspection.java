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

package com.google.cloud.tools.intellij.appengine.java.validation;

import com.google.cloud.tools.intellij.appengine.java.GctConstants;
import com.google.cloud.tools.intellij.appengine.java.util.EndpointBundle;
import com.google.cloud.tools.intellij.appengine.java.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection to check that resource or entity parameters are not a collection or an array and do
 * not utilize @Named.
 */
public class ResourceParameterInspection extends EndpointInspectionBase {

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("resource.parameter.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("resource.parameter.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("resource.parameter.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      private int resourceParameterCount = 0;

      @Override
      public void visitMethod(PsiMethod method) {
        if (!EndpointUtilities.isEndpointClass(method)) {
          return;
        }

        if (method.isConstructor()) {
          return;
        }

        // Check if method is public or non-static
        if (!EndpointUtilities.isApiMethod(method)) {
          return;
        }

        Project project;
        try {
          project = method.getContainingFile().getProject();
          if (project == null) {
            return;
          }
        } catch (PsiInvalidElementAccessException ex) {
          LOG.error("Error getting project with parameter " + method.getText(), ex);
          return;
        }

        resourceParameterCount = 0;
        for (PsiParameter param : method.getParameterList().getParameters()) {
          validateMethodParameters(param, project);
        }

        // Check that there is no more than one resource (entity) parameter for this method.
        if (resourceParameterCount > 1) {
          holder.registerProblem(
              method,
              "Multiple entity parameters. There can only be a single entity parameter per method.",
              LocalQuickFix.EMPTY_ARRAY);
        }
      }

      private void validateMethodParameters(PsiParameter psiParameter, Project project) {
        // Check if parameter is of entity (resource) type which is not of parameter type or
        // injected type.
        PsiType type = psiParameter.getType();
        if (!isEntityParameter(type, project)) {
          return;
        }

        // Update count of resource (entity) parameters for this method
        resourceParameterCount++;

        // Check that parameter is not a collection or an array
        if (type instanceof PsiArrayType || isCollectionType(type, project)) {
          holder.registerProblem(
              psiParameter,
              "Illegal parameter type (\'"
                  + psiParameter.getType().getPresentableText()
                  + "\'). Arrays or collections of entity types are not allowed.",
              LocalQuickFix.EMPTY_ARRAY);
        }

        // Check that parameter does not have an @Named annotation
        PsiModifierList modifierList = psiParameter.getModifierList();
        PsiAnnotation annotation = modifierList.findAnnotation("javax.inject.Named");
        if (annotation == null) {
          annotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED);
          if (annotation == null) {
            return;
          }
        }

        holder.registerProblem(
            psiParameter,
            "Bad parameter name. Parameter is entity (resource)" + " type and should not be named.",
            LocalQuickFix.EMPTY_ARRAY);
      }
    };
  }

  private boolean isCollectionType(PsiType type, Project project) {
    PsiClassType collectionType =
        JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");

    if (collectionType.isAssignableFrom(type)) {
      return true;
    }
    // hack because isAssignableFrom is broken
    // todo(elharo): cover other collection types and non-generic collections
    // todo(elharo): better regex on Java class names
    String name = type.getCanonicalText();
    if (name.matches("List<.+>")) {
      return true;
    }
    return false;
  }
}
