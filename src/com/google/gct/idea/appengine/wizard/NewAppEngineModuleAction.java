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

import com.android.builder.model.SourceProvider;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.android.tools.idea.templates.*;
import com.android.tools.idea.wizard.NewTemplateObjectWizard;
import com.google.gct.idea.appengine.run.AppEngineRunConfiguration;
import com.google.gct.idea.appengine.run.AppEngineRunConfigurationType;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.tools.idea.templates.KeystoreUtils.getDebugKeystore;

/**
 * Action to generate client libraries for an AppEngine endpoints project and copy them to an associated android project
 * TODO : No Longer in use, transition constants to WizardPath and remove this mechanism
 */
public class NewAppEngineModuleAction extends AnAction {

  private static final String ERROR_MESSAGE_TITLE = "New App Engine Module";
  private static final Logger LOG = Logger.getInstance(NewAppEngineModuleAction.class);
  public static final String ATTR_MODULE_NAME = "moduleName";
  public static final String ATTR_CLIENT_MODULE_NAME = "clientModuleName";
  public static final String ATTR_CLIENT_PACKAGE = "clientPackageName";
  public static final String ATTR_SERVER_MODULE_PATH = "serverModulePath";

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();

    // TODO : Make sure we can add to this project, we want to add to Android Gradle projects

    AppEngineModuleWizard dialog = new AppEngineModuleWizard(project);
    dialog.show();

