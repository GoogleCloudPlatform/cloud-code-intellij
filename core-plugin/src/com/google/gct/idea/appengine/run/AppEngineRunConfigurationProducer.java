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

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;

/**
 * Run configuration producer for App Engine modules
 * TODO: Make this useful or remove
 */
public class AppEngineRunConfigurationProducer extends JavaRunConfigurationProducerBase<AppEngineRunConfiguration> {

  public AppEngineRunConfigurationProducer() {
    super(AppEngineRunConfigurationType.getInstance());
  }

  @Override
  protected boolean setupConfigurationFromContext(AppEngineRunConfiguration configuration,
                                                  ConfigurationContext context,
                                                  Ref<PsiElement> sourceElement) {
    return false;
  }

  @Override
  public boolean isConfigurationFromContext(AppEngineRunConfiguration configuration, ConfigurationContext context) {
    return false;
  }
}
