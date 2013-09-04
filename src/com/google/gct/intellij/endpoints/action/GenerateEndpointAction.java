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

package com.google.gct.intellij.endpoints.action;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.generator.AppEngineEndpointGenerator;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;
import com.google.gct.intellij.endpoints.util.PsiUtils;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Action to generate an Endpoint class from an Entity class
 */
public class GenerateEndpointAction extends AnAction {

  private static final String ERROR_MESSAGE_TITLE = "Failed to Generate Endpoint Class";
  private static final Logger LOG = Logger.getInstance(GenerateEndpointAction.class);

  @Override
  public void actionPerformed(AnActionEvent e) {
    // TODO: check if app engine module in here as well
    PsiJavaFile psiJavaFile = getPsiJavaFileFromContext(e);
    final Project project = e.getProject();
    final Module module = e.getData(LangDataKeys.MODULE);

    if (psiJavaFile == null || module == null) {
      Messages.showErrorDialog(project, "Please select a Java file to create an Endpoint for", ERROR_MESSAGE_TITLE);
      return;
    }

    final AppEngineMavenProject appEngineMavenProject = AppEngineMavenProject.get(module);
    if (appEngineMavenProject == null) {
      Messages.showErrorDialog(project, "Please select a valid Maven enabled App Engine module", ERROR_MESSAGE_TITLE);
      return;
    }

    PsiClass entityClass = PsiUtils.getPublicAnnotatedClass(psiJavaFile, GctConstants.APP_ENGINE_ANNOTATION_ENTITY);
    if (entityClass == null) {
      Messages.showErrorDialog(project, "No JPA @Entity Class found in " + psiJavaFile.getName(), ERROR_MESSAGE_TITLE);
      return;
    }

    PsiField idField = PsiUtils.getAnnotatedFieldFromClass(entityClass, GctConstants.APP_ENGINE_ANNOTATION_ID);

    if (idField == null) {
      Messages.showErrorDialog(project, "No JPA @Id found in " + psiJavaFile.getName(), ERROR_MESSAGE_TITLE);
      return;
    }

    // TODO: this check is a little strange, but maybe necessary for information sake?
    if (idField.getType().getCanonicalText().equals(GctConstants.APP_ENGINE_TYPE_KEY)) {
      int retVal = Messages.showOkCancelDialog(project, "Endpoints with @Id of type " + GctConstants.APP_ENGINE_TYPE_KEY + " are not fully " +
                                          "compatible with the generation template and may require some user modification to work",
                                  "Warning", "Continue", "Cancel", Messages.getWarningIcon());
      if(retVal != 0) {
        return;
      }
    }
    doAction(project, appEngineMavenProject, entityClass, idField);
  }

  void doAction(final Project project, final AppEngineMavenProject appEngineMavenProject, PsiClass entityClass, PsiField idField) {
    AppEngineEndpointGenerator generator = new AppEngineEndpointGenerator(appEngineMavenProject);
    try {
      generator.generateEndpoint(entityClass, idField);
    } catch(IOException e) {
      Messages.showErrorDialog(project, "Failed to generated an Endpoint class because : " + e.getMessage(), ERROR_MESSAGE_TITLE);
      LOG.error(e);
    }
  }

  @Nullable
  private static PsiJavaFile getPsiJavaFileFromContext(AnActionEvent e) {
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    if (psiFile == null || !(psiFile instanceof PsiJavaFile)) {
      return null;
    }
    else {
      return (PsiJavaFile) psiFile;
    }
  }
}
