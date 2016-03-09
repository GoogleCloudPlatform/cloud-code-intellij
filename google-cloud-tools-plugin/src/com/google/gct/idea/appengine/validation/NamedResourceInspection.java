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

import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.GctConstants;
import com.google.gct.idea.appengine.util.EndpointBundle;
import com.google.gct.idea.appengine.util.EndpointUtilities;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Inspection to check named resource names for a parameters for a single endpoint method are unique
 * and that the resource names are specified in the @Named annotations.
 */
public class NamedResourceInspection extends EndpointInspectionBase {
  public enum Error {MISSING_NAME, DUPLICATE_PARAMETER};

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
        if (!EndpointUtilities.isEndpointClass(psiMethod)) {
          return;
        }

        if(hasTransformer(psiMethod)) {
          return;
        }

        // Check if method is public or non-static
        if(!EndpointUtilities.isApiMethod(psiMethod)) {
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
        if(nameValuePairs.length == 0) {
          // For @Named, @Named()
          holder.registerProblem(annotation, "Parameter name must be specified.", new MissingNameQuickFix());
          return;
        } else if(nameValuePairs.length != 1){
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

        // For @Named("")
        if(nameValue.isEmpty()){
          holder.registerProblem(annotation, "Parameter name must be specified.", new MissingNameQuickFix());
          return;
        }

        PsiParameter seenParameter = methodNames.get(nameValue);
        if (seenParameter == null) {
          methodNames.put(nameValue, psiParameter);
        } else {
          holder.registerProblem(annotation, "Duplicate parameter name: " + nameValue +
             ". Parameter names must be unique.", new DuplicateNameQuickFix());
        }
      }
    };
  }

  /**
   * LocalQuickFix to add "_1" to the end of a query name as specified
   * by @Named.
   */
  public class DuplicateNameQuickFix implements LocalQuickFix {
    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Rename duplicate parameter";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Adds "_1" to the query name in an @Named annotation in <code>descriptor</code>.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if (element == null) {
        return;
      }

      if(!(element instanceof PsiAnnotation)) {
        return;
      }

      PsiAnnotation annotation = (PsiAnnotation)element;
      String annotationQualifiedName = annotation.getQualifiedName();
      if (annotationQualifiedName == null) {
        return;
      }

      if((!annotationQualifiedName.equals("javax.inject.Named"))  &&
        (!annotationQualifiedName.equals(GctConstants.APP_ENGINE_ANNOTATION_NAMED))) {
          return;
      }

      // Get @Named value
      PsiNameValuePair[] nameValuePairs = annotation.getParameterList().getAttributes();
      if(nameValuePairs.length == 0){
        return;
      }

      PsiAnnotationMemberValue memberValue = nameValuePairs[0].getValue();
      if(memberValue == null){
        return;
      }

      // Create new annotation with  value equal to  @Named's value plus "_1"
      String newNamedValue  =  "@Named(\"" + EndpointUtilities.removeBeginningAndEndingQuotes(memberValue.getText()) +  "_1\")";
      PsiAnnotation newAnnotation =
        JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(newNamedValue, null);
      assert(newAnnotation.getParameterList().getAttributes().length == 1);

      // Update value of @Named
      memberValue.replace(newAnnotation.getParameterList().getAttributes()[0].getValue());
    }
  }

  /**
   * LocalQuickFix to add a query name to an @Named without a specified query name.
   */
  public class MissingNameQuickFix implements LocalQuickFix {
    private final String DEFAULT_PARAMETER_NAME = "myName";

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Add name";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Adds a query parameter name to an @Named annotation in <code>descriptor</code>.
     * If the @Named annotation in <code>descriptor</code> is a child of a {@link PsiParameter}
     * the query name is the {@link PsiParameter}'s name else, the new query name is a default
     * query name.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if (element == null) {
        return;
      }

      if(!(element instanceof PsiAnnotation)) {
        return;
      }

      PsiAnnotation annotation = (PsiAnnotation)element;
      String annotationQualifiedName = annotation.getQualifiedName();
      if (annotationQualifiedName == null) {
        return;
      }

      if((!annotationQualifiedName.equals("javax.inject.Named"))  &&
         (!annotationQualifiedName.equals(GctConstants.APP_ENGINE_ANNOTATION_NAMED))) {
        return;
      }

      String nameValue = DEFAULT_PARAMETER_NAME;
      PsiElement modifierList = annotation.getParent();
      if(modifierList != null)  {
        PsiElement annotationParent = modifierList.getParent();
        if(annotationParent instanceof  PsiParameter) {
          nameValue = ((PsiParameter)annotationParent).getName();
        }
      }

      // Create dummy annotation with replacement value
      String newNamedValue  =  "@Named(\"" + nameValue + "\")";
      PsiAnnotation newAnnotation =
        JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(newNamedValue, null);
      assert(newAnnotation.getParameterList().getAttributes().length == 1);

      // Add new value to @Named
      annotation.getParameterList().replace(newAnnotation.getParameterList());
    }
  }
}

