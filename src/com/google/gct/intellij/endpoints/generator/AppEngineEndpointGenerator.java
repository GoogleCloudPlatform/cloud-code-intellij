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
package com.google.gct.intellij.endpoints.generator;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;
import com.google.gct.intellij.endpoints.generator.template.TemplateHelper;
import com.google.gct.intellij.endpoints.util.PsiUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;

/** Generator for creating Endpoint classes from Entity classes */
public class AppEngineEndpointGenerator {

  private final AppEngineMavenProject myAppEngineMavenProject;
  private final Project myProject;
  private final Module myAppEngineModule;

  private static final Logger LOG = Logger.getInstance(AppEngineEndpointGenerator.class);

  /** Constructor */
  public AppEngineEndpointGenerator(@NotNull AppEngineMavenProject appEngineMavenProject) {
    myAppEngineMavenProject = appEngineMavenProject;
    myProject = myAppEngineMavenProject.getProject();
    myAppEngineModule = myAppEngineMavenProject.getModule();
  }

  /**
   * NOTE : Cannot hold a readAction if invoking this method
   * Generates an endpoint class from an entity class, and builds
   * @param entityClass
   *    the JPA class we are creating an endpoint for
   * @param idField
   *    the specific field from the class that is the JPA @Id
   * @return
   *    The api name of the generated endpoint, useful when you want to generate the client libraries after
   */
  public String generateEndpoint(PsiClass entityClass, PsiField idField) throws IOException {

    TemplateHelper templateHelper = new TemplateHelper(entityClass, idField);
    final PsiFile serviceClassFile = templateHelper.loadJpaSwarmServiceClass();
    final PsiFile entityManagerFactoryFile = templateHelper.loadJpaEntityManagerFactoryClass();
    final PsiFile persistenceXmlFile = templateHelper.loadPersistenceXml();
    final PsiDirectory entityDir = entityClass.getContainingFile().getContainingDirectory();

    VirtualFile metaInf = myAppEngineMavenProject.getRootDirectory().findFileByRelativePath(GctConstants.APP_ENGINE_META_INF_PATH);
    if(metaInf == null) {
      throw new FileNotFoundException("App Engine META-INF directory missing, was expected at : " +
                                      GctConstants.APP_ENGINE_META_INF_PATH);
    }
    final PsiDirectory metaInfDir = PsiManager.getInstance(myProject).findDirectory(metaInf);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        PsiUtils.addOrReplaceFile(entityDir, serviceClassFile);
        PsiUtils.addIfMissingFile(entityDir, entityManagerFactoryFile);
        PsiUtils.addIfMissingFile(metaInfDir, persistenceXmlFile);
      }
    });

    return TemplateHelper.getApiNameFromEntityName(serviceClassFile.getName());
  }
}
