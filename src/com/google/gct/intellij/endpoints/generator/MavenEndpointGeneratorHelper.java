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

import com.google.gct.intellij.endpoints.EndpointsConstants;
import com.google.gct.intellij.endpoints.templates.TemplateHelper;
import com.google.gct.intellij.endpoints.util.FacetUtils;
import com.google.gct.intellij.endpoints.util.MavenUtils;
import com.google.gct.intellij.endpoints.util.PsiUtils;
import com.google.gct.intellij.endpoints.util.ResourceUtils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.io.ZipUtil;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Functionality related to generating endpoints in AppEngine modules and copying them to Android modules
 */
public class MavenEndpointGeneratorHelper {
  private static final String API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME = "lib-expanded-source";
  private static final String API_CLIENT_LIBS_SUBFOLDER_NAME = "libs";
  private static final String API_CLIENT_PROGUARD_FILENAME = "proguard-google-api-client.txt";

  private static final String SOURCES_JAR_SUFFIX = "-sources.jar";

  private static final String ANNOTATION_API_FQN = "com.google.api.server.spi.config.Api";
  private static final String META_INF_PATH = "src/main/resources/META-INF";
  private static final Logger LOG = Logger.getInstance(MavenEndpointGeneratorHelper.class);

  private static final String MAVEN_COMPILE = "compile";
  private static final String MAVEN_APP_ENGINE_GET_DISC_DOC = "appengine:endpoints_get_discovery_doc";
  private static final String MAVEN_APP_ENGINE_GET_CLIENT_LIB = "appengine:endpoints_get_client_lib";

  /** callback interface used to indicated async generation is complete */
  public interface LibGenerationCallback {
    void onGenerationComplete(boolean generationSuccessful);
  }

