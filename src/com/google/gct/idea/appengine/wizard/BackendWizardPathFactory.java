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
package com.google.gct.idea.appengine.wizard;

import com.android.tools.idea.wizard.NewModuleWizardPathFactory;
import com.android.tools.idea.wizard.NewModuleWizardState;
import com.android.tools.idea.wizard.TemplateWizardStep;
import com.android.tools.idea.wizard.WizardPath;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author yole
 */
public class BackendWizardPathFactory implements NewModuleWizardPathFactory {
  @Override
  public Collection<WizardPath> createWizardPaths(@NotNull NewModuleWizardState wizardState,
                                                  @NotNull TemplateWizardStep.UpdateListener updateListener,
                                                  @Nullable Project project,
                                                  @Nullable Icon sidePanelIcon,
                                                  @NotNull Disposable disposable) {
    if (project == null || !ApplicationManager.getApplication().isInternal()) {
      return Collections.emptyList();
    }
    return Collections.<WizardPath>singleton(new BackendWizardPath(project, wizardState, updateListener, sidePanelIcon));
  }
}
