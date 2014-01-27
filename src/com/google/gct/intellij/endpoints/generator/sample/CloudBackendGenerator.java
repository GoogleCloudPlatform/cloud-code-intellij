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

import com.android.tools.idea.model.ManifestInfo;
import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.generator.AppEngineMavenGenerator;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;
import com.google.gct.intellij.endpoints.action.ui.GenerateBackendDialog;
import com.google.gct.intellij.endpoints.util.VfsUtils;
import com.google.gct.intellij.endpoints.util.GradleUtils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Create a backend for an android project (AppEngine backend + Android library module as client)
 */
public class CloudBackendGenerator {

  private final Project myProject;
  private final Module myAndroidModule;
  private final GenerateBackendDialog myGenerateBackendDialog;
  private AppEngineMavenProject myAppEngineMavenProject;

  /** set in AppEngineMavenGeneratorCallback */
  private Module myAppEngineModule;

  /** set in AndroidGradleGcmLibGeneratorCallback */
  private Module myAndroidGcmLibModule;

  private static final Logger LOG = Logger.getInstance(CloudBackendGenerator.class);

  /**
   * @param project
   *     The IDEA/Studio project
   * @param androidModule
   *     A main android module
   * @param generateBackendDialog
   *     The dialog used to extract api key, project num and app Id
   */
  public CloudBackendGenerator(@NotNull Project project, @NotNull Module androidModule,
                               @NotNull GenerateBackendDialog generateBackendDialog) {
    myProject = project;
    myAndroidModule = androidModule;
    myGenerateBackendDialog = generateBackendDialog;
  }

  /**
   * Will generate a full cloud backend with an App Engine part (backend) and an Android library module (frontend) and add
   * in the required gradle hooks to build
   *
   * This is expected to be initiated on an event dispatch thread and will sequentially create the sample using a
   * series of callback
   * {@link AppEngineMavenGeneratorCallback}
   * {@link MavenBuilderCallback}
   * {@link RefreshCallback}
   * {@link AndroidGradleGcmLibGeneratorCallback}
   *
   * @param callback
   *    Callback when the generation is finished/failed
   */
  public void generate(@Nullable final Callback callback) {
    new AppEngineMavenGcmGenerator(myProject, getAppEngineModuleName(), getRootPackage(), myGenerateBackendDialog.getApiKey(),
                                            myGenerateBackendDialog.getAppId()).generate(new AppEngineMavenGeneratorCallback(callback));
  }


  private String getAppEngineModuleName() {
    return myAndroidModule.getName() + GctConstants.APP_ENGINE_MODULE_SUFFIX;
  }

  @Nullable
  private String getRootPackage() {
    ManifestInfo manifestInfo = ManifestInfo.get(myAndroidModule);
    String rootPackage = manifestInfo.getPackage();
    return (rootPackage.trim().isEmpty()) ? null : rootPackage;
  }

  private class AppEngineMavenGeneratorCallback implements AppEngineMavenGenerator.Callback {
    private final Callback myRootCallback;
    public AppEngineMavenGeneratorCallback(@Nullable Callback callback) {
      myRootCallback = callback;
    }
    @Override
    public void moduleCreated(final Module appEngineModule) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          myAppEngineModule = appEngineModule;
          myAppEngineMavenProject = AppEngineMavenProject.get(appEngineModule);
          if (myAppEngineMavenProject != null) {
            myAppEngineMavenProject.runGenClientLibraries(new MavenBuilderCallback(myRootCallback));
          }
        }
      });
    }
    @Override
    public void onFailure(String errorMessage) {
      logAndCallbackFailure("Failed when creating the App Engine Maven module because : " + errorMessage, null, myRootCallback);
    }
  }

  private class MavenBuilderCallback implements AppEngineMavenProject.MavenBuildCallback {
    private final Callback myRootCallback;
    public MavenBuilderCallback(@Nullable Callback callback) {
      myRootCallback = callback;
    }
    @Override
    public void onBuildCompleted(int resultCode, String text) {
      if (resultCode == 0) {
        try {
          myAppEngineMavenProject.refreshExpandedSourceDir(true, new RefreshCallback(myRootCallback));
        } catch (FileNotFoundException e) {
          logAndCallbackFailure("Failed to refresh " + GctConstants.APP_ENGINE_GENERATED_LIB_DIR + " directory", e, myRootCallback);
          return;
        }
      }
      else {
        logAndCallbackFailure("Maven build failed with code '" + resultCode + "' and message : " + text, null, myRootCallback);
        return;
      }
    }
  }

  private class RefreshCallback implements Runnable {
    private final Callback myRootCallback;
    public RefreshCallback(@Nullable Callback callback) {
      myRootCallback = callback;
    }
    @Override
    public void run() {
      new AndroidGradleGcmLibGenerator(myProject, myAndroidModule, myAppEngineModule, getRootPackage(),
                                       myGenerateBackendDialog.getProjectNumber())
        .generate(new AndroidGradleGcmLibGeneratorCallback(myRootCallback));
    }
  }

  private class AndroidGradleGcmLibGeneratorCallback implements AndroidGradleGcmLibGenerator.Callback {
    private final Callback myRootCallback;
    public AndroidGradleGcmLibGeneratorCallback(@Nullable Callback callback) {
      myRootCallback = callback;
    }

    @Override
    public void moduleCreated(Module androidLibModule) {
      try {
        myAndroidGcmLibModule = androidLibModule;
        addGradleHooks();
      } catch(IOException e) {
      // this is a non-catastrophic failure, perhaps we shouldn't fail here?
        logAndCallbackFailure("Failed to add Gcm module dependency to Android module, you may be able to do this yourself",
                              e, myRootCallback);
        return;
      }
      if(myRootCallback != null) {
        myRootCallback.backendCreated(androidLibModule, myAppEngineModule);
      }
    }

    @Override
    public void onFailure(String errorMessage) {
      logAndCallbackFailure("Failed during generation of Android library module : ", null, myRootCallback);
    }
  }

  private void addGradleHooks() throws IOException {

    VirtualFile buildGradleVFile = VfsUtils.findFileUnderContentRoots(myAndroidModule, "build.gradle");
    if(buildGradleVFile == null) {
      throw new FileNotFoundException("Could not find build.gradle file");
    }
    final GroovyFile buildGradlePsiFile = (GroovyFile) PsiManager.getInstance(myProject).findFile(buildGradleVFile);
    if(buildGradlePsiFile == null) {
      throw new FileNotFoundException("Could not find build.gradle file");
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        // update the main android module's build.gradle with mavenCentral() because something is off with dependency management
        // and add in the newly created android library module as a dependency
        GradleUtils.addRepository(myProject, buildGradlePsiFile, GradleUtils.REPO_MAVEN_CENTRAL);
        GradleUtils.addDependency(myProject, buildGradlePsiFile, String
          .format(GradleUtils.DEPENDENCY_COMPILE, myAndroidGcmLibModule.getName()));
      }
    });
  }

  private static void logAndCallbackFailure(@NotNull String message, @Nullable Throwable t, @Nullable Callback callback) {
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

  /** callback to when the project is done creating */
  public interface Callback {
    /**
     * No guarantees on the type of thread this will return on
     * @param androidLibModule
     *    The newly created Android Library module
     * @param appEngineModule
     *    The newly created App Engine module
     */
    void backendCreated(@NotNull Module androidLibModule, @NotNull Module appEngineModule);

    /**
     * Called on generation failure
     * No guarantees on the type of thread this will return on
     */
    void onFailure(String errorMessage);
  }
}