    if (dialog.isOK()) {
      doAction(project, dialog);
    }

  }

  void doAction(@NotNull final Project project, final AppEngineModuleWizard dialog) {
    createModule(project, dialog.getTemplate(), dialog.getModuleName(), dialog.getPackageName(), "");
  }

  public static void createModule(final Project project, File templateFile, final String moduleName, String packageName,
                                  final String clientModuleName) {
    final Template template = Template.createFromPath(templateFile);
    final Template clientTemplate = AppEngineTemplates.getClientModuleTemplate(templateFile.getName());
    final Module clientModule = StringUtil.isEmpty(clientModuleName)
                                ? null
                                : ModuleManager.getInstance(project).findModuleByName(clientModuleName);

    final File projectRoot = new File(project.getBasePath());
    final File moduleRoot = new File(projectRoot, moduleName);
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
    replacementMap.put(ATTR_MODULE_NAME, moduleName);
    replacementMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, packageName);
    addPropertiesFromClientModule(clientModule, replacementMap);

    if (AppEngineTemplates.LOCAL_ENDPOINTS_TEMPLATES.contains(templateFile.getName())) {
      AppEngineTemplates.populateEndpointParameters(replacementMap, packageName);
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        List<File> allFilesToOpen = new ArrayList<File>();
        template.render(projectRoot, moduleRoot, replacementMap);
        allFilesToOpen.addAll(template.getFilesToOpen());

        if (clientModule != null) {
          patchClientModule(clientModule, clientTemplate, replacementMap);
          allFilesToOpen.addAll(clientTemplate.getFilesToOpen());
        }

        TemplateUtils.openEditors(project, allFilesToOpen, true);

        GradleProjectImporter projectImporter = GradleProjectImporter.getInstance();
        projectImporter.requestProjectSync(project, new GradleSyncListener() {
          @Override
          public void syncStarted(@NotNull Project project) {
          }

          @Override
          public void syncEnded(@NotNull final Project project) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                Module module = ModuleManager.getInstance(project).findModuleByName(moduleName);

                Parameter appEngineVersionParam = template.getMetadata().getParameter("appEngineVersion");
                String appEngineVersion = (appEngineVersionParam == null) ? "unknown" : appEngineVersionParam.initial;

                createRunConfiguration(project, module, moduleRoot, appEngineVersion);
                addAppEngineGradleFacet();
              }
            });
          }

          @Override
          public void syncFailed(@NotNull Project project, @NotNull final String errorMessage) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                Messages.showErrorDialog("Error importing App Engine module : " + errorMessage, ERROR_MESSAGE_TITLE);
              }
            });
          }
        });
      }
    });
  }

  private static void addPropertiesFromClientModule(@Nullable Module clientModule, Map<String, Object> replacementMap) {
    replacementMap.put(TemplateMetadata.ATTR_DEBUG_KEYSTORE_SHA1, "");
    replacementMap.put(ATTR_CLIENT_PACKAGE, "");
    if (clientModule != null) {
      AndroidFacet facet = AndroidFacet.getInstance(clientModule);
      if (facet != null) {
        try {
          replacementMap.put(TemplateMetadata.ATTR_DEBUG_KEYSTORE_SHA1, KeystoreUtils.sha1(getDebugKeystore(facet)));
        }
        catch (Exception e) {
          LOG.info("Failed to calculate SHA-1 of debug keystore", e);
        }
        Manifest manifest = facet.getManifest();
        if (manifest != null) {
          replacementMap.put(ATTR_CLIENT_PACKAGE, manifest.getPackage().getValue());
        }
      }
    }
  }

  private static void patchClientModule(Module clientModule, Template clientTemplate, Map<String, Object> replacementMap) {
    AndroidFacet facet = AndroidFacet.getInstance(clientModule);

    VirtualFile targetFolder = findTargetContentRoot(clientModule);
    if (targetFolder != null && facet != null) {
      String backendModulePath = (String) replacementMap.get(TemplateMetadata.ATTR_PROJECT_OUT);
      replacementMap.put(ATTR_SERVER_MODULE_PATH, FileUtil.getRelativePath(targetFolder.getPath(), backendModulePath, '/'));
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, targetFolder.getPath());
      List<SourceProvider> sourceProviders = IdeaSourceProvider.getSourceProvidersForFile(facet, targetFolder, facet.getMainSourceSet());
      SourceProvider sourceProvider = sourceProviders.get(0);
      File manifestDirectory = NewTemplateObjectWizard.findManifestDirectory(sourceProvider);
      replacementMap.put(TemplateMetadata.ATTR_MANIFEST_DIR, manifestDirectory.getPath());
      File clientContentRoot = new File(targetFolder.getPath());
      clientTemplate.render(clientContentRoot, clientContentRoot, replacementMap);
    }
  }

  @Nullable
  private static VirtualFile findTargetContentRoot(Module clientModule) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(clientModule).getContentRoots();
    for (VirtualFile contentRoot : contentRoots) {
      if (contentRoot.findChild(GradleConstants.DEFAULT_SCRIPT_NAME) != null) {
        return contentRoot;
      }
    }
    if (contentRoots.length > 0) {
      return contentRoots[0];
    }
    return null;
  }

  private static void createRunConfiguration(Project project, Module module, File moduleRoot, String appEngineVersion) {
    // Create a run configuration for this module
    final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final RunnerAndConfigurationSettings settings = runManager.
      createRunConfiguration(module.getName(), AppEngineRunConfigurationType.getInstance().getFactory());
    settings.setSingleton(true);
    final AppEngineRunConfiguration configuration = (AppEngineRunConfiguration)settings.getConfiguration();
    configuration.setModule(module);
    configuration.setWarPath(new File(moduleRoot, "build/exploded-app").getAbsolutePath());
    String gradleHomePath = GradleSettings.getInstance(project).getServiceDirectoryPath();
    if (StringUtil.isEmpty(gradleHomePath)) {
      gradleHomePath = new File(System.getProperty("user.home"), ".gradle").getAbsolutePath();
    }
    // This is a little strange because the sdk is "downloaded", but in our templates that's where the sdk is
    // TODO, perhaps extract this from the build.gradle
    // TODO, add support for the appengine environment/system properties (probably in the runconfig not here)
    configuration.setSdkPath(new File(gradleHomePath, "/appengine-sdk/appengine-java-sdk-" + appEngineVersion).getAbsolutePath());
    configuration.setServerPort("8080");
    runManager.addConfiguration(settings, false);
  }

  private static void addAppEngineGradleFacet() {
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

