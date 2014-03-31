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

package com.google.gct.idea.appengine.wizard;

import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.templates.Parameter;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateMetadata;

import com.google.gct.idea.appengine.run.AppEngineRunConfiguration;
import com.google.gct.idea.appengine.run.AppEngineRunConfigurationType;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Action to generate client libraries for an AppEngine endpoints project and copy them to an associated android project
 * Currently hidden from UI in plugin.xml
 */
public class NewAppEngineModuleAction extends AnAction {

  private static final String ERROR_MESSAGE_TITLE = "New App Engine Module";
  private static final Logger LOG = Logger.getInstance(NewAppEngineModuleAction.class);
  private static final String ATTR_MODULE_NAME = "moduleName";

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();

    // TODO : Make sure we can add to this project, we want to add to Android Gradle projects

    AppEngineModuleWizard dialog = new AppEngineModuleWizard(project);
    dialog.show();

    if(dialog.isOK()) {
      doAction(project, dialog);
    }

  }

  void doAction(@NotNull final Project project, final AppEngineModuleWizard dialog) {
    final Template template = Template.createFromPath(dialog.getTemplate());

    final File projectRoot = new File(project.getBasePath());
    final File moduleRoot = new File(projectRoot, dialog.getModuleName());
    FileUtil.createDirectory(moduleRoot);

    final Map<String, Object> replacementMap = new HashMap<String, Object>();
    try {
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getCanonicalPath());
    }
    catch (IOException e) {
      Messages.showErrorDialog("Failed to resolve Module output destination : " + e.getMessage(), ERROR_MESSAGE_TITLE);
      LOG.error(e);
      return;
    }
    replacementMap.put(ATTR_MODULE_NAME, dialog.getModuleName());
    replacementMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, dialog.getPackageName());

    if(AppEngineTemplates.LOCAL_ENDPOINTS_TEMPLATES.contains(dialog.getTemplate().getName())) {
      AppEngineTemplates.populateEndpointParameters(replacementMap, dialog.getPackageName());
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        template.render(projectRoot, moduleRoot, replacementMap);
        GradleProjectImporter projectImporter = GradleProjectImporter.getInstance();
        try {
          projectImporter.reImportProject(project, new GradleProjectImporter.Callback() {
            @Override
            public void projectImported(@NotNull final Project project) {
              ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                  Module module = ModuleManager.getInstance(project).findModuleByName(dialog.getModuleName());

                  Parameter appEngineVersionParam = template.getMetadata().getParameter("appEngineVersion");
                  String appEngineVersion = (appEngineVersionParam == null) ? "unknown" : appEngineVersionParam.initial;

                  createRunConfiguration(project, module, moduleRoot, appEngineVersion);
                  addAppEngineGradleFacet();
                }
              });
            }

            @Override
            public void importFailed(@NotNull Project project, @NotNull final String errorMessage) {
              ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                  Messages.showErrorDialog("Error importing App Engine module : " + errorMessage, ERROR_MESSAGE_TITLE);
                }
              });
            }
          });
        }
        catch (ConfigurationException e) {
          Messages.showErrorDialog(e.getMessage(), ERROR_MESSAGE_TITLE);
          LOG.error(e);
        }
      }
    });
  }

  private void createRunConfiguration(Project project, Module module, File moduleRoot, String appEngineVersion) {
    // Create a run configuration for this module
    final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final RunnerAndConfigurationSettings settings = runManager.
      createRunConfiguration(module.getName(), AppEngineRunConfigurationType.getInstance().getFactory());
    settings.setSingleton(true);
    final AppEngineRunConfiguration configuration = (AppEngineRunConfiguration)settings.getConfiguration();
    configuration.setModule(module);
    configuration.setWarPath(new File(moduleRoot, "build/exploded-app").getAbsolutePath());
    String gradleHomePath = GradleSettings.getInstance(project).getServiceDirectoryPath();
    if(StringUtil.isEmpty(gradleHomePath)) {
      gradleHomePath = new File(System.getProperty("user.home"), ".gradle").getAbsolutePath();
    }
    // This is a little strange because the sdk is "downloaded", but in our templates that's where the sdk is
    // TODO, perhaps extract this from the build.gradle
    // TODO, add support for the appengine environment/system properties (probably in the runconfig not here)
    configuration.setSdkPath(new File(gradleHomePath, "/appengine-sdk/appengine-java-sdk-" + appEngineVersion).getAbsolutePath());
    configuration.setServerPort("8080");
    runManager.addConfiguration(settings, false);
  }

  private void addAppEngineGradleFacet() {
    // Module does not have AppEngine-Gradle facet. Create one and add it.
    // Commented out for now, ENABLE when AppEngine Gradle facet is ready.
    // FacetManager facetManager = FacetManager.getInstance(module);
    // ModifiableFacetModel model = facetManager.createModifiableModel();
    //try {
    //  Facet facet = facetManager.createFacet(AppEngineGradleFacet.getFacetType(), AppEngineGradleFacet.NAME, null);
    //  model.addFacet(facet);
    //} finally {
    //  model.commit();
    //}
  }
}

