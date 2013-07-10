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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gct.intellij.endpoints.EndpointsConstants;
import com.google.gct.intellij.endpoints.templates.TemplateHelper;
import com.google.gct.intellij.endpoints.util.GradleUtils;
import com.google.gct.intellij.endpoints.util.MavenUtils;
import com.google.gct.intellij.endpoints.util.PsiUtils;
import com.google.gct.intellij.endpoints.util.ResourceUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

import org.jetbrains.idea.maven.wizards.MavenProjectBuilder;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Functionality to create the AppEngine part of the GCM/Endpoints project additions
 */
public class MavenBackendGeneratorHelper {

  public static final String[] SAMPLE_ENTITY_NAMES = {"DeviceInfo", "MessageData"};
  public static final String[] SAMPLE_ENDPOINTS_NAMES = {"DeviceInfoEndpoint", "MessageEndpoint"};

  // TODO: Use these to drive the api name replacements in the endpoint files
  public static final String[] SAMPLE_API_NAMES = {"deviceinfoendpoint", "messageEndpoint"};

  private static final Logger LOG = Logger.getInstance(MavenBackendGeneratorHelper.class);

  public static String getAppEngineModuleName(String androidModuleName) {
    return androidModuleName + EndpointsConstants.APP_ENGINE_MODULE_SUFFIX;
  }

