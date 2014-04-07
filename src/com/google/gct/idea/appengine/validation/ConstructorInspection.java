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

import com.google.gct.idea.appengine.util.EndpointBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection to check that each class within an Endpoint API has a public nullary constructor.
 * The class is allowed to have other constructors.
 */
public class ConstructorInspection extends EndpointInspectionBase {
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("constructor.description");
  }


  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("constructor.name");
  }


  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("constructor.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitClass(PsiClass psiClass) {
        if (!isEndpointClass(psiClass)) {
          return;
        }

        PsiMethod[] allConstructors = psiClass.getConstructors();
        if(allConstructors.length == 0) {
          return;
        }

        // If there are user defined constructors, check that one of them
        // is a public nullary constructor
        for(PsiMethod aConstructor : allConstructors) {
           if(isPublicNullaryConstructor(aConstructor)) {
             return;
           }
        }

        // Register error if class does not have a public nullary constructor
        holder.registerProblem(psiClass, "Each class that is within an API must have a public nullary constructor.",
          new MyQuickFix(psiClass));
      }
    };
  }

  public class MyQuickFix implements LocalQuickFix {
    private final PsiClass psiClass;

    public MyQuickFix(PsiClass psiClass) {
      this.psiClass = psiClass;
    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Add nullary constructor";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      for(PsiMethod aConstructor : psiClass.getConstructors()) {
        if(isPublicNullaryConstructor(aConstructor)) {
          return;
        }
      }

      PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
      PsiMethod newConstructor = factory.createMethodFromText("public " + psiClass.getName() + "() { }", psiClass);
      final PsiMethod[] psiMethods = psiClass.getMethods();
      PsiMethod firstMethod = (psiMethods.length == 0) ? null : psiMethods [0];
      psiClass.addBefore(newConstructor, firstMethod);
    }
  }
}
