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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PathUtil;

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

  /** Returns a list of templates that are stored locally as part of the cloud tools plugin */
  public static List<TemplateInfo> getLocalTemplates() {
    TemplateManager templateManager = TemplateManager.getInstance();
    File jarPath = new File(PathUtil.getJarPathForClass(AppEngineTemplates.class));
    if (jarPath.isFile()) {
      jarPath = jarPath.getParentFile();
    }
    File root = new File(jarPath, TEMPLATES_DIR);

    List<TemplateInfo> templates = new ArrayList<TemplateInfo>();
    if (root.exists()) {
      for(String template : LOCAL_TEMPLATES) {
        File file = new File(root, template);
        if(file.exists() && file.isDirectory() && (new File(file, Template.TEMPLATE_XML_NAME)).exists()) {
          templates.add(new TemplateInfo(file, templateManager.getTemplate(file)));
        }
        else {
          LOG.error("Template is corrupt or missing : " + template);
        }
      }
    }
    else {
      LOG.error("Failed to find templates directory, perhaps your cloud tools plugin is corrupt?");
    }

    return templates;
  }

  private static final String DEFAULT_OWNER = "mycompany.com";
  private static final String DEFAULT_PACKAGE = "services";
  public static final String ATTR_ENDPOINTS_OWNER = "endpointOwnerDomain";
  public static final String ATTR_ENDPOINTS_PACKAGE = "endpointPackagePath";

  /** Populate endpoints specific template parameters into the replacement map */
  public static void populateEndpointParameters(Map<String, Object> replacementMap, String rootPackage) {

    String[] packageComponents = rootPackage.split("\\.");

    String ownerDomain = DEFAULT_OWNER;
    String packagePath = DEFAULT_PACKAGE;

    if (packageComponents.length >= 2) {
      ownerDomain = packageComponents[1] + "." + packageComponents[0];
      packagePath = "";
      for (int i = 2; i < packageComponents.length; i++) {
        packagePath += packageComponents[i];
        if (i != packageComponents.length - 1) {
          packagePath += ".";
        }
      }
    }
    replacementMap.put(ATTR_ENDPOINTS_OWNER, ownerDomain);
    replacementMap.put(ATTR_ENDPOINTS_PACKAGE, packagePath);
  }
}
