/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.action;

import com.android.tools.idea.gradle.parser.BuildFileKey;
import com.android.tools.idea.gradle.parser.BuildFileStatement;
import com.android.tools.idea.gradle.parser.Dependency;
import com.android.tools.idea.gradle.parser.GradleBuildFile;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.templates.Parameter;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.templates.TemplateUtils;

import com.google.gct.idea.appengine.util.AppEngineUtils;
import com.google.gct.idea.appengine.util.PsiUtils;
import com.google.gct.idea.appengine.wizard.AppEngineTemplates;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action to generate an Endpoint class from a specified class.
 */
public class NewEndpointFromClassAction extends AnAction {
  public static final String ENTITY_NAME = "entityName";
  public static final String ENTITY_TYPE = "entityType";
  public static final String ENDPOINT_TEMPLATE = "RudimentaryEndpoint";

  private static final String ENDPOINT_CLASS_SUFFIX = "Endpoint.java";
  private static final String ERROR_MESSAGE_TITLE = "Failed to Generate Endpoint Class";
  private static final String DEFAULT_ERROR_MESSAGE = "Error occurred while generating Endpoint class";
  private static final String ENDPOINTS_DEPENDENCY = "com.google.appengine:appengine-endpoints:";
  private static final String ENDPOINTS_DEPS_DEPENDENCY = "com.google.appengine:appengine-endpoints-deps:";
  private static final Logger LOG = Logger.getInstance(NewEndpointFromClassAction.class);

  @Override
  public void actionPerformed(AnActionEvent e) {
    PsiJavaFile psiJavaFile = PsiUtils.getPsiJavaFileFromContext(e);
    Project project = e.getProject();
    Module module = e.getData(LangDataKeys.MODULE);

    if (psiJavaFile == null || module == null || project == null) {
      Messages.showErrorDialog(project, "Please select a Java file to create an Endpoint for.", ERROR_MESSAGE_TITLE);
      return;
    }

    try {
      if (!AppEngineUtils.isAppEngineModule(project, module)) {
        Messages.showErrorDialog(project, "Endpoints can only be generated for App Engine projects.", ERROR_MESSAGE_TITLE);
        return;
      }
    }
    catch (FileNotFoundException error) {
      LOG.error(ERROR_MESSAGE_TITLE, error);
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
      return;
    }

    String packageName = psiJavaFile.getPackageName();
    PsiDirectory psiJavaFileContainingDirectory = psiJavaFile.getContainingDirectory();
    if (psiJavaFileContainingDirectory == null) {
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
    }
    String directory = psiJavaFileContainingDirectory.getVirtualFile().getPath();

    String classType = psiJavaFile.getName();
    int lastIndexOfDot = psiJavaFile.getName().lastIndexOf('.');
    if (lastIndexOfDot > 0) {
      classType = psiJavaFile.getName().substring(0, psiJavaFile.getName().lastIndexOf('.'));
    }

    doAction(project, module, packageName, directory, classType);
  }

  /**
   * Generates an endpoint class in the specified module and updates the gradle build file
   * to include endpoint dependencies if they don't already exist.
   */
  private void doAction(Project project, Module module, String packageName, String directory, @NonNls String classType) {
    if (classType.isEmpty()) {
      Messages.showErrorDialog(project, "Class object is required for Endpoint generation", ERROR_MESSAGE_TITLE);
      return;
    }

    // Check if there is a file with the same name as the file that will contain the endpoint class
    String endpointFileName = directory +"/" + classType + ENDPOINT_CLASS_SUFFIX;
    File temp = new File(endpointFileName);
    if (temp.exists()) {
      Messages.showErrorDialog(project, "\'" + temp.getName() + "\" already exists", ERROR_MESSAGE_TITLE);
      return;
    }

    AppEngineTemplates.TemplateInfo templateInfo = AppEngineTemplates.getAppEngineTemplate(ENDPOINT_TEMPLATE);

    if (templateInfo == null) {
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
      return;
    }

    final Template template = Template.createFromPath(templateInfo.getFile());
    if (template == null) {
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
      return;
    }

    final File projectRoot = new File(project.getBasePath());
    final File moduleRoot = new File(projectRoot, module.getName());

    // Create the replacement map
    final Map<String, Object> replacementMap = new HashMap<String, Object>();
    try {
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getCanonicalPath());
    }
    catch (Exception e) {
      Messages.showErrorDialog("Failed to resolve Module output destination : " + e.getMessage(), ERROR_MESSAGE_TITLE);
      LOG.error(e);
      return;
    }

    String className = String.valueOf(Character.toLowerCase(classType.charAt(0)));
    if (classType.length() > 1) {
     className += classType.substring(1);
    }

    replacementMap.put(ENTITY_NAME, className);
    replacementMap.put(ENTITY_TYPE, classType);
    replacementMap.put(TemplateMetadata.ATTR_SRC_DIR, directory);
    replacementMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, packageName);

    AppEngineTemplates.populateEndpointParameters(replacementMap, packageName);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        template.render(projectRoot, moduleRoot, replacementMap);
      }
    });

    // Add any missing Endpoint dependency to the build file and sync
    updateBuildFile(project, module, template);

    // Open the new Endpoint class in the editor
    VirtualFile endpointFile = LocalFileSystem.getInstance().findFileByPath(endpointFileName);
    TemplateUtils.openEditor(project, endpointFile);
  }

  /**
   * Adds missing endpoint dependencies to gradle build file of the specified module.
   */
  private void updateBuildFile(@NonNls Project project, @NotNull Module module, @NonNls Template template) {
    final VirtualFile buildFile = GradleUtil.getGradleBuildFile(module);
    if (buildFile == null) {
      LOG.error("Cannot find gradle build file for module \"" + module.getName() + "\"");
      return;
    }

    Parameter appEngineVersionParam = template.getMetadata().getParameter("appEngineVersion");
    String appEngineVersion = (appEngineVersionParam == null) ? "+" : appEngineVersionParam.initial;

    final GradleBuildFile gradleBuildFile = new GradleBuildFile(buildFile, project);
    Dependency endpointDependency =
      new Dependency(Dependency.Scope.COMPILE, Dependency.Type.EXTERNAL, ENDPOINTS_DEPENDENCY + appEngineVersion);
    Dependency endpointDepsDependency =
      new Dependency(Dependency.Scope.COMPILE, Dependency.Type.EXTERNAL, ENDPOINTS_DEPS_DEPENDENCY + appEngineVersion);
    List<Dependency> missingDependencies = new ArrayList<Dependency>();

    // Check if the endpoint dependencies already exist in the gradle file
    if (!gradleBuildFile.hasDependency(endpointDependency)) {
       missingDependencies.add(endpointDependency);
    }

    if (!gradleBuildFile.hasDependency(endpointDepsDependency)) {
      missingDependencies.add(endpointDepsDependency);
    }

    // Add the missing dependencies to the gradle build file
    if (missingDependencies.size() > 0) {
      final List<BuildFileStatement> currentDependencies = gradleBuildFile.getDependencies();
      currentDependencies.addAll(missingDependencies);

      WriteCommandAction.runWriteCommandAction(project, new Runnable() {
        @Override
        public void run() {
          gradleBuildFile.setValue(BuildFileKey.DEPENDENCIES, currentDependencies);
        }
      });

      GradleProjectImporter.getInstance().requestProjectSync(project, null);
    }
  }
}
