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
package com.google.gct.intellij.endpoints.project;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.util.VfsUtils;
import com.google.gct.intellij.endpoints.util.ResourceUtils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import com.intellij.util.io.ZipUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is a wrapper for App Engine functionality on a MavenProject, perhaps we should just extend MavenProject
 */
public class AppEngineMavenProject {
  private final MavenProject myMavenProject;
  private final Project myProject;
  private final Module myModule;

  public static final String GOAL_COMPILE = "compile";
  public static final String GOAL_APP_ENGINE_GEN_CLIENT_LIBRARIES = "appengine:endpoints_get_client_lib";
  public static final String GOAL_APP_ENGINE_GET_DISCOVERY_DOC = "appengine:endpoints_get_discovery_doc";

  public static final String PLUGIN_GROUP_APP_ENGINE = "com.google.appengine";
  public static final String PLUGIN_ARTIFACT_APP_ENGINE = "appengine-maven-plugin";

  private static final Logger LOG = Logger.getInstance(AppEngineMavenProject.class);

  /* Consider using get() or checking isAppEngineMavenProject before using constructor */
  public AppEngineMavenProject(@NotNull Project project, @NotNull Module appEngineModule, @NotNull MavenProject mavenProject) {
    myProject = project;
    myModule = appEngineModule;
    myMavenProject = mavenProject;
  }

  /**
   *  Create an instance of AppEngineMavenProject for a given module
   * @param appEngineModule
   *    A valid appEngineModule
   * @return
   *    An instance of AppEngineMavenProject or <code>null</code> if none can be found
   */
  @Nullable
  public static AppEngineMavenProject get(@NotNull Module appEngineModule) {
    MavenProject mavenProject = MavenProjectsManager.getInstance(appEngineModule.getProject()).findProject(appEngineModule);
    if (isAppEngineMavenProject(mavenProject)) {
      return new AppEngineMavenProject(appEngineModule.getProject(), appEngineModule, mavenProject);
    }
    return null;
  }

  /** Checks if a maven project can be wrapped with the AppEngineMaven wrapper and be useful */
  public static boolean isAppEngineMavenProject(MavenProject mavenProject) {
    if (mavenProject == null) {
      return false;
    }
    if (mavenProject.findPlugin(PLUGIN_GROUP_APP_ENGINE, PLUGIN_ARTIFACT_APP_ENGINE) == null) {
      return false;
    }
    return true;
  }

