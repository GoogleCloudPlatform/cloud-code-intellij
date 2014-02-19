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
package com.google.gct.idea.appengine.run;

import com.intellij.execution.Location;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/** Run configuration producer for App Engine modules */
public class AppEngineRunConfigurationProducer extends JavaRuntimeConfigurationProducerBase {

  private PsiElement myElementContext;

  public AppEngineRunConfigurationProducer() {
    super(AppEngineRunConfigurationType.getInstance());
  }

  @Override
  public PsiElement getSourceElement() {
    return myElementContext;
  }

  @Nullable
  @Override
  protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context) {
    Module module = context.getModule();
    if (module == null) return null;

    myElementContext = location.getPsiElement();

    return createConfiguration(location.getProject(), module);
  }

  @Override
  public int compareTo(Object o) {
    // TODO: Provide better comparison algorithm
    return 0;
  }

  private RunnerAndConfigurationSettings createConfiguration(@Nullable Project project, Module module) {
    if (project == null) {
      return null;
    }

    RunnerAndConfigurationSettings settings =
      RunManagerEx.getInstanceEx(project).createRunConfiguration(module.getName() + " - " + AppEngineRunConfiguration.NAME, getConfigurationFactory());
    final AppEngineRunConfiguration configuration = (AppEngineRunConfiguration)settings.getConfiguration();

    configuration.setModule(module);

    return settings;
  }
}