  /**
   * NOTE: We're invoking on the dispatch thread for now.
   * Unzip the sources directory of generated endpoints
   * @param mavenProject
   * @param apiName
   * @return reference to the directory after expansion
   */
  @Nullable
  public static File expandSourceDirForApi(MavenProject mavenProject, String apiName) {

    if (mavenProject == null) {
      return null;
    }

    File targetGenSourcesDir = new File(mavenProject.getBuildDirectory(), "generated-sources");
    if (!targetGenSourcesDir.exists()) {
      return null;
    }

    // There are sometimes case differences between the API name and the zip file
    Pattern pattern = Pattern.compile(apiName + "-.*\\.zip", Pattern.CASE_INSENSITIVE);
    List<File> apiFiles = FileUtil.findFilesByMask(pattern, targetGenSourcesDir);

    // Should only be 1
    if (apiFiles.isEmpty()) {
      return null;
    }

    File apiZipFile = apiFiles.get(0);

    File genDir = null;

    try {
      genDir = new File(VfsUtil.virtualToIoFile(mavenProject.getDirectoryFile()), EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);
      ResourceUtils.createResource(genDir);

      // TODO : perhaps we don't need this, the eclipse plugin only really uses the libs and drops them in android,
      // they aren't persisted like this but it cleans out the specific directory that is being regenerated
      ZipFile zip = new ZipFile(apiZipFile);
      Enumeration<? extends ZipEntry> entries = zip.entries();
      String baseDir = null;
      String srcJarPath = null;
      if (entries.hasMoreElements()) {
        baseDir = entries.nextElement().getName();
        char lastChar = baseDir.charAt(baseDir.length() - 1);
        if (lastChar == '/' || lastChar == File.separatorChar) {
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

        if (curEntrySegments[1].contains(baseDir) && curEntrySegments[1].endsWith(SOURCES_JAR_SUFFIX)) {
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
      File libExpandedSourceDirFileRef = new File(baseDirFileRef, API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME);
      libExpandedSourceDirFileRef.mkdir();
      ZipUtil.extract(new File(genDir, srcJarPath), libExpandedSourceDirFileRef, null);
      return libExpandedSourceDirFileRef;
    }
    catch (IOException e) {
      LOG.error(e);
    }

    return null;
  }

  /**
   * Copy endpoints source to an android module
   * @param androidGcmLibModuleRoot
   * @param apiNameToSourceRoot
   */
  private static void copySource(File androidGcmLibModuleRoot, Map<String, PsiDirectory> apiNameToSourceRoot) {
    File endpointSrcDir = GradleGcmGeneratorHelper.getEndpointSrcDir(androidGcmLibModuleRoot);
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

  private final Module myAppEngineModule;
  private final Project myProject;

  /** create a helper for a specific module */
  public MavenEndpointGeneratorHelper(Project project, Module appEngineModule) {
    this.myProject = project;
    this.myAppEngineModule = appEngineModule;
  }

  /**
   * NOTE : Cannot hold a readAction if invoking this method
   * Generates an endpoint class from an entity class
   * @param entityClass
   * @param idField the specific field from the class that is the JPA @Id
   */
  public void generateEndpoint(PsiClass entityClass, PsiField idField) {
    TemplateHelper templateHelper = new TemplateHelper(entityClass, idField);
    final List<PsiFile> serviceClassFiles = new ArrayList<PsiFile>();
    final PsiFile entityManagerFactoryFile;
    final PsiFile persistenceXmlFile;

    try {
      serviceClassFiles.add(templateHelper.loadJpaSwarmServiceClass());
      entityManagerFactoryFile = templateHelper.loadJpaEntityManagerFactoryClass();
      persistenceXmlFile = templateHelper.loadPersistenceXml();
    }
    catch (IOException e) {
      LOG.error("Failed to generate files from templates during endpoint generation", e);
      return;
    }

    final PsiDirectory entityDir = entityClass.getContainingFile().getContainingDirectory();
    final PsiDirectory metaInfDir =
      PsiManager.getInstance(myProject).findDirectory(myAppEngineModule.getModuleFile().getParent().findFileByRelativePath(META_INF_PATH));

    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        final List<PsiJavaFile> endpointServiceFiles = new ArrayList<PsiJavaFile>();
        for (PsiFile serviceClassFile : serviceClassFiles) {
          endpointServiceFiles.add((PsiJavaFile)PsiUtils.addOrReplaceFile(entityDir, serviceClassFile));
        }
        PsiUtils.addOrReplaceFile(entityDir, entityManagerFactoryFile);
        PsiUtils.addOrReplaceFile(metaInfDir, persistenceXmlFile);
      }
    });

    // Kick off the maven build
    List<String> goalsToRun = new ArrayList<String>();
    goalsToRun.add(MAVEN_COMPILE);
    goalsToRun.add(MAVEN_APP_ENGINE_GET_DISC_DOC);
    MavenUtils.runMavenBuilder(myProject, myProject.getBaseDir().findChild(myAppEngineModule.getName()), goalsToRun, null);

  }

  /**
   * Generate client libraries from AppEngine module
   * @param callback
   */
  public void regenerateAllClientLibraries(final LibGenerationCallback callback) {
    // Kick off the maven build
    List<String> goalsToRun = new ArrayList<String>();
    goalsToRun.add(MAVEN_COMPILE);
    goalsToRun.add(MAVEN_APP_ENGINE_GET_CLIENT_LIB);
    final VirtualFile appEngineModuleRootDir = myProject.getBaseDir().findChild(myAppEngineModule.getName());
    final MavenProject mavenProject = MavenUtils.getMavenProjectForModule(myProject, myAppEngineModule.getName());
    if (mavenProject == null) {
      // TODO: log an error and return, or show an error to the user
    }

    MavenUtils.runMavenBuilder(myProject, appEngineModuleRootDir, goalsToRun, new MavenUtils.MavenBuildCallback() {
      @Override
      public void onBuildCompleted(int resultCode) {
        if (resultCode == 0) {
          // TODO : Happening on the dispatch thread, but we can move it off of there.
          Map<PsiJavaFile, String> endpointClassesToApiName = findAllEndpointServiceClasses();
          for (Map.Entry<PsiJavaFile, String> entry : endpointClassesToApiName.entrySet()) {
            if (entry.getValue() != null && entry.getValue().trim().length() > 0) {
              expandSourceDirForApi(mavenProject, entry.getValue().trim());
            }
          }

          // Now, perform a refresh so that the generated endpoint libs folder (google_generated) shows up
          File generatedLibsDir = new File(VfsUtil.virtualToIoFile(appEngineModuleRootDir),
                                           EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);
          LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(generatedLibsDir), true, true, null);
        }
        callback.onGenerationComplete(resultCode == 0);
      }
    });
  }

