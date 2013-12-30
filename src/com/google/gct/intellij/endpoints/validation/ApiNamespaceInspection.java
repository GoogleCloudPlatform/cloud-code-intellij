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
import com.google.gct.intellij.endpoints.util.EndpointUtilities;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElementVisitor;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection class for validating endpoint API namespace.
 */
public class ApiNamespaceInspection extends LocalInspectionTool{
  private static final String API_NAMESPACE_DOMAIN_ATTRIBUTE = "ownerDomain";
  private static final String API_NAMESPACE_NAME_ATTRIBUTE = "ownerName";
  private static final String API_NAMESPACE_PACKAGE_PATH_ATTRIBUTE = "packagePath";

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("api.namespace.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("api.namespace.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("api.namespace.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final com.intellij.codeInspection.ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {

      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        if (!isEndpointClass(annotation)) {
          return;
        }

        if(!annotation.getQualifiedName().equals(GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE)) {
          return;
        }

        PsiAnnotationParameterList annotationMemberValue = annotation.getParameterList();
        if(annotationMemberValue == null) {
          return;
        }

        PsiAnnotationMemberValue ownerDomainMember =
          annotation.findAttributeValue(API_NAMESPACE_DOMAIN_ATTRIBUTE);
        if(ownerDomainMember == null) {
          return;
        }
        String ownerDomainWithQuotes = ownerDomainMember.getText();

        PsiAnnotationMemberValue ownerNameMember =
          annotation.findAttributeValue(API_NAMESPACE_NAME_ATTRIBUTE);
        if (ownerNameMember == null) {
          return;
        }
        String ownerNameWithQuotes = ownerNameMember.getText();

        String ownerDomain =
          EndpointUtilities.removeBeginningAndEndingQuotes(ownerDomainWithQuotes);
        String ownerName =
          EndpointUtilities.removeBeginningAndEndingQuotes(ownerNameWithQuotes);
        // Package Path has a default value of ""
        String packagePath = EndpointUtilities.removeBeginningAndEndingQuotes(
          annotation.findAttributeValue(API_NAMESPACE_PACKAGE_PATH_ATTRIBUTE).getText());

        boolean allUnspecified =
          ownerDomain.isEmpty() && ownerName.isEmpty() && packagePath.isEmpty();
        boolean ownerFullySpecified =
          !ownerDomain.isEmpty() && !ownerName.isEmpty();

        // Either everything must be fully unspecified or owner domain/name must both be specified.
        if (!allUnspecified && !ownerFullySpecified) {
          // TODO: Add quick fix.
          holder.registerProblem(annotation, "Invalid namespace configuration. If a namespace is set,"
            + " make sure to set an Owner Domain and Name. Package Path is optional.", LocalQuickFix.EMPTY_ARRAY);

        }
      }
    };
  }
}
