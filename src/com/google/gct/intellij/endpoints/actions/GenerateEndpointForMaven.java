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

package com.google.gct.intellij.endpoints.actions;

import com.google.gct.intellij.endpoints.generator.MavenEndpointGeneratorHelper;
import com.google.gct.intellij.endpoints.util.MavenUtils;
import com.google.gct.intellij.endpoints.util.PsiUtils;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.Nullable;

/**
 * Action to create an endpoint class from a JPA Entity class
 */

public class GenerateEndpointForMaven extends AnAction {

  private static final String ANNOTATION_ENTITY_FQN = "javax.persistence.Entity";
  private static final String ANNOTATION_ID_FQN = "javax.persistence.Id";
  //private static final String TYPE_APP_ENGINE_KEY = "com.google.appengine.api.datastore.Key";

  @Override
  public void actionPerformed(AnActionEvent e) {
    // TODO: check if app engine module in here as well
    PsiJavaFile psiJavaFile = getPsiJavaFileFromContext(e);
    Module module = ModuleUtil.findModuleForPsiElement(psiJavaFile);
    Project project = psiJavaFile.getProject();
    if (e == null) {
      Messages.showMessageDialog(project, "Not a valid Java File", "Error", Messages.getErrorIcon());
      return;
    }
    PsiClass entityClass = PsiUtils.getPublicAnnotatedClass(psiJavaFile, ANNOTATION_ENTITY_FQN);
    if(entityClass == null) {
      Messages.showMessageDialog(project, "Could not find an Entity Class in " + psiJavaFile.getName(), "Error", Messages.getErrorIcon());
      return;
    }
    // TODO: I think entity enforced the @Id requirement, so this shouldn't return null, but maybe we should check it
    PsiField idField = PsiUtils.getAnnotatedFieldFromClass(entityClass, ANNOTATION_ID_FQN);

    // TODO: this check is a little off, perhaps we shouldn't disallow it: warn the user or document it
    //if (idField.getType().getCanonicalText().equals(TYPE_APP_ENGINE_KEY)) {
    //  Messages.showMessageDialog(project, "Sorry, unable to generate endpoint for @Entity " + entityClass.getName() +
    //                                      " because\n" + TYPE_APP_ENGINE_KEY + " is not a valid @Id field type." +
    //                                      "\nPlease use java.lang.Long or java.lang.String",
    //      "Error", Messages.getErrorIcon());
    //  return;
    //}
    MavenEndpointGeneratorHelper endpointGenerator = new MavenEndpointGeneratorHelper(project, module);
    endpointGenerator.generateEndpoint(entityClass, idField);
  }

  /** Only active on JPA Entity classes in a AppEngine maven project */
  @Override
  public void update(AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE);
    final Project project = e.getData(DataKeys.PROJECT);

    if (!MavenUtils.isMavenProjectWithAppEnginePlugin(project, module)) {
      e.getPresentation().setEnabled(false);
      return;
    }

    PsiJavaFile psiJavaFile = getPsiJavaFileFromContext(e);
    if(psiJavaFile == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabled(PsiUtils.getPublicAnnotatedClass(psiJavaFile, ANNOTATION_ENTITY_FQN) != null);
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
