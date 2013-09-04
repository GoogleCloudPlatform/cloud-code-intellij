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
package com.google.gct.intellij.endpoints.generator.sample;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.generator.AppEngineMavenGenerator;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;
import com.google.gct.intellij.endpoints.generator.template.TemplateHelper;
import com.google.gct.intellij.endpoints.util.PsiUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Instance of AppEngineMavenGenerator that creates an AppEngine sample specifically and only for
 * the GCM sample project (CloudBackendGenerator)
 */
public class AppEngineMavenGcmGenerator extends AppEngineMavenGenerator {

  protected final String myAppId;
  protected final String myApiKey;

  public static final String[] SAMPLE_ENTITY_NAMES = {"DeviceInfo", "MessageData"};
  public static final String[] SAMPLE_ENDPOINTS_NAMES = {"DeviceInfoEndpoint", "MessageEndpoint"};
  public static final String[] SAMPLE_API_NAMES = {"deviceinfoendpoint", "messageEndpoint"};

  private static final Logger LOG = Logger.getInstance(AppEngineMavenGcmGenerator.class);

  /**
   *
   * @param project
   *    The IDEA/Studio project
   * @param moduleName
   *    The name of the AppEngine module
   * @param rootPackage
   *    The root java package that will be the base package of this module
   * @param apiKey
   *    A cloud console API key (for endpoints)
   * @param appId
   *    A app engine appID (for general app engine functionality)
   */
  public AppEngineMavenGcmGenerator(@NotNull Project project, @NotNull String moduleName, @NotNull String rootPackage,
                                    @NotNull String apiKey, @NotNull String appId) {
    super(project, moduleName, rootPackage);
    myApiKey = apiKey;
    myAppId = appId;
  }

