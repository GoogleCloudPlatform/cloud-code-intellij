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

package com.google.cloud.tools.intellij.appengine.facet;

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.IdeaModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by nbashirbello on 10/18/17.
 */
public class AddAppEngineFrameworkSupportDialog extends DialogWrapper {
  // TODO: Why is AppEngineFlexibleSupportConfigurable static?
  private FrameworkSupportInModuleConfigurable configurable;
  private Module module;

  public AddAppEngineFrameworkSupportDialog (@Nullable Project project, Module module,
      @NotNull FrameworkSupportInModuleConfigurable frameworkSupportInModuleConfigurable) {
    super(project);
    setTitle("Some title");
    this.module = module;
    //configurable = new AppEngineFlexibleSupportConfigurable();
    configurable = frameworkSupportInModuleConfigurable;
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return configurable.createComponent();
  }


  @Override
  protected void doOKAction() {
    if (getOKAction().isEnabled()) {
      // TODO: action?
      new WriteAction() {
        protected void run(@NotNull final Result result) {
          ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
          final IdeaModifiableModelsProvider modifiableModelsProvider = new IdeaModifiableModelsProvider();
          configurable.addSupport(module, model, modifiableModelsProvider);
          model.commit();
        }
      }.execute();
    }
    super.doOKAction();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#com.intellij.ide.util.frameworkSupport.AddFrameworkSupportDialog";
  }
}