  @NotNull
  public MavenProject getMavenProject() {
    return myMavenProject;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  /** runs a compile goal on a project */
  public void runCompile(@Nullable final MavenBuildCallback callback) {
    runMavenBuilder(Arrays.asList(GOAL_COMPILE), callback);
  }

  /** runs a compile goal AND gen discovery doc goal, always makes a network call */
  public void runGetDiscoveryDoc(@Nullable final MavenBuildCallback callback) {
    runMavenBuilder(Arrays.asList(GOAL_COMPILE, GOAL_APP_ENGINE_GET_DISCOVERY_DOC), callback);
  }

  /** runs a compile goal AND gen discovery doc goal AND a gen client libraries, always makes a network call */
  public void runGenClientLibraries(@Nullable final MavenBuildCallback callback) {
    runMavenBuilder(Arrays.asList(GOAL_COMPILE, GOAL_APP_ENGINE_GET_DISCOVERY_DOC, GOAL_APP_ENGINE_GEN_CLIENT_LIBRARIES), callback);
  }

  /**
   * Expand out a generated client library into the App Engine module,
   * This needs to be run on the dispatch thread
   */
  @NotNull
  public File expandClientLibrary(@NotNull String apiName) throws IOException {

    File targetGenSourcesDir = new File(myMavenProject.getBuildDirectory(), GctConstants.API_CLIENT_GENERATED_SOURCES);
    if (!targetGenSourcesDir.exists()) {
      throw new FileNotFoundException("No sources were generated");
    }

    // There are sometimes case differences between the API name and the zip file
    Pattern pattern = Pattern.compile(apiName + "-.*\\.zip", Pattern.CASE_INSENSITIVE);
    List<File> apiFiles = FileUtil.findFilesByMask(pattern, targetGenSourcesDir);

    // Should only be 1
    if (apiFiles.isEmpty()) {
      throw new FileNotFoundException("No sources found for api : " + apiName);
    }

    File apiZipFile = apiFiles.get(0);

    File genDir = null;

    genDir = new File(VfsUtil.virtualToIoFile(myMavenProject.getDirectoryFile()), GctConstants.APP_ENGINE_GENERATED_LIB_DIR);
    ResourceUtils.createResource(genDir);

    // We expand the generated sources in "google_generated" in the main project, others can pick it up from there
    ZipFile zip = null;
    try {
      zip = new ZipFile(apiZipFile);
      Enumeration<? extends ZipEntry> entries = zip.entries();
      String baseDir = null;
      String srcJarPath = null;
      if (entries.hasMoreElements()) {
        baseDir = entries.nextElement().getName();
        // get the name of the top-evel directory at the root of the zip (ex: deviceinfoendpoint)
        char lastChar = baseDir.charAt(baseDir.length() - 1);
        if(lastChar == '/' || lastChar == File.separatorChar) {
          baseDir = baseDir.substring(0, baseDir.length() - 1);
        }
      }

      while (entries.hasMoreElements()) {
        ZipEntry curEntry = entries.nextElement();
        if (curEntry.isDirectory()) {
          continue;
        }

        String[] curEntrySegments = curEntry.getName().split("/");
        if (curEntrySegments.length != 2) {
          continue;
        }

        if (curEntrySegments[1].contains(baseDir) && curEntrySegments[1].endsWith(GctConstants.API_CLIENT_SOURCES_JAR_SUFFIX)) {
          //  We then look for a an entry directly underneath the baseDir that has the suffix -sources.jar, such as:
          // com.foo-deviceinfoendpoint-v1-datestamp-java-1.1x.rc-sources.jar
          // This is the jar file that contains the sources for the client stubs, we can't include the sources directly because we
          // need to compile them. So we need to expand, copy, and compile this in the Android-endpoints module.
          srcJarPath = curEntry.getName();
          break;
        }
      }

      if (baseDir != null) {
        File baseDirFile = new File(genDir, baseDir);
        ResourceUtils.deleteResource(baseDirFile);
      }

      ZipUtil.extract(apiZipFile, genDir, null);

      File baseDirFileRef = new File(genDir, baseDir);
      File libExpandedSourceDirFileRef = new File(baseDirFileRef, GctConstants.API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME);
      libExpandedSourceDirFileRef.mkdir();
      ZipUtil.extract(new File(genDir, srcJarPath), libExpandedSourceDirFileRef, null);
      return libExpandedSourceDirFileRef;
    }
    finally {
      if(zip != null) {
        zip.close();
      }
    }
  }

  /**
   * Expand out all client library sources into the App Engine module. Should probably run a compile and generate client libraries before
   * calling this to ensure something gets copied
   *
   */
  @NotNull
  public List<File> expandAllClientLibs() throws IOException {
    Map<PsiJavaFile, String> apiMap = findAllEndpointServiceClasses();
    List<File> expandedDirReferences = new ArrayList<File>(apiMap.size());
    for(String apiName : apiMap.values()) {
      expandedDirReferences.add(expandClientLibrary(apiName));
    }
    return expandedDirReferences;
  }

  /**
   * NOTE : Requires a readAction if not coming from the dispatch thread
   * Find all the classes that are endpoint files (annotated with @Api from javax.persistence)
   * @return a map of endpoint files and corresponding api names
   */
  public Map<PsiJavaFile, String> findAllEndpointServiceClasses() throws FileNotFoundException {

    Map<PsiJavaFile, String> endpointFileToApiName = new HashMap<PsiJavaFile, String>();

    PsiClass apiAnnotationClass = JavaPsiFacade.getInstance(myProject).
      findClass(GctConstants.APP_ENGINE_ANNOTATION_API, new ModuleWithDependenciesScope(myModule,
                                                                    ModuleWithDependenciesScope.COMPILE |
                                                                    ModuleWithDependenciesScope.LIBRARIES));
    if (apiAnnotationClass == null) {
      LOG.info("Unable to load annotation class: " + GctConstants.APP_ENGINE_ANNOTATION_API);
      throw new FileNotFoundException("Could not find the JPA api annotation class, perhaps your dependencies are broken");
    }

    Query<PsiClass> psiClassQuery = AnnotatedElementsSearch
      .searchPsiClasses(apiAnnotationClass, new ModuleWithDependenciesScope(myModule, ModuleWithDependenciesScope.CONTENT));
    Collection<PsiClass> endpointServiceClasses = psiClassQuery.findAll();

    for (PsiClass serviceClass : endpointServiceClasses) {
      // TODO: here is some code to ignore inner classes, not sure if this is allowed or not for entities
      //if (clazz.getContainingClass() == null) {
      //  continue;
      //}
      PsiAnnotation apiAnnotation = AnnotationUtil.findAnnotation(serviceClass, true, GctConstants.APP_ENGINE_ANNOTATION_API);
      PsiAnnotationMemberValue nameValue = apiAnnotation.findAttributeValue("name");

      String apiName = null;
      if (nameValue != null && nameValue instanceof PsiLiteral) {
        PsiLiteral literal = (PsiLiteral)nameValue;
        if (literal.getValue() instanceof String) {
          apiName = (String)literal.getValue();
        }
      }

      endpointFileToApiName.put((PsiJavaFile)serviceClass.getContainingFile(), apiName);
    }

    return endpointFileToApiName;
  }

  /** Get a reference to the generated sources dir in the App Engine module */
  @Nullable
  public VirtualFile getExpandedSourceDir() {
    return VfsUtils.findFileUnderContentRoots(myModule, GctConstants.APP_ENGINE_GENERATED_LIB_DIR);
  }

  /** Get a IO file reference to the generated sources dir in the App Engine module */
  public File getExpandedSourceDirFile() throws FileNotFoundException {
    VirtualFile dir = getExpandedSourceDir();
    if(dir == null) {
      throw new FileNotFoundException("Could not find generated sources directory : "
                                      + GctConstants.APP_ENGINE_GENERATED_LIB_DIR
                                      + " in " + getRootDirectory());
    }
    return VfsUtilCore.virtualToIoFile(dir);
  }

  /** Refresh the expanded source dir, useful after calling {@link #expandAllClientLibs()}
   *
   * @param async
   *    calling this synchronously requires a read action
   * @param onFinished
   *    get notified when calling asynchronously
   * @throws FileNotFoundException
   *    if the generated source directory is missing
   */
  public void refreshExpandedSourceDir(Boolean async, Runnable onFinished) throws FileNotFoundException {
    LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(getExpandedSourceDirFile()), async, true, onFinished);
  }

