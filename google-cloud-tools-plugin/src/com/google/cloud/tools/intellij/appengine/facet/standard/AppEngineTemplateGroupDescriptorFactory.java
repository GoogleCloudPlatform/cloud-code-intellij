/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;

import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public class AppEngineTemplateGroupDescriptorFactory implements FileTemplateGroupDescriptorFactory {

  @NonNls
  public static final String APP_ENGINE_WEB_XML_TEMPLATE = "GctAppEngineWeb.xml";
  @NonNls
  public static final String APP_YAML_TEMPLATE = "app.yaml";
  @NonNls
  public static final String DOCKERFILE_WAR_TEMPLATE = "WarDockerfile.docker";
  @NonNls
  public static final String DOCKERFILE_JAR_TEMPLATE = "JarDockerfile.docker";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    final FileTemplateDescriptor appEngineXml = new FileTemplateDescriptor(
        APP_ENGINE_WEB_XML_TEMPLATE, StdFileTypes.XML.getIcon());

    final FileTemplateDescriptor appYaml = new FileTemplateDescriptor(
        APP_YAML_TEMPLATE, FileTypeManager.getInstance().getStdFileType("YAML").getIcon());

    // TODO Better icon for Dockerfiles?

    final FileTemplateDescriptor warDockerfile = new FileTemplateDescriptor(
        DOCKERFILE_WAR_TEMPLATE, StdFileTypes.PLAIN_TEXT.getIcon());

    final FileTemplateDescriptor jarDockerfile = new FileTemplateDescriptor(
        DOCKERFILE_JAR_TEMPLATE, StdFileTypes.PLAIN_TEXT.getIcon());

    return new FileTemplateGroupDescriptor("Google App Engine", GoogleCloudToolsIcons.APP_ENGINE,
        appEngineXml,
        appYaml,
        warDockerfile,
        jarDockerfile);
  }
}
