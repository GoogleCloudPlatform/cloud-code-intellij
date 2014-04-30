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
package com.google.gct.idea.appengine.wizard;

import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateManager;
import com.android.tools.idea.templates.TemplateMetadata;
import com.google.gct.idea.appengine.util.AppEngineUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Class that defines available templates for App Engine wizards */
public class AppEngineTemplates {

  public static final Logger LOG = Logger.getInstance(AppEngineTemplates.class);

  public static final String TEMPLATES_DIR = "templates";
  public static final String HELLO_WORLD = "HelloWorld";
  public static final String HELLO_ENDPOINTS = "HelloEndpoints";
  public static final String GCM_ENDPOINTS = "GcmEndpoints";

  public static final List<String> LOCAL_TEMPLATES = Arrays.asList(HELLO_WORLD, HELLO_ENDPOINTS, GCM_ENDPOINTS);
  public static final List<String> LOCAL_ENDPOINTS_TEMPLATES = Arrays.asList(HELLO_ENDPOINTS, GCM_ENDPOINTS);

  // ToDo: remove after integrating with SampleSyncTask
  private final static String ANDROID_REPO_PATH = System.getProperty("user.home") + "/.android/cloud";

  /** Class that encapsulates a templates and its metadata */
  public static class TemplateInfo {
    private final File myFile;
    private final TemplateMetadata myMetadata;

    public TemplateInfo(File templateFile, TemplateMetadata templateMetadata) {
      myFile = templateFile;
      myMetadata = templateMetadata;
    }

    public File getFile() {
      return myFile;
    }

    public TemplateMetadata getMetadata() {
      return myMetadata;
    }

    @Override
    public String toString() {
      return myMetadata.getTitle();
    }
  }

  /** Returns a list of App Engine templates.
   * Depending on user preference, returns either the built-in templates or the cached
   * templates in the local git repository.
   */
  public static List<TemplateInfo> getTemplates() {
    boolean usingBuiltInTemplates = true; //SampleSyncConfiguration.usingBuiltInSamples()
    if (usingBuiltInTemplates) {
      return getLocalTemplates();
    } else {
      return getCachedTemplates();
    }
  }

  /** Returns a list of templates that are stored locally as part of the cloud tools plugin */
  public static List<TemplateInfo> getLocalTemplates() {
    LOG.info("Populating built-in App Engine templates...");
    return getAppEngineTemplates(LOCAL_TEMPLATES);
  }

  /** Returns a list of templates that are stored in a local repository */
  public static List<TemplateInfo> getCachedTemplates() {
    LOG.info("Populating cached App Engine templates...");

    File root = new File(ANDROID_REPO_PATH, TEMPLATES_DIR);
    if (!root.exists()) {
      LOG.error("Failed to find cached templates directory, using built-in templates");
      return getLocalTemplates();
    }

    return populateAppEngineTemplates(root, LOCAL_TEMPLATES);
  }

  public static final String ATTR_ENDPOINTS_OWNER = "endpointOwnerDomain";
  public static final String ATTR_ENDPOINTS_PACKAGE = "endpointPackagePath";

  /**
   * Populate endpoints specific template parameters into the replacement map
   * Owner Domain is reverse of package path
   */
  public static void populateEndpointParameters(Map<String, Object> replacementMap, String rootPackage) {
    String[] pkgParts = rootPackage.split("\\.");
    String ownerDomain = StringUtil.join(ArrayUtil.reverseArray(pkgParts),".");
    replacementMap.put(ATTR_ENDPOINTS_OWNER, ownerDomain);
    replacementMap.put(ATTR_ENDPOINTS_PACKAGE, "");
  }

  /**
   * Returns the App Engine template located in the <code>templateDirectory</code>
   * of the App Engine template depot.
   * @param templateDirectory The template directory of interest.
   * @return  The template and its metadata in the <code>templateDirectory</code> of template depot or null
   * if the template is not found.
   */
  public static @Nullable
  TemplateInfo getAppEngineTemplate(String templateDirectory) {
    List<String> templateDirectories = Arrays.asList(templateDirectory);
    List<TemplateInfo> templateInfoList = getAppEngineTemplates(templateDirectories);

    if (templateInfoList.size() == 1) {
      return templateInfoList.get(0);
    } else {
      return null;
    }
  }

  /**
   * Returns a list of App Engine templates from the App Engine template depot
   * associated with the specified <code>templateDirectories</code>.
   * @param templateDirectories  The list of template directories of interest.
   * @return A list of templates and their associated metadata.
   */
  public static @NotNull
  List<TemplateInfo> getAppEngineTemplates(List<String> templateDirectories) {
    File jarPath = new File(PathUtil.getJarPathForClass(AppEngineUtils.class));
    if (jarPath.isFile()) {
      jarPath = jarPath.getParentFile();
    }

    File root = new File(jarPath, TEMPLATES_DIR);
    return populateAppEngineTemplates(root, templateDirectories);
  }

  /**
   * Returns a list of App Engine templates from <code>root</code>
   * associated with the specified <code>templateDirectories</code>.
   * @param root The template depot.
   * @param templateDirectories  The list of template directories of interest.
   * @return A list of templates and their associated metadata.
   */
  public static List<TemplateInfo> populateAppEngineTemplates(@NotNull File root, List<String> templateDirectories) {
    TemplateManager templateManager = TemplateManager.getInstance();
    List<TemplateInfo> templates = new ArrayList<TemplateInfo>();

    if (root.exists()) {
      for (String template : templateDirectories) {
        File file = new File(root, template);
        if (file.exists() && file.isDirectory() && (new File(file, Template.TEMPLATE_XML_NAME)).exists()) {
          templates.add(new TemplateInfo(file, templateManager.getTemplate(file)));
        }
        else {
          LOG.error("Template is corrupt or missing : " + template);
        }
      }
    }
    else {
      LOG.error("Failed to find templates directory at \"" + root + "\", perhaps your cloud tools plugin is corrupt?");
    }
    return templates;
  }

}
