package com.google.gct.intellij.endpoints.generator.sample;

import com.android.SdkConstants;
import com.android.sdklib.BuildToolInfo;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.wizard.NewModuleWizardState;
import com.android.tools.idea.wizard.NewProjectWizardState;
import com.android.tools.idea.wizard.TemplateWizard;

import com.google.common.io.Files;
import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtil;

import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Generator for the sample that deals with the Android Library module
 */
public class AndroidGradleGcmLibGenerator {

  protected final Project myProject;
  protected final Module myAndroidModule;
  protected final Module myAppEngineModule;
  protected final String myProjectNumber;
  protected final String myRootPackage;
  protected final String myLibModuleName;
  protected final File myLibModuleRoot;
  protected AppEngineMavenProject myAppEngineMavenProject;

  private static final String SDK_TEMPLATES = "sdktemplates";
  private static final String GCM_ACTIVITY = "GcmActivity";
  private static final String MESSAGE_ENDPOINT_API_NAME = "messageEndpoint";
  private static final String DEVICE_INFO_ENDPOINTS_API_NAME = "deviceinfoendpoint";
  private static final String TEMPLATE_DEVICE_INFO_ENDPOINT = "deviceInfoEndpointImport";
  private static final String TEMPLATE_DEVICE_INFO = "deviceInfoImport";
  private static final String TEMPLATE_MESSAGE_ENDPOINT = "messageEndpointImport";
  private static final String TEMPLATE_MESSAGE_DATA = "messageDataImport";
  private static final String TEMPLATE_COLLECTION_RESPONSE_MESSAGE_DATA = "collectionResponseMessageDataImport";
  private static final String TEMPLATE_PROJECT_NUMBER = "gcmProjectNumber";
  private static final String DEFAULT_IMPORT_PATH = "com.unknown";

  private static final Logger LOG = Logger.getInstance(AndroidGradleGcmLibGenerator.class);

  /**
   *
   * @param project
   *    The IDEA/Studio project
   * @param androidModule
   *    An android module that will use this gcm library module
   * @param appEngineModule
   *    The connected App Engine module from which this will get client libs from
   * @param rootPackage
   *    The root java package that will be used here
   * @param projectNumber
   *    A cloud console project number (for GCM), can be empty, but not null
   */
  public AndroidGradleGcmLibGenerator(@NotNull Project project, @NotNull Module androidModule, @NotNull Module appEngineModule,
                                      @NotNull String rootPackage, @NotNull String projectNumber) {
    myProject = project;
    myAndroidModule = androidModule;
    myAppEngineModule = appEngineModule;
    myProjectNumber = projectNumber;
    myRootPackage = rootPackage;
    myLibModuleName = myAndroidModule.getName() + GctConstants.ANDROID_GCM_LIB_MODULE_SUFFIX;
    myLibModuleRoot = new File(myProject.getBasePath(), myLibModuleName);
  }