  /** Generate a Maven App Engine module as a backend for the Cloud Backend Sample */
  @Override
  public void generate(@Nullable final Callback callback) {
    try {
      createAppEngineDirStructure();
    } catch (IOException e) {
      logAndCallbackFailure("IOException when trying to create App Engine module directory structure", e, callback);
      return;
    }

    try {
      addAppEngineSampleCode(myRootPackage, myAppId, myApiKey);
    }
    catch (IOException e) {
      logAndCallbackFailure("IOException when trying to add App Engine sample code", e, callback);
      return;
    }

    try {
      addMavenFunctionality();
      waitForMavenImport();
    }
    catch (IOException e) {
      logAndCallbackFailure("IOException when trying to add Maven functionality to App Engine module", e, callback);
      return;
    }
    catch (ConfigurationException e) {
      logAndCallbackFailure("Configuration exception when trying import App Engine Maven project", e, callback);
      return;
    }

    try {
      addGradleRequirements();
    }
    catch (IOException e) {
      logAndCallbackFailure("Failed to add build.gradle file", e, callback);
      return;
    }

    // We need to invoke later to ensure the maven module is imported fully and searchable
    // TODO : we are waiting earlier for the maven Import, it is worth taking a look at the necessity of this invokeLater
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        myModule = ModuleManager.getInstance(myProject).findModuleByName(myModuleName);

        // TODO : It is also worth taking a look at this
        // Sometimes the AppEngine plugin isn't picked up by the module import even if the main maven information is
        // Force an update so our AppEngineMavenProject functionality works properly.
        MavenProjectsManager.getInstance(myProject).forceUpdateProjects(
          Arrays.asList(MavenProjectsManager.getInstance(myProject).findProject(myModule)));


        final AppEngineMavenProject mavenProject = AppEngineMavenProject.get(myModule);
        if(mavenProject != null) {
          mavenProject.runGenClientLibraries(new AppEngineMavenProject.MavenBuildCallback() {
            @Override
            public void onBuildCompleted(int resultCode, String text) {
              if(resultCode == 0) {
                try {
                  for (String apiName : SAMPLE_API_NAMES) {
                    mavenProject.expandClientLibrary(apiName);
                  }
                }
                catch (IOException e) {
                  logAndCallbackFailure("IOException when expanding client library source folders", e, callback);
                  return;
                }
                if(callback != null) {
                  callback.moduleCreated(myModule);
                  return;
                }
              }
              if (callback != null) {
                logAndCallbackFailure("Maven build failed : (result code " + resultCode + ") " + text, null, callback);
                return;
              }
            }
          });
        }
        else {
          if (callback != null) {
            logAndCallbackFailure("App Engine module failed to import maven functionality", null, callback);
            return;
          }
        }
      }
    });
    // careful not add any code here, the code above makes some assumptions about code flow that means no non-conditional code is
    // executed
  }

  /** Pull in appEngineSampleCode from somewhere, right now it's the in-plugin templating, but maybe this whole things changes */
  protected void addAppEngineSampleCode(final String rootPackage, final String appId, final String apiKey) throws IOException {

    ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<Void, IOException>() {
      @Override
      public Void compute() throws IOException {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        PsiManager psiManager = PsiManager.getInstance(myProject);
        PsiDirectory moduleRootDir = psiManager.findDirectory(myModuleRootDir);
        PsiDirectory javaSrcDirectory = psiManager.findDirectory(myModuleRootDir.findFileByRelativePath("src/main/java"));
        PsiDirectory resourcesDirectory = psiManager.findDirectory(myModuleRootDir.findFileByRelativePath("src/main/resources"));
        PsiDirectory webappDirectory = psiManager.findDirectory(myModuleRootDir.findFileByRelativePath("src/main/webapp"));

        // Create package directory
        PsiDirectory curPackageDir = javaSrcDirectory;
        for (String packageComponent : rootPackage.split("\\.")) {
          curPackageDir = curPackageDir.createSubdirectory(packageComponent);
        }

        // Add entity manager
        curPackageDir.add(TemplateHelper.loadJpaEntityManagerFactoryClass(myProject, rootPackage));

        TemplateHelper.EndpointPackageInfo endpointPackageInfo = TemplateHelper.getEndpointPackageInfo(rootPackage);

        // Add entities
        for (String entityName : SAMPLE_ENTITY_NAMES) {
          curPackageDir
            .add(TemplateHelper.generateJavaTemplateContentWithOwnerDomain(myProject, entityName, rootPackage, endpointPackageInfo));
        }

        // Add endpoints
        for (String endpointName : SAMPLE_ENDPOINTS_NAMES) {
          PsiElement addedEndpoint = curPackageDir.add(TemplateHelper
                                                         .generateJavaSampleTemplateWithOwnerDomainAndApiKey(myProject, endpointName,
                                                                                                             rootPackage,
                                                                                                             endpointPackageInfo, apiKey));
          PsiDocumentManager docManager = PsiDocumentManager.getInstance(myProject);
          docManager.doPostponedOperationsAndUnblockDocument(docManager.getDocument((PsiFile) addedEndpoint));
        }

        // Add/replace persistence.xml
        final PsiDirectory metaInfDir = resourcesDirectory.findSubdirectory(GctConstants.APP_ENGINE_META_INF_DIR);
        PsiUtils.addOrReplaceFile(metaInfDir, TemplateHelper.loadPersistenceXml(myProject));

        // Add static content

        // css
        PsiDirectory cssDir = webappDirectory.createSubdirectory(GctConstants.APP_ENGINE_CSS_DIR);
        cssDir.add(TemplateHelper.generateStaticContent(myProject, "bootstrap.min.css"));

        // js
        PsiDirectory jsDir = webappDirectory.createSubdirectory(GctConstants.APP_ENGINE_JS_DIR);
        jsDir.add(TemplateHelper.generateStaticContent(myProject, "bootstrap.min.js"));
        jsDir.add(TemplateHelper.generateStaticContent(myProject, "jquery-1.9.0.min.js"));

        // images (to support twitter bootstrap)
        VirtualFile moduleImgDir = webappDirectory.getVirtualFile().createChildDirectory(null, GctConstants.APP_ENGINE_IMG_DIR);
        File img = new File(moduleImgDir.getPath() + "/glyphicons-halflings.png");
        Files.write(Resources.toByteArray(TemplateHelper.class.getResource("glyphicons-halflings.png")), img);

        img = new File(moduleImgDir.getPath() + "/glyphicons-halflings-white.png");
        Files.write(Resources.toByteArray(TemplateHelper.class.getResource("glyphicons-halflings-white.png")), img);
        moduleImgDir.refresh(false, true);

        // xml
        PsiDirectory webInfDirectory = webappDirectory.createSubdirectory(GctConstants.APP_ENGINE_WEB_INF_DIR);
        webInfDirectory.add(TemplateHelper.loadWebXml(myProject));
        webInfDirectory.add(TemplateHelper.generateAppEngineWebXml(myProject, appId));

        // html
        webappDirectory.add(TemplateHelper.generateStaticContent(myProject, "index.html"));
        return null; // to Void
      }
    });
  }

  /** Add a build.gradle so the IDE doesn't complain like a nut, this may not be necessary
   * must be run on the event dispatch thread */
  private void addGradleRequirements() throws IOException {
    PsiManager psiManager = PsiManager.getInstance(myProject);
    final PsiDirectory moduleDirectory = psiManager.findDirectory(myModuleRootDir);
    if(moduleDirectory == null) {
      // not sure this is a reachable error because the maven project get root directory has got to exist
      throw new FileNotFoundException("Could not find App Engine maven project root directory");
    }
    final PsiFile gradlebuildFile = TemplateHelper.loadGradleBuildFile(myProject);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        moduleDirectory.add(gradlebuildFile);
      }
    });
  }

  private static void logAndCallbackFailure(String message, @Nullable Throwable t, @Nullable Callback callback) {
    if(t == null) {
      LOG.error(message);
    }
    else {
      LOG.error(message, t);
    }
    if (callback != null) {
      callback.onFailure(message);
    }
  }
}
