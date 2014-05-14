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
package com.google.gct.idea.appengine.util;

import com.android.tools.idea.gradle.parser.GradleBuildFile;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateManager;
import com.google.gct.idea.appengine.wizard.AppEngineTemplates;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.PathUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for App Engine.
 */
public class AppEngineUtils {
  private static final Logger LOG = Logger.getInstance(AppEngineUtils.class);
  private static final String APP_ENGINE_PLUGIN = "appengine";

  /**
   * Returns true if <code>module</code> is an App Engine module by checking to see
   * if the App Engine plugin is part of the gradle build. Returns false otherwise.
   * @param project The project associated with <code>module</code>
   * @param module The module to be evaluated for being an App Engine module.
   * @return true if <code>module</code> is an App Engine module, false otherwise.
   * @throws FileNotFoundException if the gradle build file associated with <code>module</code> does not exist.
   */
  public static boolean isAppEngineModule(Project project, Module module) throws FileNotFoundException {
    final VirtualFile buildFile = GradleUtil.getGradleBuildFile(module);
    if (buildFile == null) {
      throw new FileNotFoundException("Cannot find gradle build file for module \"" + module.getName() + "\"");
    }

    final GradleBuildFile gradleBuildFile = new GradleBuildFile(buildFile, project);
    List<String> allPlugins = gradleBuildFile.getPlugins();
    return allPlugins.contains(APP_ENGINE_PLUGIN);
  }
}
