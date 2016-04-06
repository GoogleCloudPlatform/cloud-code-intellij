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

import com.google.cloud.tools.intellij.appengine.GctConstants;
import com.google.cloud.tools.intellij.appengine.util.EndpointBundle;
import com.google.cloud.tools.intellij.appengine.util.EndpointUtilities;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspection to check that if a variable is named (that is, enclosed in { }) in the path attribute
 * of an @ApiMethod, the corresponding method parameter identified with the @Named attribute must
 * not also have a @Nullable or @DefaultValue annotation.
 */
public class InvalidParameterAnnotationsInspection extends EndpointInspectionBase {
  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("invalid.parameter.annotations.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("invalid.parameter.annotations.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("invalid.parameter.annotations.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitMethod (PsiMethod method){
        if (!EndpointUtilities.isEndpointClass(method)) {
          return;
        }

        if(!EndpointUtilities.isApiMethod(method)) {
          return;
        }

        // Get all the method parameters
        PsiParameterList parameterList = method.getParameterList();
        if(parameterList.getParametersCount() == 0) {
          return;
        }

        // Check for @ApiMethod
        PsiAnnotation apiMethodAnnotation = method.getModifierList().findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_API_METHOD);
        if(apiMethodAnnotation == null) {
          return;
        }

        // path has a default value of ""
        PsiAnnotationMemberValue pathMember = apiMethodAnnotation.findAttributeValue("path");
        String path = EndpointUtilities.removeBeginningAndEndingQuotes(pathMember.getText());

        // Check for path parameter, @ApiMethod(path="xys/{xy}")
        Collection<String> pathParameters = getPathParameters(path);
        if(pathParameters.size() == 0) {
          return;
        }

        // Check that no method parameter named with @Named that is also named in the path parameters
        // has the @Nullable or @DefaultValue
        for(PsiParameter aParameter : parameterList.getParameters()) {
          PsiAnnotationMemberValue namedAnnotation = getNamedAnnotationValue(aParameter);
          if(namedAnnotation == null) {
            continue;
          }
          String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(namedAnnotation.getText());

          // Check is @Named value is in list of path parameters
          if (!pathParameters.contains(nameValue)) {
            continue;
          }

          // Check if @Named parameter also has @Nullable or @DefaultValue
          if((aParameter.getModifierList().findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NULLABLE) != null)||
             (aParameter.getModifierList().findAnnotation("javax.annotation.Nullable") != null) ||
             (aParameter.getModifierList().findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_DEFAULT_VALUE) != null)) {
            holder.registerProblem(aParameter, "Invalid parameter configuration. " +
              "A parameter in the method path should not be marked @Nullable or @DefaultValue.",
              new MyQuickFix());
          }
        }


      }

      /**
       * Gets the parameters in <code>path</code>
       * @param path The path whose parameters are to be parsed.
       * @return A collection of the parameter in <code>path</code>
       */
      private Collection<String> getPathParameters(String path) {
        Pattern pathPattern = Pattern.compile("\\{([^\\}]*)\\}");
        Matcher pathMatcher = pathPattern.matcher(path);

        Collection<String> pathParameters = new HashSet<String>();
        while (pathMatcher.find()) {
          pathParameters.add(pathMatcher.group(1));
        }
        return pathParameters;
      }
    };
  }

  /**
   * Quick fix for {@link InvalidParameterAnnotationsInspection} problems.
   */
  public static class MyQuickFix implements LocalQuickFix {
    public MyQuickFix() {

    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Remove @Nullable and/or @DefaultValue";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return EndpointBundle.message("api.name.name");
    }

    /**
     * Remove @Default and @Nullable from the method parameter if they exist.
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

      PsiModifierList modifierList = ((PsiParameter)element).getModifierList();
      PsiAnnotation gaeNullableAnnotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NULLABLE);
      if(gaeNullableAnnotation != null){
        gaeNullableAnnotation.delete();
      }

      PsiAnnotation javaxNullableAnnotation = modifierList.findAnnotation("javax.annotation.Nullable");
      if(javaxNullableAnnotation != null){
        javaxNullableAnnotation.delete();
      }

      PsiAnnotation defaultValueAnnotation = modifierList.findAnnotation(GctConstants.APP_ENGINE_ANNOTATION_DEFAULT_VALUE);
      if(defaultValueAnnotation != null){
        defaultValueAnnotation.delete();
      }
    }
  }
}
