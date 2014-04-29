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

import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.appengine.GctConstants;
import com.google.gct.idea.appengine.util.EndpointBundle;
import com.google.gct.idea.appengine.util.EndpointUtilities;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptorBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiUtilBase;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Inspection class for validating endpoint API name.
 */
public class ApiNameInspection extends EndpointInspectionBase {
  private static final String API_NAME_ATTRIBUTE = "name";
  private static final Pattern API_NAME_PATTERN = Pattern.compile("^[a-z]+[A-Za-z0-9]*$");

  @Override
  @Nullable
  public String getStaticDescription() {
    return EndpointBundle.message("api.name.description");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return EndpointBundle.message("api.name.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return EndpointBundle.message("api.name.short.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final com.intellij.codeInspection.ProblemsHolder holder, boolean isOnTheFly) {
    return new EndpointPsiElementVisitor() {

      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        if(annotation == null) {
          return;
        }

        String annotationQualifiedName = annotation.getQualifiedName();
        if(annotationQualifiedName == null) {
          return;
        }

        if(!annotationQualifiedName.equals(GctConstants.APP_ENGINE_ANNOTATION_API)) {
          return;
        }

        // Need to check for user added attributes because default values are used when not
        // specified by user and we are only interested in the user specified values
        PsiAnnotationParameterList parameterList = annotation.getParameterList();
        if(parameterList.getAttributes().length == 0) {
          return;
        }

        PsiAnnotationMemberValue annotationMemberValue = annotation.findAttributeValue(API_NAME_ATTRIBUTE);
        if(annotationMemberValue == null) {
          return;
        }

        String nameValueWithQuotes = annotationMemberValue.getText();
        String nameValue = EndpointUtilities.removeBeginningAndEndingQuotes(nameValueWithQuotes);

        // Empty API name is valid
        if(nameValue.isEmpty()) {
          return;
        }

        if (!API_NAME_PATTERN.matcher(nameValue).matches()) {
          holder.registerProblem(annotationMemberValue, "Invalid api name: it must start with a lower case letter and consists only of letter and digits",
            new MyQuickFix());
        }

      }
    };
  }

  public class MyQuickFix implements LocalQuickFix {
    private static final String DEFAULT_API_NAME = "myApi";

    public MyQuickFix() {

    }

    @NotNull
    @Override
    public String getName() {
      return getFamilyName() + ": Rename API name";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getDisplayName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      if (element == null) return;

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

      String variant = "\"" + getNameSuggestions(word) + "\"";
      LookupManager lookupManager = LookupManager.getInstance(project);
      lookupManager.showLookup(editor, LookupElementBuilder.create(variant));
    }

    @VisibleForTesting
    public String getNameSuggestions(String baseString) {
      if(baseString.isEmpty()) {
        return DEFAULT_API_NAME;
      }

      // If name consists of illegal characters, remove all illegal characters
      String noInvalidChars = baseString.replaceAll("[^a-zA-Z0-9]+","");
      if(noInvalidChars.isEmpty()) {
        return DEFAULT_API_NAME;
      }

      if(API_NAME_PATTERN.matcher(noInvalidChars).matches()) {
        return noInvalidChars;
      }
      baseString = noInvalidChars;

      // If name starts with digit, suggestion will be to remove beginning digits
      if (Character.isDigit(baseString.charAt(0))) {
        int n = 0;
        while ((n < baseString.length()) && Character.isDigit(baseString.charAt(n))) {
          n += 1;
        }
        String nonDigitName = baseString.substring(n);

        if(API_NAME_PATTERN.matcher(nonDigitName).matches()) {
          return nonDigitName;
        }

        if (nonDigitName.isEmpty()) {
          return "api" + noInvalidChars;
        }
        baseString = nonDigitName;
      }

      // If name starts with uppercase, suggestion will be to change to lower case
      if(Character.isUpperCase(baseString.charAt(0))){
        String beginWithLowerCase = baseString.substring(0, 1).toLowerCase() + baseString.substring(1);
        if(API_NAME_PATTERN.matcher(beginWithLowerCase).matches()) {
          return beginWithLowerCase;
        }
      }

      return DEFAULT_API_NAME;
    }
  }
}

