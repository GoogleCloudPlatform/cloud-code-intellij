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

package com.google.cloud.tools.intellij.appengine.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Representation of the Docker configuration file type.
 */
public class DockerFileType extends LanguageFileType {

  public static final DockerFileType INSTANCE = new DockerFileType();
  static final String DEFAULT_EXTENSION = "docker";

  private DockerFileType() {
    super(DockerFileLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "Docker";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Dockerfile";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    // TODO Better icon for Dockerfiles?
    return StdFileTypes.PLAIN_TEXT.getIcon();
  }
}
