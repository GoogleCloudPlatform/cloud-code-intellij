/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Representation of the Docker configuration file type used for generation of Dockerfile's from
 * templates. The "dummy" extension is needed so that the template system will work. It is up to the
 * generating code to handle this - i.e. remove the extension after generation to comply with the
 * extension'less Dockerfile convention.
 */
public class DockerFileTemplateType extends LanguageFileType {

  public static final DockerFileTemplateType INSTANCE = new DockerFileTemplateType();
  static final String DEFAULT_EXTENSION = "gaedocker";

  private DockerFileTemplateType() {
    super(DockerFileTemplateLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "App Engine Dockerfile Template";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "App Engine Dockerfile Template";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return StdFileTypes.PLAIN_TEXT.getIcon();
  }
}
