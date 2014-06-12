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
package com.google.gct.idea.appengine.gradle.project;

import com.google.appengine.gradle.model.AppEngineModel;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Project wrapper for App Engine Gradle Projects
 */
public class IdeaAppEngineProject {
  @NotNull private final String myModuleName;
  @NotNull private final VirtualFile myRootDir;
  @NotNull private final AppEngineModel myDelegate;

  public IdeaAppEngineProject(@NotNull String moduleName, @NotNull File rootDir, @NotNull AppEngineModel delegate) {
    myModuleName = moduleName;
    VirtualFile found = VfsUtil.findFileByIoFile(rootDir, true);
    // the module's root directory can never be null.
    assert found != null;
    myRootDir = found;
    myDelegate = delegate;
  }

  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @NotNull
  public VirtualFile getRootDir() {
    return myRootDir;
  }

  @NotNull
  public AppEngineModel getDelegate() {
    return myDelegate;
  }
}