  /** Create the required directory stucture for the AppEngine backend */
  public static VirtualFile createAppEngineDirStructure(final Project p, final Module androidModule) {

    final VirtualFile appEngineModuleRootDir = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {

        final String appEngineModuleName = getAppEngineModuleName(androidModule.getName());

        try {

          // Create the root directory
          VirtualFile appEngineModuleRootDir = p.getBaseDir().createChildDirectory(null, appEngineModuleName);

          // Create the generator structure
          VirtualFile mavenRoot = appEngineModuleRootDir
            .createChildDirectory(null, EndpointsConstants.APP_ENGINE_SRC_DIR)
            .createChildDirectory(null, EndpointsConstants.APP_ENGINE_MAIN_DIR);
          VirtualFile javaRoot = mavenRoot.createChildDirectory(null, EndpointsConstants.APP_ENGINE_JAVA_DIR);

          VirtualFile resourcesRoot = mavenRoot.createChildDirectory(null, EndpointsConstants.APP_ENGINE_RES_DIR);
          // Add META-INF
          resourcesRoot.createChildDirectory(null, EndpointsConstants.APP_ENGINE_META_INF_DIR);

          VirtualFile webapp = mavenRoot.createChildDirectory(null, EndpointsConstants.APP_ENGINE_WEBAPP_DIR);
          return appEngineModuleRootDir;

        }
        catch (IOException e) {
          LOG.error(e);
          return null;
        }
      }
    });

    return appEngineModuleRootDir;
  }


  /**
   * Add AppEngine template code in
   * @param project
   * @param appEngineModuleDir
   * @param rootPackage
   * @param appId  The App Engine application Id
   * @param apiKey The API key from cloud console
   */
  public static void addAppEngineSampleCode(final Project project,
                                            final VirtualFile appEngineModuleDir,
                                            final String rootPackage,
                                            final String appId,
                                            final String apiKey) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {

        LocalFileSystem fs = LocalFileSystem.getInstance();
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDirectory moduleRootDir = psiManager.findDirectory(appEngineModuleDir);
        PsiDirectory javaSrcDirectory = psiManager.findDirectory(appEngineModuleDir.findFileByRelativePath("src/main/java"));
        PsiDirectory resourcesDirectory = psiManager.findDirectory(appEngineModuleDir.findFileByRelativePath("src/main/resources"));
        PsiDirectory webappDirectory = psiManager.findDirectory(appEngineModuleDir.findFileByRelativePath("src/main/webapp"));

        try {


          // Create package directory
          PsiDirectory curPackageDir = javaSrcDirectory;
          for (String packageComponent : rootPackage.split("\\.")) {
            curPackageDir = curPackageDir.createSubdirectory(packageComponent);
          }

          // Add entity manager
          curPackageDir.add(TemplateHelper.loadJpaEntityManagerFactoryClass(project, rootPackage));

          TemplateHelper.EndpointPackageInfo endpointPackageInfo = TemplateHelper.getEndpointPackageInfo(rootPackage);

          // Add entities
          for (String entityName : SAMPLE_ENTITY_NAMES) {
            curPackageDir.add(TemplateHelper.generateJavaTemplateContentWithOwnerDomain(project, entityName, rootPackage,
                                                                                        endpointPackageInfo));
          }

          // Add endpoints
          for (String endpointName : SAMPLE_ENDPOINTS_NAMES) {
            curPackageDir.add(TemplateHelper.generateJavaSampleTemplateWithOwnerDomainAndApiKey(project, endpointName, rootPackage,
                                                                                                endpointPackageInfo, apiKey));
          }

          // Add/replace persistence.xml
          final PsiDirectory metaInfDir = resourcesDirectory.findSubdirectory(EndpointsConstants.APP_ENGINE_META_INF_DIR);
          PsiUtils.addOrReplaceFile(metaInfDir, TemplateHelper.loadPersistenceXml(project));

          // Add static content

          // css
          PsiDirectory cssDir = webappDirectory.createSubdirectory(EndpointsConstants.APP_ENGINE_CSS_DIR);
          cssDir.add(TemplateHelper.generateStaticContent(project, "bootstrap.min.css"));

          // js
          PsiDirectory jsDir = webappDirectory.createSubdirectory(EndpointsConstants.APP_ENGINE_JS_DIR);
          jsDir.add(TemplateHelper.generateStaticContent(project, "bootstrap.min.js"));
          jsDir.add(TemplateHelper.generateStaticContent(project, "jquery-1.9.0.min.js"));

          // images (to support twitter bootstrap)
          VirtualFile moduleImgDir = webappDirectory.getVirtualFile().createChildDirectory(null, EndpointsConstants.APP_ENGINE_IMG_DIR);
          File img = new File(moduleImgDir.getPath() + "/glyphicons-halflings.png");
          Files.write(Resources.toByteArray(TemplateHelper.class.getResource("glyphicons-halflings.png")), img);

          img = new File(moduleImgDir.getPath() + "/glyphicons-halflings-white.png");
          Files.write(Resources.toByteArray(TemplateHelper.class.getResource("glyphicons-halflings-white.png")), img);
          moduleImgDir.refresh(false, true);

          // xml
          PsiDirectory webInfDirectory = webappDirectory.createSubdirectory(EndpointsConstants.APP_ENGINE_WEB_INF_DIR);
          webInfDirectory.add(TemplateHelper.loadWebXml(project));
          webInfDirectory.add(TemplateHelper.generateAppEngineWebXml(project, appId));

          // html
          webappDirectory.add(TemplateHelper.generateStaticContent(project, "index.html"));
        }
        catch (IOException ioe) {
          LOG.error(ioe);
        }
      }
    });
  }

  /**
   * Add in the maven related templates to the AppEngine project
   * @param project
   * @param appEngineModuleDir
   * @param rootPackage
   */
  // TODO: Allow user to choose App Engine Version
  public static void addMavenFunctionality(final Project project, final VirtualFile appEngineModuleDir, final String rootPackage) {


    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        try {

          PsiManager psiManager = PsiManager.getInstance(project);
          PsiDirectory moduleDirectory = psiManager.findDirectory(appEngineModuleDir);
          moduleDirectory.add(TemplateHelper.generatePomXml(project, rootPackage, appEngineModuleDir.getName()));

          // TODO: Temporary workaround to satisfy the Gradle builder
          moduleDirectory.add(TemplateHelper.loadGradleBuildFile(project));

          VirtualFile settingsGradlevFile = project.getBaseDir().findFileByRelativePath("settings.gradle");
          if (settingsGradlevFile != null) {
            GradleUtils.includeModule(project, (GroovyFile) PsiManager.getInstance(project).findFile(settingsGradlevFile), appEngineModuleDir.getName());
          }

        }
        catch (IOException ioe) {
          LOG.error(ioe);
        }
      }
    });

    MavenProjectBuilder projectBuilder = new MavenProjectBuilder();

    try {
      projectBuilder.setRootDirectory(project, appEngineModuleDir.getPath());

      List<Module> modules = projectBuilder.commit(project, null, new DefaultModulesProvider(project));
      project.save();
    }
    catch (ConfigurationException e2) {
      LOG.error(e2);
    }
  }

  /**
   * NOTE: Needs to to be invoked via the dispatch thread
   * Build the App Engine module and generate the client libraries (locally to that module)
   *
   * @param project
   * @param appEngineModuleRootDir
   * @param callback
   */
  public static void buildModuleAndGenClientlibs(final Project project,
                                                 final VirtualFile appEngineModuleRootDir,
                                                 final MavenUtils.MavenBuildCallback callback) {


    List<String> goals = new ArrayList<String>();

    goals.add("compile");
    goals.add("appengine:endpoints_get_client_lib");

    MavenUtils.runMavenBuilder(project, appEngineModuleRootDir, goals, callback);
  }

}
