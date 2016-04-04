/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.cloud.tools.intellij.debugger;

import com.google.common.annotations.VisibleForTesting;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Provides a translation between file names sent from the CDB API and IntelliJ project files in
 * the local file system.
 *
 * <p>This is necessary mostly because StackFrames locations in CDB only contain the package and
 * file name. The Cloud Debugger API returns StackFrame.SourceLocation.path in the form of
 *
 * com/my/package/Class.java
 *
 * while IntelliJ offers VirtualFiles whose path is the absolute path from root. e.g.,
 *
 * /home/user/workspace/repo/path/com/my/package/Class.java
 *
 * <p>These are methods which are Java specific for the cloud debugger. When we add other languages,
 * some of this may need to be extracted to an extensionpoint.
 */
public class ServerToIDEFileResolver {
  private VirtualFileSystem FILE_SYSTEM =
      VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);

  /**
   * Utility method that returns the full class name for a file.
   */
  public static String getCloudPathFromJavaFile(PsiJavaFile javaFile) {
    return javaFile.getPackageName().replace('.', '/') + "/" + javaFile.getName();
  }

  /**
   * Given a possible file path, returns a VirtualFile instance that the IDE can work with.
   *
   * <p>This method tries to fetch a file in three ways. First, it uses its full path in the local
   * file system. Then, it tries the full class name form (com/google/gct/idea/debugger/
   * CloudDebugProcess.java). Finally, it searches for possible file matches within the project.
   */
  public VirtualFile getFileFromPath(@NotNull Project project, @NotNull String path) {
    // Try the relative full project path.
    VirtualFile file = FILE_SYSTEM.findFileByPath(project.getBasePath() + "/" + path);
    // Try class name with package and class file name.
    if (file == null) {
      PsiPackage psiPackage = JavaPsiFacade.getInstance(project)
          .findPackage(getPackageFromPath(path));
      // If a class isn't in a project's classpath, psiPackage will be null.
      if (psiPackage != null) {
        PsiClass[] matchingClasses = psiPackage.findClassByShortName(
            getClassNameFromPath(path), GlobalSearchScope.allScope(project));
        if (matchingClasses.length > 0) {
          file = matchingClasses[0].getContainingFile().getVirtualFile();
        }
      }
    }
    // If we still couldn't find the file, search for possible file name matches and return the
    // first.
    // We might want to improve string matching and return more than one possible match.
    if (file == null) {
      Collection<VirtualFile> projectJavaFiles = FileBasedIndex.getInstance().getContainingFiles(
          FileTypeIndex.NAME, JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
      for (VirtualFile projectFile : projectJavaFiles) {
        if (projectFile.getName().equals(path)) {
          file = projectFile;
          break;
        }
      }
    }
    return file;
  }

  /**
   * Produces a Java package name from a file path.
   *
   * <p>Example: returns "com.java.package" from "com/java/package/Class.java".
   */
  @VisibleForTesting
  static String getPackageFromPath(String path) {
    String[] tokens = path.split("/");
    StringBuilder packageBuilder = new StringBuilder();
    if (tokens.length > 1) {
      if(tokens[0].length() > 0) {
        packageBuilder.append(tokens[0]);
      }
      for (int nToken = 1; nToken < tokens.length - 1; nToken++) {
        if(tokens[nToken].length() > 0 && packageBuilder.length() > 0) {
          packageBuilder.append(".");
        }
        packageBuilder.append(tokens[nToken]);
      }
    }

    return packageBuilder.toString();
  }

  /**
   * Produces a class name from a file path.
   *
   * <p>Example: returns "Class" from "com/java/package/Class.java".
   */
  @VisibleForTesting
  static String getClassNameFromPath(String path) {
    if (path.indexOf('.') != -1 && path.lastIndexOf("/") < path.lastIndexOf(".")) {
      return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf('.'));
    }

    return path.substring(path.lastIndexOf("/") + 1);
  }
}
