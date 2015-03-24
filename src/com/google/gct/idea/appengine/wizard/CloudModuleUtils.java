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
import com.android.tools.idea.gradle.invoker.GradleInvoker;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.android.tools.idea.stats.UsageTracker;
import com.android.tools.idea.templates.KeystoreUtils;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.templates.TemplateUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacetConfiguration;
import com.google.gct.idea.appengine.run.AppEngineRunConfiguration;
import com.google.gct.idea.appengine.run.AppEngineRunConfigurationType;
import com.google.gct.idea.util.GctTracking;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.indexing.IndexInfrastructure;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.android.tools.idea.templates.KeystoreUtils.getDebugKeystore;

/**
 * Utility class that generates a new Android Studio AppEngine module and copies the associated
 * client libraries into the selected client Android module. This class also defines constants
 * which are used to store state during the new module wizard workflow and are used as
 * parameters in the templates.xml.
 */
public final class CloudModuleUtils {
  public static final String ATTR_MODULE_NAME = "moduleName";
  public static final String ATTR_PACKAGE_NAME = "packageName";
  public static final String ATTR_CLIENT_PACKAGE = "clientPackageName";
  public static final String ATTR_SERVER_MODULE_PATH = "serverModulePath";
  public static final String ATTR_DOC_URL = "docUrl";
  public static final String ATTR_MODULE_TYPE = "moduleType";
  public static final String ATTR_ENDPOINTS_OWNER = "endpointOwnerDomain";
  public static final String ATTR_ENDPOINTS_PACKAGE = "endpointPackagePath";
  private static final String ERROR_MESSAGE_TITLE = "New App Engine Module";
  private static final Logger LOG = Logger.getInstance(CloudModuleUtils.class);

  static void createModule(@NotNull final Project project,
                           @NotNull final File templateFile,
                           @NotNull final String moduleName,
                           @NotNull String packageName,
                           @Nullable final String clientModuleName) {
    final File projectRoot = new File(project.getBasePath());
    final File moduleRoot = new File(projectRoot, moduleName);
    FileUtil.createDirectory(moduleRoot);

    final Map<String, Object> replacementMap = Maps.newHashMap();
    try {
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getCanonicalPath());
    }
    catch (IOException e) {
      Messages.showErrorDialog("Failed to resolve Module output destination : " + e.getMessage(), ERROR_MESSAGE_TITLE);
      LOG.error(e);
      return;
    }
    replacementMap.put(ATTR_MODULE_NAME, moduleName);
    replacementMap.put(ATTR_PACKAGE_NAME, packageName);

    final Module clientModule;
    if (Strings.isNullOrEmpty(clientModuleName)) {
      clientModule = null;
    } else {
      clientModule = ModuleManager.getInstance(project).findModuleByName(clientModuleName);
    }
    addPropertiesFromClientModule(clientModule, replacementMap);

    if (CloudTemplateUtils.LOCAL_ENDPOINTS_TEMPLATES.contains(templateFile.getName())) {
      populateEndpointParameters(replacementMap, packageName);
    }

    final Template template = Template.createFromPath(templateFile);
    final Template clientTemplate = CloudTemplateUtils.getClientModuleTemplate(templateFile.getName());
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final List<File> allFilesToOpen = Lists.newArrayList();
        template.render(projectRoot, moduleRoot, replacementMap, project);

        UsageTracker.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.WIZARD, templateFile.getName(), null);

        allFilesToOpen.addAll(template.getFilesToOpen());

        DumbService.getInstance(project).smartInvokeLater(new Runnable() {
          // because the previous template render initiates some indexing, if we don't smart invoke later we might
          // cause a IndexNotReadyException when starting this.
          @Override
          public void run() {
            if (clientModule != null) {
              patchClientModule(clientModule, clientTemplate, replacementMap, project);
              allFilesToOpen.addAll(clientTemplate.getFilesToOpen());
            }
            TemplateUtils.openEditors(project, allFilesToOpen, true);
          }
        });

        // download SDK, the appengine-gradle-plugin will know what to do
        DumbService.getInstance(project).smartInvokeLater(new Runnable() {
          @Override
          public void run() {
            GradleInvoker.getInstance(project).executeTasks(Collections.singletonList("appengineDownloadSdk"));
          }
        });
      }
    });
  }

  private static void addPropertiesFromClientModule(@Nullable Module clientModule, Map<String, Object> replacementMap) {
    replacementMap.put(TemplateMetadata.ATTR_DEBUG_KEYSTORE_SHA1, "");
    replacementMap.put(ATTR_CLIENT_PACKAGE, "");
    if (clientModule != null) {
      final AndroidFacet facet = AndroidFacet.getInstance(clientModule);
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

  private static void patchClientModule(Module clientModule, Template clientTemplate, Map<String, Object> replacementMap, Project project) {
    final AndroidFacet facet = AndroidFacet.getInstance(clientModule);
    final VirtualFile targetFolder = findTargetContentRoot(clientModule);
    if (targetFolder != null && facet != null) {
      final String backendModulePath = (String)replacementMap.get(TemplateMetadata.ATTR_PROJECT_OUT);
      replacementMap.put(ATTR_SERVER_MODULE_PATH, FileUtil.getRelativePath(targetFolder.getPath(), backendModulePath, '/'));
      replacementMap.put(TemplateMetadata.ATTR_PROJECT_OUT, targetFolder.getPath());
      final List<SourceProvider> sourceProviders =
        IdeaSourceProvider.getSourceProvidersForFile(facet, targetFolder, facet.getMainSourceProvider());
      replacementMap.put(TemplateMetadata.ATTR_MANIFEST_DIR, sourceProviders.get(0).getManifestFile().getParentFile().getPath());
      final File clientContentRoot = new File(targetFolder.getPath());
      clientTemplate.render(clientContentRoot, clientContentRoot, replacementMap, project);
    }
  }

  @Nullable
  private static VirtualFile findTargetContentRoot(Module clientModule) {
    final VirtualFile[] contentRoots = ModuleRootManager.getInstance(clientModule).getContentRoots();
    for (VirtualFile contentRoot : contentRoots) {
      if (contentRoot.findChild(GradleConstants.DEFAULT_SCRIPT_NAME) != null) {
        return contentRoot;
      }
    }
    return contentRoots.length > 0 ? contentRoots[0] : null;
  }

  private static void createRunConfiguration(@NotNull Project project, @NotNull Module module) {
    // Create a run configuration for this module
    final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final RunnerAndConfigurationSettings settings = runManager.
      createRunConfiguration(module.getName(), AppEngineRunConfigurationType.getInstance().getFactory());
    settings.setSingleton(true);
    final AppEngineRunConfiguration configuration = (AppEngineRunConfiguration)settings.getConfiguration();
    configuration.setModule(module);
    // pull configuration out of gradle
    configuration.setSyncWithGradle(true);
    runManager.addConfiguration(settings, false);
  }

  /**
   * Populate endpoints specific template parameters into the replacement map.
   */
  @VisibleForTesting
  static void populateEndpointParameters(Map<String, Object> replacementMap, String rootPackage) {
    // Owner Domain is the reverse of package path.
    replacementMap.put(ATTR_ENDPOINTS_OWNER, StringUtil.join(ArrayUtil.reverseArray(rootPackage.split("\\.")), "."));
    replacementMap.put(ATTR_ENDPOINTS_PACKAGE, "");
  }

  private CloudModuleUtils() {
    // This utility class should not be instantiated.
  }
}

