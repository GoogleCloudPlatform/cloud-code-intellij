/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * These are methods which are Java specific for the cloud debugger. When we add other languages, some of this may need
 * to be extracted to an extensionpoint.
 */
public class JavaUtil {
  private static final Map<Project, Map<String, VirtualFile>> ourLocationMaps =
    new ConcurrentHashMap<Project, Map<String, VirtualFile>>();

  public static String getCloudPathFromJavaFile(PsiJavaFile javaFile) {
    return javaFile.getPackageName().replace('.', '/') + "/" + javaFile.getName();
  }

  /**
   * Given the cloud path in package form (com/google/gct/idea/debugger/CloudDebugProcess.java) it returns the
   * VirtualFile within the IDE that represents that class.
   */
  public static VirtualFile getFileFromCloudPath(@NotNull Project project, @NotNull String cloudPath) {
    return ourLocationMaps.get(project).get(cloudPath);
  }

  /**
   * Creates a map from package-path syntax to {@link VirtualFile} which allows us to build IDE sourcelocations given
   * information from the Cloud Debugger api. We only do a force refresh when re-attaching to a target because the
   * project state may change between debug sessions.
   */
  public static void initializeLocations(@NotNull final Project project, boolean forceRefresh) {
    if (forceRefresh || !ourLocationMaps.containsKey(project)) {
      final Map<String, VirtualFile> locationMap = Maps.newHashMap(); // The inner does not have to be concurrent.
      ProjectFileIndex.SERVICE.getInstance(project).iterateContent(new ContentIterator() {
        @Override
        public boolean processFile(VirtualFile fileOrDir) {
          String cloudPathIndex = getCloudPathFromFile(project, fileOrDir);
          if (!Strings.isNullOrEmpty(cloudPathIndex)) {
            locationMap.put(cloudPathIndex, fileOrDir);
          }
          return true;
        }
      });
      ourLocationMaps.put(project, locationMap);
    }
  }

  /**
   * Given a {@link VirtualFile}, returns the package path format for it.
   */
  private static String getCloudPathFromFile(@Nullable Project project, @Nullable VirtualFile file) {
    if (file == null || project == null) {
      return null;
    }
    PsiFile javaFile = PsiManager.getInstance(project).findFile(file);
    if (!(javaFile instanceof PsiJavaFile)) {
      return null;
    }
    return getCloudPathFromJavaFile((PsiJavaFile)javaFile);
  }
}
