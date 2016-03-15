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
import com.google.gct.idea.appengine.util.EndpointBundle;

import com.google.gct.idea.appengine.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Inspection to check for unique backend method names.
 * The backend method name is the full java name of the actual java method
 * (fully-specified class name + "." + method name).
 */
public class FullJavaNameInspection extends EndpointInspectionBase {
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("backend.name.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("backend.name.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("backend.name.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      /**
       * Flags methods that have a duplicate full java name.
       */
      @Override
      public void visitClass(PsiClass aClass){
        if (!EndpointUtilities.isEndpointClass(aClass)) {
          return;
        }

        PsiMethod[] allMethods = aClass.getMethods();
        Map<String, PsiMethod> javaMethodNames = Maps.newHashMap();

        for(PsiMethod aMethod : allMethods)  {
          validateBackendMethodNameUnique(aMethod, javaMethodNames );
        }
      }

      /**
       * Checks that the backend method name is unique to ensure that there are no overloaded
       * API methods.
       * @param psiMethod
       * @param javaMethodNames
       */
      private void validateBackendMethodNameUnique(PsiMethod psiMethod, Map<String, PsiMethod> javaMethodNames) {
        // Check if method is a public or non-static
        if(!EndpointUtilities.isApiMethod(psiMethod)) {
          return;
        }

        if(psiMethod.isConstructor()) {
          return;
        }

        String javaName = psiMethod.getContainingClass().getQualifiedName() + "." + psiMethod.getName();
        PsiMethod seenMethod = javaMethodNames.get(javaName);
        if (seenMethod == null) {
          javaMethodNames.put(javaName, psiMethod);
        } else {
          String psiMethodName = psiMethod.getContainingClass().getName() + "." + psiMethod.getName() +
            psiMethod.getParameterList().getText();
          String seenMethodName = seenMethod.getContainingClass().getName() + "." + seenMethod.getName() +
            seenMethod.getParameterList().getText();
          holder.registerProblem(psiMethod, "Overloaded methods are not supported. " +  javaName +
            " has at least one overload: " + psiMethodName + " and " + seenMethodName,
            new MyQuickFix());
        }

      }
    };
  }

  /**
   * Quick fix for {@link FullJavaNameInspection} problems.
   */
  public class MyQuickFix implements LocalQuickFix {
    public MyQuickFix() {

    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Rename method";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Adds "_1" to the name of the PsiElement in <code>desciptor</code>.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if(!(element instanceof PsiMethod)) {
        return;
      }
      PsiMethod method = (PsiMethod)element;
      method.setName(method.getName() + "_1");
    }

  }
}
