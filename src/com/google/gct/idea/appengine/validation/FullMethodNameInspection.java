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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Inspection to check that the full method names are unique.
 * The full method name is made up of the api name + "." + api method name
 * (specified in @ApiMethod).
 */
public class FullMethodNameInspection extends EndpointInspectionBase  {
  private static final String API_METHOD_NAME_ATTRIBUTE = "name";

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("full.method.name.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("full.method.name.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("full.method.name.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      /**
       * Flags @ApiMethods that have a duplicate API method name.
       */
      @Override
      public void visitClass(PsiClass aClass){
        if (!isEndpointClass(aClass)) {
          return;
        }

        PsiMethod[] allMethods = aClass.getMethods();
        Map<String, PsiMethod> apiMethodNames = Maps.newHashMap();

        for(PsiMethod aMethod : allMethods)  {
          validateBackendMethodNameUnique(aMethod, apiMethodNames);
        }
      }

      /**
       * Checks that the API method name specified in @APiMethod's name attribute is unique.
       * API methods.
       */
      private void validateBackendMethodNameUnique(PsiMethod psiMethod, Map<String, PsiMethod> apiMethodNames) {
        // Check if method is a public or non-static
        if(!isApiMethod(psiMethod)) {
          return;
        }

        if(psiMethod.isConstructor()) {
          return;
        }

        // Get @ApiMethod's name attribute
        PsiModifierList modifierList = psiMethod.getModifierList();
        PsiAnnotation apiMethodAnnotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API_METHOD);
        if(apiMethodAnnotation == null) {
          return;
        }

        PsiAnnotationMemberValue apiMethodNameAttribute = apiMethodAnnotation.findAttributeValue(API_METHOD_NAME_ATTRIBUTE);
        if(apiMethodNameAttribute == null) {
          return;
        }

        String nameValueWithQuotes = apiMethodNameAttribute.getText();
        String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);

        // Check that @ApiMethod's name attribute has been used previously
        PsiMethod seenMethod = apiMethodNames.get(nameValue);
        if (seenMethod == null) {
          apiMethodNames.put(nameValue, psiMethod);
        } else {
          holder.registerProblem(apiMethodAnnotation, "Multiple methods with same API method name are prohibited. \"" +  nameValue +
            "\" is the API method name for " + psiMethod.getName() + " and " + seenMethod.getName() + ".",  new MyQuickFix());
        }

      }
    };
  }

  public class MyQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Rename API method name";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if(!(element instanceof PsiAnnotation)) {
        return;
      }

      PsiAnnotation annotation = (PsiAnnotation)element;
      if(!annotation.getQualifiedName().equals(GctConstants.APP_ENGINE_ANNOTATION_API_METHOD)) {
        return;
      }

      PsiAnnotationMemberValue apiMethodNameAttribute = annotation.findAttributeValue(API_METHOD_NAME_ATTRIBUTE);
      if(apiMethodNameAttribute == null) {
        return;
      }

      // Get name
      String nameValueWithQuotes = apiMethodNameAttribute.getText();
      String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);

      // @A(name = "someName")
      PsiAnnotationMemberValue newMemberValue = JavaPsiFacade.getInstance(project).getElementFactory()
        .createAnnotationFromText("@A(" + API_METHOD_NAME_ATTRIBUTE + " = \"" + nameValue + "_1\")", null).findDeclaredAttributeValue(API_METHOD_NAME_ATTRIBUTE);

      apiMethodNameAttribute.replace(newMemberValue);
    }
  }
}