  /** Get the maven project root directory */
  public VirtualFile getRootDirectory() {
    return myMavenProject.getDirectoryFile();
  }

  /** Runs a write action and needs to be on dispatch thread (use with invokeLater if not) */
  public void copyExpandedSources(final File destinationRootDir) {

    VirtualFile genLibVFile = VfsUtils.findFileUnderContentRoots(myModule, GctConstants.APP_ENGINE_GENERATED_LIB_DIR);

    PsiDirectory appEngineGenLibDir = PsiManager.getInstance(myProject).findDirectory(genLibVFile);

    assert (appEngineGenLibDir != null && appEngineGenLibDir.getVirtualFile().exists());

    final Map<String, PsiDirectory> apiNameToSourceRoot = new HashMap<String, PsiDirectory>();
    final PsiDirectory[] libsDirHolder = new PsiDirectory[]{null};
    final PsiFile[] proguardFileHolder = new PsiFile[]{null};

    for (PsiElement curElem : appEngineGenLibDir.getChildren()) {
      if (!(curElem instanceof PsiDirectory)) {
        continue;
      }

      PsiDirectory apiDir = (PsiDirectory)curElem;
      String apiName = apiDir.getName();

      PsiDirectory expandedSourceDir = apiDir.findSubdirectory(GctConstants.API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME);

      if (expandedSourceDir == null) {
        continue;
      }

      apiNameToSourceRoot.put(apiName, expandedSourceDir);

      // While we're here, look for a folder with the dependent libs
      if (libsDirHolder[0] == null) {
        PsiDirectory subDir = apiDir.findSubdirectory(GctConstants.API_CLIENT_LIBS_SUBFOLDER_NAME);
        if (subDir != null) {
          libsDirHolder[0] = subDir;
        }
      }
      else {
        // We already have a directory that points to the list of dependent libs; the contents
        // of any other libs directory for Cloud Endpoints should be identical
      }

      // While we're also here, look for the proguard file for the client library
      if (proguardFileHolder[0] == null) {
        proguardFileHolder[0] = apiDir.findFile(GctConstants.API_CLIENT_PROGUARD_FILENAME);
      }
      else {
        // We already have the proguard file; it does not differ for each endpoint, as it's for the Google API Client Library,
        // and this library does not vary from endpoint API to endpoint API.
      }
    }

    if (apiNameToSourceRoot.isEmpty()) {
      // nothing to do
      return;
    }

    if (proguardFileHolder[0] == null) {
      // Something's wrong here..
      return;
    }

    // Collect the dependencies
    if (libsDirHolder[0] == null) {
      // no available dependencies
      // TODO: make sure we account for this when changing how we deal with java client lib dependencies
      return;
    }

    // Now that we have all of the necessary information, we can go ahead and mutate the Android module
    // First, copy all of the generated source over to the android project, along with the dependencies
    // and add the appropriate source roots
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {

        File endpointSrcDir = new File(destinationRootDir, GctConstants.ANDROID_ENDPOINT_SRC_PATH);
        if (!endpointSrcDir.exists()) {
          endpointSrcDir.mkdirs();
        }

        // Remove any existing source
        for (File child : endpointSrcDir.listFiles()) {
          FileUtil.delete(child);
        }

        // Copy the new source over
        for (Map.Entry<String, PsiDirectory> entry : apiNameToSourceRoot.entrySet()) {
          try {
            FileUtil.copyDirContent(VfsUtil.virtualToIoFile(entry.getValue().getVirtualFile()), endpointSrcDir);
          }
          catch (IOException e) {
            LOG.error(e);
          }
        }
      }
    });
  }

  /** run the maven builder on a list of goals */
  public void runMavenBuilder(List<String> goalsToRun, @Nullable final MavenBuildCallback callback) {

    assert (myMavenProject != null);

    final MavenRunnerParameters params = new MavenRunnerParameters(true, myMavenProject.getDirectory(), goalsToRun,
                                                                   MavenProjectsManager.getInstance(myProject).getExplicitProfiles());

    MavenRunConfigurationType.runConfiguration(myProject, params, new ProgramRunner.Callback() {
      @Override
      public void processStarted(RunContentDescriptor descriptor) {
        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler != null) {
          processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
              if (callback != null) {
                callback.onBuildCompleted(event.getExitCode(), event.getText());
              }
            }
          });
        }
      }
    });
  }



  /**
   * A callback when a mavenbuild is complete. This callback will not return on the Dispatch thread, if you need code to run on
   * dispatch thread then use ApplicationManager.getApplication().invokeLater...
   */
  public static interface MavenBuildCallback {
    void onBuildCompleted(int resultCode, String text);
  }
}
