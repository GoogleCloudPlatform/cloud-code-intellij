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

package com.google.cloud.tools.intellij.appengine.java.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;

/** The base class for all endpoint inspections. */
public class EndpointInspectionBase extends LocalInspectionTool {

  public static final Logger LOG = Logger.getInstance(EndpointInspectionBase.class);

  @Override
  public String getGroupDisplayName() {
    return "Google Cloud Platform";
  }

  /** Get the project for the hierarchy. */
  public Project getProject(PsiElement element) {
    Project project;
    try {
      project = element.getContainingFile().getProject();
      if (project == null) {
        return null;
      }
    } catch (PsiInvalidElementAccessException ex) {
      LOG.error("Error getting project with annotation " + element.getText(), ex);
      return null;
    }

    return project;
  }
}
