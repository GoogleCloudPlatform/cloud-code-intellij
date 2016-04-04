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
import com.google.common.annotations.VisibleForTesting;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptorBase;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiUtilBase;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 *  Inspection to check that a specified method name provided in @ApiMethod
 *  is in the correct pattern.
 */
public class MethodNameInspection extends EndpointInspectionBase {
  private static final String API_NAME_ATTRIBUTE = "name";
  private static final Pattern API_NAME_PATTERN = Pattern.compile("^\\w+(\\.\\w+)*$");

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("method.name.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("method.name.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("method.name.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {
      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        if (!EndpointUtilities.isEndpointClass(annotation)) {
          return;
        }

        if(!GctConstants.APP_ENGINE_ANNOTATION_API_METHOD.equals(annotation.getQualifiedName())) {
          return;
        }

        PsiAnnotationMemberValue memberValue = annotation.findAttributeValue(API_NAME_ATTRIBUTE);
        if(memberValue == null) {
          return;
        }

        String nameValueWithQuotes = memberValue.getText();
        String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);
        if(nameValue.isEmpty()) {
          return;
        }

        if (!API_NAME_PATTERN.matcher(EndpointUtilities.collapseSequenceOfDots(nameValue)).matches()) {
          holder.registerProblem(memberValue, "Invalid method name: letters, digits, underscores and dots are acceptable characters. " +
            "Leading and trailing dots are prohibited.", new MyQuickFix());
        }
      }
    };
  }

  /**
   * Quick fix for {@link MethodNameInspection} problems by providing a valid API method name.
   */
  public class MyQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Rename API method";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    /**
     * Provides a replacement API method name that matches the {@link MethodNameInspection.API_NAME_PATTERN}.
     * @param project    {@link com.intellij.openapi.project.Project}
     * @param descriptor problem reported by the tool which provided this quick fix action
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if (element == null) {
        return;
      }

      Editor editor = PsiUtilBase.findEditor(element);
      if (editor == null) {
        return;
      }

      TextRange textRange = ((ProblemDescriptorBase)descriptor).getTextRange();
      editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());

      String wordWithQuotes = editor.getSelectionModel().getSelectedText();
      String word = EndpointUtilities.removeBeginningAndEndingQuotes(wordWithQuotes);

      if (word == null || StringUtil.isEmpty(word)) {
        return;
      }

      String variant = "\"" + getMethodNameSuggestions(word) + "\"";
      LookupManager lookupManager = LookupManager.getInstance(project);
      lookupManager.showLookup(editor, LookupElementBuilder.create(variant));
    }

    /**
     * Returns a valid API method name. An empty string is valid.
     */
    @VisibleForTesting
    public String getMethodNameSuggestions(String baseString) {
      if(baseString == null) {
        return null;
      }

      if(baseString.isEmpty()) {
        return "";
      }

      // Remove illegal characters
      String noInvalidChars = baseString.replaceAll("[^a-zA-Z0-9_.]+","");
      if(noInvalidChars.isEmpty()) {
        return "";
      }

      if(API_NAME_PATTERN.matcher(noInvalidChars).matches()) {
        return noInvalidChars;
      }
      baseString = noInvalidChars;

      // Replace sequence of dots with single dot
      String noSequencingDots = EndpointUtilities.collapseSequenceOfDots(baseString);
      if(API_NAME_PATTERN.matcher(noSequencingDots).matches()) {
        return noSequencingDots;
      }
      baseString = noSequencingDots;


      // If name starts with ".", remove starting "."
      if(baseString.startsWith(".")) {
        String noLeadingDots = baseString.substring(1);

        if(API_NAME_PATTERN.matcher(noLeadingDots).matches()) {
          return noLeadingDots;
        }

        if (noLeadingDots.isEmpty()) {
          return "";
        }
        baseString = noLeadingDots;
      }

      // If name ends with ".", remove ending "."
      if (baseString.endsWith(".")) {
        String noTrailingDots = baseString.substring(0, baseString.length() - 1);

        if(API_NAME_PATTERN.matcher(noTrailingDots).matches()) {
          return noTrailingDots;
        }

        if (noTrailingDots.isEmpty()) {
          return "";
        }
      }
      return "";
    }
  }
}
