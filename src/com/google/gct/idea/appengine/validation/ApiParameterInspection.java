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

package com.google.gct.idea.appengine.validation;

import com.google.gct.idea.appengine.GctConstants;
import com.google.gct.idea.appengine.util.EndpointBundle;

import com.google.gct.idea.appengine.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.generate.tostring.psi.PsiAdapter;

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
        if (!EndpointUtilities.isEndpointClass(psiParameter)) {
          return;
        }

        // Check if method is public or non-static
        PsiElement psiElement = psiParameter.getDeclarationScope();
        if (psiElement instanceof PsiMethod) {
          if(!EndpointUtilities.isApiMethod((PsiMethod)psiElement)) {
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
          new MyQuickFix());
        }
      }
    };
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

  /**
   * Quick fix for {@link ApiParameterInspection} problems to add @Named to method parameters.
   */
  public class MyQuickFix implements LocalQuickFix {
    public MyQuickFix() {

    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Add @Named";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Add the {@link GctConstants.APP_ENGINE_ANNOTATION_NAMED} annotation to the {@link PsiParameter}
     * in <code>descriptor</code>. The query name in {@link GctConstants.APP_ENGINE_ANNOTATION_NAMED}
     * will be the name of the {@link PsiParameter} in <code>descriptor</code>.
     * If the {@link PsiElement} in <code>descriptor</code> is not of {@link PsiParameter} type or
     * if the {@link PsiParameter} in <code>descriptor</code> already has
     * {@link GctConstants.APP_ENGINE_ANNOTATION_NAMED} or javax.inject.Named, no annotation will be added.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if (element == null) {
        return;
      }

      if(!(element instanceof PsiParameter)) {
        return;
      }
      PsiParameter parameter = (PsiParameter)element;

      PsiModifierList modifierList = parameter.getModifierList();
      if(modifierList == null) {
        return;
      }

      if(modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED) != null) {
        return;
      }

      if(modifierList.findAnnotation("") != null) {
        return;
      }

      String annotationString = "@Named(\"" + parameter.getName() + "\")";
      PsiAnnotation annotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(annotationString, element);
      modifierList.add(annotation);

      PsiFile file = parameter.getContainingFile();
      if(file == null) {
        return;
      }

      if(!(file instanceof PsiJavaFile)) {
        return;
      }

      PsiAdapter.addImportStatement((PsiJavaFile)file, GctConstants.APP_ENGINE_ANNOTATION_NAMED);
    }
  }
}
