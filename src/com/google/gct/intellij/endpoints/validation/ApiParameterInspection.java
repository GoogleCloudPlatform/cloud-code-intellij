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
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Inspection to check that API parameters of parameter type have an API parameter
 * name that is specified with @Named.
 */
public class ApiParameterInspection extends EndpointInspectionBase {
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("api.parameter.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("api.parameter.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("api.parameter.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitParameter(PsiParameter psiParameter){
        if (!isEndpointClass(psiParameter)) {
          return;
        }

        // Check if method is public or non-static
        PsiElement psiElement = psiParameter.getDeclarationScope();
        if (psiElement instanceof PsiMethod) {
          if(!isApiMethod((PsiMethod)psiElement)) {
            return;
          }

          if(((PsiMethod)psiElement).isConstructor()) {
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
          LOG.error("Cannot determine project with parameter " + psiParameter.getText(), e);
          return;
        }

        // Check if parameter is an API Parameter
        PsiType psiType = psiParameter.getType();
        if(!isApiParameter(psiType, project)) {
          return;
        }

        // Check that API parameter has Named Resource (@Named)
        if(!hasParameterName(psiParameter)) {
          holder.registerProblem(psiParameter, "Missing parameter name. Parameter type (" +
          psiType.getPresentableText() +
          ") is not an entity type and thus should be annotated with @Named.",
          LocalQuickFix.EMPTY_ARRAY);
        }
      }
    };
  }

  /**
   * Returns true if the raw or base type of <code>psiParameter</code> is
   * one of endpoint parameter type.
   * @return
   */
  private boolean isApiParameter(PsiType psiType, Project project) {
    PsiType baseType = psiType;
    PsiClassType collectionType =
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Collection");

    // If type is an array or collection, get the component type
    if (psiType instanceof PsiArrayType) {
      PsiArrayType arrayType = (PsiArrayType)psiType;
      baseType= arrayType.getDeepComponentType();
    } else if (collectionType.isAssignableFrom(psiType)) {
      assert (psiType instanceof PsiClassType);
      PsiClassType classType = (PsiClassType) psiType;
      PsiType[] parameters = classType.getParameters();
      if(parameters.length == 0) {
        return false;
      }
      baseType = parameters[0];
    }

    Map<PsiClassType, String> parameterTypes = createParameterTypes(project);
    for(PsiClassType aClassType : parameterTypes.keySet()) {
      if (aClassType.isAssignableFrom(baseType)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasParameterName(PsiParameter psiParameter) {
    PsiModifierList modifierList = psiParameter.getModifierList();
    if(modifierList == null) {
      return false;
    }

    PsiAnnotation annotation = modifierList.findAnnotation("javax.inject.Named");
    if(annotation != null) {
      return  true;
    }

    annotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED );
    if(annotation != null) {
      return true;
    }

    return  false;
  }

  private static Map<PsiClassType, String> createParameterTypes(Project project) {
    Map<PsiClassType, String> parameterTypes = new HashMap<PsiClassType, String>();
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Enum"),"enum");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.String"),"string");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Boolean"),"boolean");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Integer"),"int32");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Long"),"int64");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Float"),"float");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.lang.Double"),"double");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("java.util.Date"),"datetime");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("com.google.api.server.spi.types.DateAndTime"),"datetime");
    parameterTypes.put(
      JavaPsiFacade.getElementFactory(project).createTypeByFQClassName("com.google.api.server.spi.types.SimpleDate"),"date");

    return Collections.unmodifiableMap(parameterTypes);
  }
}
