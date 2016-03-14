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
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inspection class for validating endpoint API namespace.
 */
public class ApiNamespaceInspection extends EndpointInspectionBase{
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

      /**
       * Flags @ApiNamespace that have one or more attributes specified where both the
       * OwnerName and OwnerDomain attributes are not specified.
       */
      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        if (!EndpointUtilities.isEndpointClass(annotation)) {
          return;
        }

        if(!GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE.equals(annotation.getQualifiedName())) {
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
          holder.registerProblem(annotation, "Invalid namespace configuration. If a namespace is set,"
            + " make sure to set an Owner Domain and Name. Package Path is optional.", new MyQuickFix());

        }
      }
    };
  }

  /**
   * Quick fix for {@link ApiNameInspection} problems.
   */
  public class MyQuickFix implements LocalQuickFix {
    private static final String SUGGESTED_OWNER_ATTRIBUTE = "YourCo";
    private static final String SUGGESTED_DOMAIN_ATTRIBUTE = "your-company.com";

    public MyQuickFix() {

    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Add missing attributes";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Provides a default value for OwnerName and OwnerDomain attributes in @ApiNamespace
     * when they are not provided.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement psiElement = descriptor.getPsiElement();
      if(psiElement == null){
        return;
      }

      if(!(psiElement instanceof PsiAnnotation)) {
        return;
      }

      PsiAnnotation annotation = (PsiAnnotation)psiElement;
      if(!GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE.equals(annotation.getQualifiedName())) {
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

      if(ownerDomain.isEmpty() && ownerName.isEmpty()) {
        addOwnerDomainAndNameAttributes(project, annotation);
      } else if(ownerDomain.isEmpty() && !ownerName.isEmpty()) {
        addOwnerDomainAttribute(project, annotation);
      } else if (!ownerDomain.isEmpty() && ownerName.isEmpty()) {
        addOwnerNameAttribute(project, annotation);
      }

      return;
    }

    private void addOwnerDomainAttribute(@NotNull final Project project, final PsiAnnotation annotation) {
      new WriteCommandAction(project, annotation.getContainingFile()) {
        @Override
        protected void run(final Result result) throws Throwable {
          // @A(ownerDomain = "your-company.com")
          PsiAnnotationMemberValue newMemberValue = JavaPsiFacade.getInstance(project).getElementFactory()
            .createAnnotationFromText("@A(" + API_NAMESPACE_DOMAIN_ATTRIBUTE + " = \"" + SUGGESTED_DOMAIN_ATTRIBUTE + "\")", null).findDeclaredAttributeValue(API_NAMESPACE_DOMAIN_ATTRIBUTE);

          annotation.setDeclaredAttributeValue(API_NAMESPACE_DOMAIN_ATTRIBUTE, newMemberValue);

        }
      }.execute();
    }

    private void addOwnerNameAttribute(@NotNull final Project project, final PsiAnnotation annotation) {
      new WriteCommandAction(project, annotation.getContainingFile()) {
        @Override
        protected void run(final Result result) throws Throwable {
          // @A(ownerName = "YourCo")
          PsiAnnotationMemberValue newMemberValue = JavaPsiFacade.getInstance(project).getElementFactory()
            .createAnnotationFromText("@A(" + API_NAMESPACE_NAME_ATTRIBUTE + " = \"" + SUGGESTED_OWNER_ATTRIBUTE + "\")", null).findDeclaredAttributeValue(API_NAMESPACE_NAME_ATTRIBUTE);

          annotation.setDeclaredAttributeValue(API_NAMESPACE_NAME_ATTRIBUTE, newMemberValue);

        }
      }.execute();

    }

    private void addOwnerDomainAndNameAttributes(@NotNull final Project project, final PsiAnnotation annotation) {
      new WriteCommandAction(project, annotation.getContainingFile()) {
        @Override
        protected void run(final Result result) throws Throwable {
          // @A(ownerName = "YourCo", ownerDomain = "your-company.com")
          String annotationString = "@A(" + API_NAMESPACE_NAME_ATTRIBUTE + " = \"" +
            SUGGESTED_OWNER_ATTRIBUTE +  "\", " + API_NAMESPACE_DOMAIN_ATTRIBUTE + " = \"" + "your-company.com" + "\")";
          PsiAnnotation newAnnotation = JavaPsiFacade.getInstance(project).getElementFactory()
            .createAnnotationFromText(annotationString, null);
          PsiAnnotationMemberValue newDomainMemberValue = newAnnotation.findDeclaredAttributeValue(API_NAMESPACE_DOMAIN_ATTRIBUTE);
          PsiAnnotationMemberValue newNameMemberValue = newAnnotation.findDeclaredAttributeValue(API_NAMESPACE_NAME_ATTRIBUTE);

          annotation.setDeclaredAttributeValue(API_NAMESPACE_NAME_ATTRIBUTE, newNameMemberValue);
          annotation.setDeclaredAttributeValue(API_NAMESPACE_DOMAIN_ATTRIBUTE, newDomainMemberValue);
        }
      }.execute();
    }
  }
}
