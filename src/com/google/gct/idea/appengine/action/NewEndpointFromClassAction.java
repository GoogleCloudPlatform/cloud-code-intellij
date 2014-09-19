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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.dom.WebApp;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.appengine.util.AppEngineUtils;
import com.google.gct.idea.appengine.util.PsiUtils;
import com.google.gct.idea.appengine.wizard.CloudModuleUtils;
import com.google.gct.idea.appengine.wizard.CloudTemplateUtils;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Action to generate an Endpoint class from a specified class.
 */
public class NewEndpointFromClassAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(NewEndpointFromClassAction.class);

  public static final String ENTITY_NAME = "entityName";
  public static final String ENTITY_TYPE = "entityType";
  public static final String ENDPOINT_TEMPLATE = "EndpointFromClass";
  private static final String ENDPOINT_CLASS_SUFFIX = "Endpoint";
  private static final String ERROR_MESSAGE_TITLE = "Failed to Generate Endpoint Class";
  private static final String DEFAULT_ERROR_MESSAGE = "Error occurred while generating Endpoint class";
  private static final String ENDPOINTS_DEPENDENCY = "com.google.appengine:appengine-endpoints:";
  private static final String ENDPOINTS_DEPS_DEPENDENCY = "com.google.appengine:appengine-endpoints-deps:";
  private static final String OBJECTIFY_ENTITY_ANNOTATION = "com.googlecode.objectify.annotation.Entity";
  private static final String OBJECTIFY_ID_ANNOTATION = "com.googlecode.objectify.annotation.Id";
  private static final String ENDPOINTS_SERVLET_CLASS = "com.google.api.server.spi.SystemServiceServlet";
  private static final String ENDOINTS_SERVLET_NAME = "SystemServiceServlet";
  private static final String ENDPOINTS_SERVICES_INIT_PARAM_NAME = "services";

  @Override
  public void update(AnActionEvent e) {
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(shouldDisplayAction(e));
    }
  }

  private static boolean shouldDisplayAction(AnActionEvent e) {
    if (!AppEngineUtils.isAppEngineModule(e.getData(LangDataKeys.MODULE))) {
      return false;
    }
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    if (psiFile == null || !(psiFile instanceof PsiJavaFileImpl)) {
      return false;
    }
    Module srcModule = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (srcModule == null) {
      return false;
    }
    if (ProjectRootManager.getInstance(srcModule.getProject()).getFileIndex().getSourceRootForFile(psiFile.getVirtualFile()) == null) {
      return false;
    }
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    PsiJavaFile psiJavaFile = PsiUtils.getPsiJavaFileFromContext(e);
    Project project = e.getProject();
    Module module = e.getData(LangDataKeys.MODULE);

    if (psiJavaFile == null || module == null || project == null) {
      Messages.showErrorDialog(project, "Please select a Java file to create an Endpoint for.", ERROR_MESSAGE_TITLE);
      return;
    }

    if (!AppEngineUtils.isAppEngineModule(module)) {
      Messages.showErrorDialog(project, "Endpoints can only be generated for App Engine projects.", ERROR_MESSAGE_TITLE);
      return;
    }

    String packageName = psiJavaFile.getPackageName();
    PsiDirectory psiJavaFileContainingDirectory = psiJavaFile.getContainingDirectory();
    if (psiJavaFileContainingDirectory == null) {
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
    }
    String directory = psiJavaFileContainingDirectory.getVirtualFile().getPath();
    PsiClass[] psiClasses = psiJavaFile.getClasses();
    if (psiClasses.length > 1) {
      Messages.showErrorDialog(project, "We only support generating an endpoint from Java files with one top level class.",
                               "Error Generating Endpoint");
      return;
    }
    if (psiClasses.length == 0) {
      Messages.showErrorDialog(project, "This Java file does not contain any classes.", "Error Generating Endpoint");
      return;
    }
    PsiClass resourcePsiClass = psiClasses[0];
    boolean isObjectifyEntity = AnnotationUtil.isAnnotated(resourcePsiClass, OBJECTIFY_ENTITY_ANNOTATION, true);

    String idType = null;
    String idName = null;
    String idGetterName = null;
    if (isObjectifyEntity) {
      for (PsiField psiField : resourcePsiClass.getAllFields()) {
        if (AnnotationUtil.isAnnotated(psiField, OBJECTIFY_ID_ANNOTATION, false)) {
          idType = psiField.getType().getPresentableText();
          idName = psiField.getName();
        }
      }

      if (idType == null) {
        Messages.showErrorDialog(project,
                                 "Please add the required @Id annotation to your entity before trying to generate an endpoint from" +
                                 " this class.", "Error Generating Objectify Endpoint.");
        return;
      }
      idGetterName = getIdGetter(resourcePsiClass, idName);
    }

    String fileName = psiJavaFile.getName();
    String classType = fileName.substring(0, fileName.lastIndexOf('.'));
    doAction(project, module, packageName, directory, classType, isObjectifyEntity, idType, idName, idGetterName);
  }

  @Nullable
  private static String getIdGetter(@NotNull PsiClass resourcePsiClass, @NotNull String idName) {
    PsiMethod[] idGetterMethods = resourcePsiClass.findMethodsByName("get" + StringUtil.capitalize(idName), true);
    if (idGetterMethods.length == 0) {
      return null;
    }
    for (PsiMethod idGetterMethod : idGetterMethods) {
      if (idGetterMethod.getParameterList().getParametersCount() == 0) {
        return idGetterMethod.getName();
      }
    }
    return null;
  }

  /**
   * Generates an endpoint class in the specified module and updates the gradle build file
   * to include endpoint dependencies if they don't already exist.
   */
  private static void doAction(Project project,
                        Module module,
                        String packageName,
                        String directory,
                        @NonNls String classType,
                        boolean isObjectifyEntity,
                        @Nullable String idType,
                        @Nullable String idName,
                        @Nullable String idGetterName) {
    if (classType.isEmpty()) {
      Messages.showErrorDialog(project, "Class object is required for Endpoint generation", ERROR_MESSAGE_TITLE);
      return;
    }

    // Check if there is a file with the same name as the file that will contain the endpoint class
    String endpointFileName = directory + "/" + classType + ENDPOINT_CLASS_SUFFIX + ".java";
    String endpointFQClassName = packageName + "." + classType + ENDPOINT_CLASS_SUFFIX;
    File temp = new File(endpointFileName);
    if (temp.exists()) {
      Messages.showErrorDialog(project, "\'" + temp.getName() + "\" already exists", ERROR_MESSAGE_TITLE);
      return;
    }

    CloudTemplateUtils.TemplateInfo templateInfo = CloudTemplateUtils.getTemplate(ENDPOINT_TEMPLATE);

    if (templateInfo == null) {
      LOG.error("Failed to load endpoint template info: " + ENDPOINT_TEMPLATE);
      Messages.showErrorDialog(project, DEFAULT_ERROR_MESSAGE, ERROR_MESSAGE_TITLE);
      return;
    }

    final Template template = Template.createFromPath(templateInfo.getFile());
    final File projectRoot = new File(project.getBasePath());
    final File moduleRoot = new File(projectRoot, module.getName());

    // Create the replacement map
    final Map<String, Object> replacementMap = Maps.newHashMap();
    try {
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getCanonicalPath());
    }
    catch (IOException e) {
      LOG.error(e);
      Messages.showErrorDialog("Failed to resolve Module output destination : " + e.getMessage(), ERROR_MESSAGE_TITLE);
      return;
    }

    String className = String.valueOf(Character.toLowerCase(classType.charAt(0)));
    if (classType.length() > 1) {
      className += classType.substring(1);
    }
    replacementMap.put("isObjectified", isObjectifyEntity);
    replacementMap.put("idType", idType);
    replacementMap.put("idName", idName);
    replacementMap.put("idGetterName", idGetterName);
    replacementMap.put(ENTITY_NAME, className);
    replacementMap.put(ENTITY_TYPE, classType);
    replacementMap.put(TemplateMetadata.ATTR_SRC_DIR, directory);
    replacementMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, packageName);

    // Owner Domain is the reverse of package path.
    replacementMap.put(CloudModuleUtils.ATTR_ENDPOINTS_OWNER, StringUtil.join(ArrayUtil.reverseArray(packageName.split("\\.")), "."));
    replacementMap.put(CloudModuleUtils.ATTR_ENDPOINTS_PACKAGE, "");

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        template.render(projectRoot, moduleRoot, replacementMap);
      }
    });

    // Add any missing Endpoint dependency to the build file and sync
    updateBuildFile(project, module, template);
    updateWebXml(project, module, endpointFQClassName);

    // Open the new Endpoint class in the editor
    VirtualFile endpointFile = LocalFileSystem.getInstance().findFileByPath(endpointFileName);
    new ReformatCodeProcessor(project, PsiManager.getInstance(project).findFile(endpointFile), null, false).run();
    TemplateUtils.openEditor(project, endpointFile);
  }

  private static void updateWebXml(Project project, Module module, final String endpointFQClassName) {
    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
    WebApp webApp = facet.getWebXmlForEdit();
    String result = validateWebXml(webApp);
    if (result != null) {
      LOG.error(result);
      Messages.showErrorDialog(result, ERROR_MESSAGE_TITLE);
      return;
    }
    WebApp.Servlet.InitParam.ParamValue endpointServletParamValue = getEndpointServletInitParam(webApp);
    if (endpointServletParamValue == null) {
      Messages.showErrorDialog("Could not find a correctly configured SystemServiceServlet in this module's web.xml", ERROR_MESSAGE_TITLE);
    } else {
      addEndpointClassToInitParam(project, endpointServletParamValue, endpointFQClassName);
    }
  }

  @Nullable
  private static WebApp.Servlet.InitParam.ParamValue getEndpointServletInitParam(@NotNull WebApp webApp) {
    for (WebApp.Servlet servlet : webApp.getServlets()) {
      String servletName = servlet.getServletName().getValue();
      String servletClass = servlet.getServletClass().getValue();
      if (servletName == null || servletClass == null) {
        continue;
      }
      if (servletName.equals(ENDOINTS_SERVLET_NAME) && servletClass.equals(ENDPOINTS_SERVLET_CLASS)) {
        if (servlet.getInitParams() == null) {
          continue;
        }
        for (WebApp.Servlet.InitParam initParam : servlet.getInitParams()) {
          String paramName = initParam.getParamName().getValue();
          if (paramName != null && paramName.equals(ENDPOINTS_SERVICES_INIT_PARAM_NAME)) {
            return initParam.getParamValue();
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static String validateWebXml(WebApp webApp) {
    if (webApp == null) {
      return "This App Engine module's web.xml could not be updated as the file failed to load.";
    }
    if (webApp.getServlets() == null) {
      return "This App Engine module's web.xml does not contain the required SystemServiceServlet servlet.";
    }
    return null;
  }

  private static void addEndpointClassToInitParam(@NotNull Project project,
                                                  @NotNull final WebApp.Servlet.InitParam.ParamValue initParamValue,
                                                  @NotNull final String endpointFQClassName) {
    if (initParamValue.getValue().contains(endpointFQClassName)) {
      return;
    }
    final String initParamValueString =
      initParamValue.getValue().trim().isEmpty() ? endpointFQClassName : initParamValue.getValue() + ", " + endpointFQClassName;
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            initParamValue.setValue(initParamValueString);
          }
        });
      }
    }, "Update App Engine web.xml", null);
  }

  /**
   * Adds missing endpoint dependencies to gradle build file of the specified module.
   */
  private static void updateBuildFile(@NonNls Project project, @NotNull Module module, @NonNls Template template) {
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
    List<Dependency> missingDependencies = Lists.newArrayList();

    // Check if the endpoint dependencies already exist in the gradle file
    if (!gradleBuildFile.hasDependency(endpointDependency)) {
      missingDependencies.add(endpointDependency);
    }

    if (!gradleBuildFile.hasDependency(endpointDepsDependency)) {
      missingDependencies.add(endpointDepsDependency);
    }

    // Add the missing dependencies to the gradle build file
    if (!missingDependencies.isEmpty()) {
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