  /**
   * NOTE : Requires a readAction if not coming from the dispatch thread
   * Find all the classes that are endpoint files (annotated with @Api from javax.persistence)
   * @return a map of endpoint files and corresponding api names
   */
  public Map<PsiJavaFile, String> findAllEndpointServiceClasses() {

    Map<PsiJavaFile, String> endpointFileToApiName = new HashMap<PsiJavaFile, String>();

    PsiClass apiAnnotationClass = JavaPsiFacade.getInstance(myProject).
        findClass(ANNOTATION_API_FQN, new ModuleWithDependenciesScope(myAppEngineModule,
                                                                      ModuleWithDependenciesScope.COMPILE |
                                                                      ModuleWithDependenciesScope.LIBRARIES));
    if (apiAnnotationClass == null) {
      // TODO would it better to show a message here?
      LOG.info("Unable to load annotation class: " + ANNOTATION_API_FQN);
      return Collections.emptyMap();
    }

    Query<PsiClass> psiClassQuery = AnnotatedElementsSearch
      .searchPsiClasses(apiAnnotationClass, new ModuleWithDependenciesScope(myAppEngineModule, ModuleWithDependenciesScope.CONTENT));
    Collection<PsiClass> endpointServiceClasses = psiClassQuery.findAll();

    for (PsiClass serviceClass : endpointServiceClasses) {
      // TODO: here is some code to ignore inner classes, not sure if this is allowed or not for entities
      //if (clazz.getContainingClass() == null) {
      //  continue;
      //}
      PsiAnnotation apiAnnotation = AnnotationUtil.findAnnotation(serviceClass, true, ANNOTATION_API_FQN);
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


  /**
   * NOTE : This method holds acquires writeAction and must be invoked from the dispatch thread
   * Copy Endpoints source files to an android module
   * @param androidGcmLibModuleRoot
   */
  public void copyEndpointSourceAndDepsToAndroidModule(final File androidGcmLibModuleRoot) {
    // genLibDirectory should have the following structure:
    // google_generated
    //     apiname1
    //        libs-expanded-source
    //             com
    //               example
    //                   ..
    //                    apiname1
    //                       Foo.java
    //        libs
    //           google-api-client.jar
    //           foo.jar
    //           ..
    //        descriptor.json
    //        proguard-google-api-client.txt
    //     apiname2
    //         <same as above>
    //     apiname3
    //         <same as above>


    // Return, there's no associated android module
    if (androidGcmLibModuleRoot == null) {
      return;
    }

    assert (myAppEngineModule.getName().endsWith(EndpointsConstants.APP_ENGINE_MODULE_SUFFIX));

    VirtualFile genLibVFile = FacetUtils.findFileUnderContentRoots(myAppEngineModule,
                                                                   EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);

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

      PsiDirectory expandedSourceDir = apiDir.findSubdirectory(API_CLIENT_EXPANDED_SOURCE_SUBFOLDER_NAME);

      if (expandedSourceDir == null) {
        continue;
      }

      apiNameToSourceRoot.put(apiName, expandedSourceDir);

      // While we're here, look for a folder with the dependent libs
      if (libsDirHolder[0] == null) {
        PsiDirectory subDir = apiDir.findSubdirectory(API_CLIENT_LIBS_SUBFOLDER_NAME);
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
        proguardFileHolder[0] = apiDir.findFile(API_CLIENT_PROGUARD_FILENAME);
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
      return;
    }

    // Now that we have all of the necessary information, we can go ahead and mutate the Android project

    // First, copy all of the generated source over to the android project, along with the dependencies
    // and add the appropriate source roots
    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        copySource(androidGcmLibModuleRoot, apiNameToSourceRoot);
        //addSourceAndLibsEntries(endpointSrcDir, androidLibsDir, androidModule);
        //updateProguardInformation(androidModule, proguardFileHolder[0]);
      }
    });
  }

  /**
   * Expands out the source from the generated client libraries
   * @param apiName
   * @return a reference to the expanded source directory
   */
  // TODO: A bit strange that we have this one and the static version as well...
  @Nullable
  public File expandSourceDirForApi(String apiName) {
    MavenProject mavenProject = MavenUtils.getMavenProjectForModule(myProject, myAppEngineModule.getName());
    return expandSourceDirForApi(mavenProject, apiName);
  }
}
