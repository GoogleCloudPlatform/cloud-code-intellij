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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Utility class that extracts the available resource templates for use in the new cloud module wizards.
 */
public final class CloudTemplateUtils {
  private static final String TEMPLATES_DIR = "templates";
  private static final String CLIENT_TEMPLATES_DIR = "clientTemplates";
  private static final String HELLO_WORLD = "HelloWorld";
  private static final String HELLO_ENDPOINTS = "HelloEndpoints";
  private static final String GCM_ENDPOINTS = "GcmEndpoints";
  private static final Logger LOG = Logger.getInstance(CloudTemplateUtils.class);

  // Used by CloudModuleUtils.
  static final List<String> LOCAL_ENDPOINTS_TEMPLATES = ImmutableList.of(HELLO_ENDPOINTS, GCM_ENDPOINTS);
  private static final List<String> LOCAL_TEMPLATES = ImmutableList.of(HELLO_WORLD, HELLO_ENDPOINTS, GCM_ENDPOINTS);

  /**
   * Returns a list of templates that are stored locally as part of the cloud tools plugin.
   */
  @NotNull
  public static List<TemplateInfo> getTemplates() {
    LOG.info("Populating built-in App Engine templates...");
    return getTemplates(LOCAL_TEMPLATES);
  }

  /**
   * Returns a list of App Engine templates from the App Engine template depot associated with the specified
   * {@code templateDirectories}.
   *
   * @param templateDirectories The list of template directories of interest
   */
  @NotNull
  private static List<TemplateInfo> getTemplates(List<String> templateDirectories) {
    return populateTemplates(findTemplatesDirInPluginJar(TEMPLATES_DIR), templateDirectories);
  }

  /**
   * Returns the App Engine template located in the {@code templateDirectory} of the App Engine template depot.
   *
   * @param templateDirectory The template directory of interest
   * @return The template and its metadata in the {@code templateDirectory} of template depot or null
   * if the template is not found.
   */
  @Nullable
  public static TemplateInfo getTemplate(String templateDirectory) {
    final List<TemplateInfo> templateInfoList = getTemplates(ImmutableList.of(templateDirectory));
    return templateInfoList.size() == 1 ? templateInfoList.get(0) : null;
  }

  /**
   * Returns a list of App Engine templates from {@code root} associated with the specified {@code templateDirectories}.
   *
   * @param root                The template depot
   * @param templateDirectories The list of template directories of interest
   * @return A list of templates and their associated metadata
   */
  @NotNull
  private static List<TemplateInfo> populateTemplates(@NotNull File root, List<String> templateDirectories) {
    final ImmutableList.Builder<TemplateInfo> templatesBuilder = ImmutableList.builder();
    if (root.exists()) {
      for (String template : templateDirectories) {
        final File file = new File(root, template);
        if (file.exists() && file.isDirectory() && new File(file, Template.TEMPLATE_XML_NAME).exists()) {
          final TemplateMetadata templateMetadata = TemplateManager.getInstance().getTemplate(file);
          assert templateMetadata != null;
          templatesBuilder.add(new TemplateInfo(file, templateMetadata));
        } else {
          LOG.error("Template is corrupt or missing : " + template);
        }
      }
    } else {
      LOG.error("Failed to find templates directory at \"" + root + "\", perhaps your cloud tools plugin is corrupt?");
    }
    return templatesBuilder.build();
  }

  // Used by CloudModuleUtils.
  static Template getClientModuleTemplate(String templateName) {
    final File templateFile = new File(findTemplatesDirInPluginJar(CLIENT_TEMPLATES_DIR), templateName);
    Preconditions.checkState(templateFile.exists(), "Failed to find client template " + templateName);
    return Template.createFromPath(templateFile);
  }

  private static File findTemplatesDirInPluginJar(String name) {
    File jarPath = new File(PathUtil.getJarPathForClass(CloudTemplateUtils.class));
    if (jarPath.isFile()) {
      jarPath = jarPath.getParentFile();
    }
    return new File(jarPath, name);
  }

  /**
   * A lightweight container class that holds a template and its metadata.
   */
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
      return Objects.toStringHelper(this)
        .add("file", myFile)
        .add("title", myMetadata.getTitle())
        .toString();
    }
  }

  private CloudTemplateUtils() {
    // This utility class should not be instantiated.
  }
}
