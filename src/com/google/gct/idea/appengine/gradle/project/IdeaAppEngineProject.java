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
import java.io.Serializable;

/**
 * Transient project wrapper for App Engine Gradle Projects during gradle imports
 */
public class IdeaAppEngineProject implements Serializable {
  @NotNull private final AppEngineModel myDelegate;
  @NotNull private final String myModuleName;
  @NotNull private final File myRootDirPath;
  private transient VirtualFile myRootDir;

  public IdeaAppEngineProject(@NotNull String moduleName, @NotNull File rootDir, @NotNull AppEngineModel delegate) {
    myModuleName = moduleName;
    myRootDirPath = rootDir;
    myDelegate = delegate;
  }

  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @NotNull
  // TODO : this can be useful when determining relative paths when populating the facet
  public File getRootDirPath() {
    if (myRootDir == null) {
      VirtualFile found = VfsUtil.findFileByIoFile(myRootDirPath, true);
      // the module's root directory can never be null.
      assert found != null;
      myRootDir = found;
    }
    return myRootDirPath;
  }

  @NotNull
  public AppEngineModel getDelegate() {
    return myDelegate;
  }
}