  /** Generate an Android Library module for the Cloud Backend Sample*/
  public void generate(@Nullable final Callback callback) {

    if (myLibModuleRoot.exists()) {
      logAndCallbackFailure("Android library module " + myLibModuleName + " already exists", null, callback);
      return;
    }
    else {
      myLibModuleRoot.mkdirs();
    }

    myAppEngineMavenProject = AppEngineMavenProject.get(myAppEngineModule);
    if (myAppEngineMavenProject == null) {
      logAndCallbackFailure("Could not find maven project in App Engine module", null, callback);
      return;
    }

    File templateDir;
    if ((templateDir = findTemplate()) == null) {
      // this should not be reached
      logAndCallbackFailure("Failed to find template, your plugin installation may be corrupt", null, callback);
      return;
    }

    final Template t = Template.createFromPath(templateDir);

    try {
      final Map<String, Object> templateReplacementMap =  buildReplacementMap(t);
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          t.render(myLibModuleRoot, myLibModuleRoot, templateReplacementMap);
        }
      });
    } catch (IOException e) {
      logAndCallbackFailure("Unable to find source files for sample client libraries under "
                            + GctConstants.APP_ENGINE_GENERATED_LIB_DIR, null, callback);
      return;
    }

    try {
      copyGcmJar();
    } catch (IOException e) {
      logAndCallbackFailure("Unable to find source files for sample client libraries under "
                            + GctConstants.APP_ENGINE_GENERATED_LIB_DIR, e, callback);
      return;
    }

    myAppEngineMavenProject.copyExpandedSources(myLibModuleRoot);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          GradleProjectImporter.getInstance().reImportProject(myProject, new GradleProjectImporter.Callback() {
            @Override
            public void projectImported(@NotNull Project project) {
              // I don't see how this can fail, but perhaps after the project is imported, the module could not exists?
              Module androidLibModule = ModuleManager.getInstance(project).findModuleByName(myLibModuleName);
              if(androidLibModule == null) {
                logAndCallbackFailure("Could not find Android Gcm Lib module after import, perhaps a sync is required", null, callback);
                return;
              }
              if (callback != null) {
                callback.moduleCreated(androidLibModule);
              }
            }

            @Override
            public void importFailed(@NotNull Project project, @NotNull String errorMessage) {
              logAndCallbackFailure("Import of Android Gcm library failed : " + errorMessage, null, callback);
            }
          });
        }
        catch (ConfigurationException e) {
          logAndCallbackFailure("Configuration exception when re-importing Android Gcm Module project", e, callback);
        }
      }
    });
  }

  @Nullable
  private static File findTemplate() {
    File jarPath = new File(PathUtil.getJarPathForClass(AndroidGradleGcmLibGenerator.class));
    if (jarPath.isFile()) {
      jarPath = jarPath.getParentFile();
    }

    File localTemplateDir = new File(jarPath, SDK_TEMPLATES);
    File blankLibraryTemplateDir = new File(localTemplateDir, GCM_ACTIVITY);

    if(blankLibraryTemplateDir.exists()) {
      return blankLibraryTemplateDir;
    }
    return null;
  }

  // TODO: this should be converted to import/check for play services at some point
  private void copyGcmJar() throws IOException {
    // grab gcm.jar from Android SDK extras
    File gcmJar = new File(AndroidSdkUtils.tryToChooseAndroidSdk().getLocation() + GctConstants.ANDROID_SDK_GCM_PATH);
    File androidLibModuleLibsFolder = new File(myLibModuleRoot, SdkConstants.LIBS_FOLDER);
    if(!androidLibModuleLibsFolder.exists()) {
      androidLibModuleLibsFolder.mkdirs();
    }
    File targetGcmJar = new File(androidLibModuleLibsFolder, "gcm.jar");

    Files.copy(gcmJar, targetGcmJar);
  }

  @NotNull
  private Map<String, Object> buildReplacementMap(@NotNull Template loadedTemplate) throws IOException {

    Map<String, Object> nameToObjMap = new HashMap<String, Object>();

    Map<String, String> clientLibBasedReplacements = buildReplacementsBasedOnGeneratedEndpointLibs();

    nameToObjMap.putAll(clientLibBasedReplacements);

    nameToObjMap.put(NewModuleWizardState.ATTR_PROJECT_LOCATION, myProject.getBasePath());

    // All of these hardcoded values can be implied from the master Android module
    //nameToObjMap.put(TemplateMetadata.ATTR_IS_NEW_PROJECT, false);
    nameToObjMap.put(TemplateMetadata.ATTR_IS_GRADLE, "true");
    nameToObjMap.put(TemplateMetadata.ATTR_CREATE_ICONS, false);
    nameToObjMap.put(TemplateMetadata.ATTR_IS_LIBRARY_MODULE, true);
    nameToObjMap.put(NewModuleWizardState.ATTR_CREATE_ACTIVITY, false);


    BuildToolInfo buildTool = AndroidSdkUtils.tryToChooseAndroidSdk().getLatestBuildTool();
    if (buildTool != null) {
      // If buildTool is null, the template will use buildApi instead, which might be good enough.
      nameToObjMap.put(TemplateMetadata.ATTR_BUILD_TOOLS_VERSION, buildTool.getRevision().toString());
    }

    nameToObjMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, myRootPackage);

    // Convert these values to ints
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_MIN_API);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_MIN_API_LEVEL);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_BUILD_API);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_TARGET_API);

    File mainFlavorSourceRoot = new File(myLibModuleRoot, TemplateWizard.MAIN_FLAVOR_SOURCE_PATH);
    File javaSourceRoot = new File(mainFlavorSourceRoot, TemplateWizard.JAVA_SOURCE_PATH);
    File javaSourcePackageRoot = new File(javaSourceRoot, myRootPackage.replace('.', '/'));
    File resourceSourceRoot = new File(mainFlavorSourceRoot, TemplateWizard.RESOURCE_SOURCE_PATH);

    nameToObjMap.put(NewProjectWizardState.ATTR_MODULE_NAME, myLibModuleRoot.getName());
    //nameToObjMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_MANIFEST_OUT, mainFlavorSourceRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_SRC_OUT, javaSourcePackageRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_RES_OUT, resourceSourceRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_TOP_OUT, myProject.getBasePath());


    String mavenUrl = System.getProperty("android.mavenRepoUrl");
    if (mavenUrl != null) {
      nameToObjMap.put(TemplateMetadata.ATTR_MAVEN_URL, mavenUrl);
    }


    return nameToObjMap;
  }

  @NotNull
  private Map<String, String> buildReplacementsBasedOnGeneratedEndpointLibs() throws IOException {
    Map<String, String> paramMap = new HashMap<String, String>();

    File deviceinfoEndpointSrcFolder = myAppEngineMavenProject.expandClientLibrary(DEVICE_INFO_ENDPOINTS_API_NAME);

    File messageEndpointsSrcFolder = myAppEngineMavenProject.expandClientLibrary(MESSAGE_ENDPOINT_API_NAME);

    paramMap.put(TEMPLATE_PROJECT_NUMBER, myProjectNumber);

    paramMap.put(TEMPLATE_DEVICE_INFO, getImportPathForClass(deviceinfoEndpointSrcFolder, "DeviceInfo.java"));
    paramMap.put(TEMPLATE_DEVICE_INFO_ENDPOINT, getImportPathForClass(deviceinfoEndpointSrcFolder, "Deviceinfoendpoint.java"));

    paramMap.put(TEMPLATE_MESSAGE_DATA, getImportPathForClass(messageEndpointsSrcFolder, "MessageData.java"));
    paramMap.put(TEMPLATE_MESSAGE_ENDPOINT, getImportPathForClass(messageEndpointsSrcFolder, "MessageEndpoint.java"));
    paramMap.put(TEMPLATE_COLLECTION_RESPONSE_MESSAGE_DATA,
                 getImportPathForClass(messageEndpointsSrcFolder, "CollectionResponseMessageData.java"));

    return paramMap;
  }

  @NotNull
  private static String getImportPathForClass(@NotNull File srcRootDir, @NotNull String classFileName) {
    Pattern p = Pattern.compile(classFileName);

    List<File> matchingClassFiles = FileUtil.findFilesByMask(p, srcRootDir);

    if (matchingClassFiles.size() == 0) {
      return DEFAULT_IMPORT_PATH;
    }
    String importPath = FileUtil.getRelativePath(srcRootDir, matchingClassFiles.get(0)).replace(File.separatorChar, '.');

    if (importPath == null) {
      return DEFAULT_IMPORT_PATH;
    }

    int indexOfJavaSuffix = importPath.lastIndexOf(".java");
    if (indexOfJavaSuffix < 0) {
      return DEFAULT_IMPORT_PATH;
    }

    return importPath.substring(0, indexOfJavaSuffix);
  }

  private static void convertToInt(Map<String, Object> nameToObjMap, Template loadedTemplate, String attrName) {
    String strVal = loadedTemplate.getMetadata().getParameter(attrName).initial;
    if (strVal == null) {
      return;
    }

    try {
      int intVal = Integer.parseInt(strVal);
      nameToObjMap.put(attrName, intVal);
    }
    catch (NumberFormatException nfe) {
      LOG.error("Template generation error", nfe);
    }
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

  /** Callback because this generator module makes some asynchronous calls */
  public static interface Callback {
    /** There are no guarantees on what thread this will return */
    void moduleCreated(Module androidLibModule);
    /** There are no guarantees on what thread this will return */
    void onFailure(String errorMessage);
  }
}
