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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * The base class for all endpoint inspections.
 */
public class EndpointInspectionBase extends LocalInspectionTool {
  public static final Logger LOG =
    Logger.getInstance(EndpointInspectionBase.class);

  public Project getProject(PsiElement element ) {
    Project project;
    try {
      project = element.getContainingFile().getProject();
      if (project == null) {
        return null;
      }
    } catch (PsiInvalidElementAccessException e) {
      LOG.error("Error getting project with annotation " + element.getText(), e);
      return null;
    }

    return  project;
  }
}
