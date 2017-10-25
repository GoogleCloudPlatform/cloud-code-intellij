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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.appengine.facet.AddAppEngineFrameworkSupportAction;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModelImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a shortcut in the tools menu to add the App Engine Standard framework support to a
 * module.
 */
public class AddAppEngineStandardFrameworkSupportToolsMenuAction
    extends AddAppEngineFrameworkSupportAction {
  private AppEngineStandardSupportProvider provider;

  public AddAppEngineStandardFrameworkSupportToolsMenuAction() {
    super(
        GctBundle.message("appengine.standard.facet.name"),
        GctBundle.message("appengine.standard.name.in.message"));
    provider = new AppEngineStandardSupportProvider();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable getModuleConfigurable(Module module) {
    Project project = module.getProject();
    String contentRootPath = "";
    VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
    if (roots.length > 0) {
      contentRootPath = roots[0].getPath();
    }

    FrameworkSupportModelImpl model =
        new FrameworkSupportModelImpl(
            project, contentRootPath, LibrariesContainerFactory.createContainer(project));
    return provider.createConfigurable(model);
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider getModuleProvider() {
    return provider;
  }
}
