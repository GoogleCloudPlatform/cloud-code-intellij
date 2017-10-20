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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.google.cloud.tools.intellij.appengine.facet.AddAppEngineFrameworkSupportAction;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleSupportProvider.AppEngineFlexibleSupportConfigurable;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a shortcut in the tools menu to add the App Engine Flexible framework support to a
 * module.
 */
public class AddAppEngineFlexibleFacetToolsMenuAction extends AddAppEngineFrameworkSupportAction {
  public AddAppEngineFlexibleFacetToolsMenuAction() {
    super(GctBundle.message("appengine.flexible.facet.name"),
        GctBundle.message("appengine.add.flexible.framework.support.tools.menu.description"));
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable getModuleConfigurable(Module module) {
    return new AppEngineFlexibleSupportConfigurable();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleProvider getModuleProvider() {
    return new AppEngineFlexibleSupportProvider();
  }

}
