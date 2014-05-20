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

import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.wizard.*;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Wizard path for creating an App Engine backend module.
 */
public class BackendWizardPath implements WizardPath {
  private final Project myProject;
  private final NewModuleWizardState myWizardState;
  private final TemplateWizardStep.UpdateListener myUpdateListener;
  private final Icon mySidePanelIcon;
  private BackendModuleWizardStep myBackendModuleWizardStep;

  public BackendWizardPath(Project project,
                           NewModuleWizardState wizardState,
                           TemplateWizardStep.UpdateListener updateListener,
                           Icon sidePanelIcon) {
    myProject = project;
    myWizardState = wizardState;
    myUpdateListener = updateListener;
    mySidePanelIcon = sidePanelIcon;
  }

  @Override
  public Collection<ModuleWizardStep> getSteps() {
    myBackendModuleWizardStep = new BackendModuleWizardStep(myProject, myWizardState, myUpdateListener, mySidePanelIcon);
    return Collections.<ModuleWizardStep>singletonList(myBackendModuleWizardStep);
  }

  @Override
  public void update() {
    myBackendModuleWizardStep.loadDataFromTemplate();
  }

  @Override
  public void createModule() {
    Object moduleName = myWizardState.get(NewAppEngineModuleAction.ATTR_MODULE_NAME);
    if (moduleName == null) {
      myWizardState.put(NewAppEngineModuleAction.ATTR_MODULE_NAME, myWizardState.get(NewProjectWizardState.ATTR_MODULE_NAME));
    }
    NewAppEngineModuleAction.createModule(myProject, myWizardState.getTemplate().getRootPath(),
                                          myWizardState.getString(NewAppEngineModuleAction.ATTR_MODULE_NAME),
                                          myWizardState.getString(TemplateMetadata.ATTR_PACKAGE_NAME),
                                          myWizardState.getString(NewAppEngineModuleAction.ATTR_CLIENT_MODULE_NAME));
  }

  @Override
  public boolean isStepVisible(ModuleWizardStep step) {
    return step instanceof BackendModuleWizardStep;
  }

  @Override
  public Collection<String> getExcludedTemplates() {
    return Collections.emptyList();
  }

  @Override
  public Collection<ChooseTemplateStep.MetadataListItem> getBuiltInTemplates() {
    List<AppEngineTemplates.TemplateInfo> templates = AppEngineTemplates.getTemplates();
    List<ChooseTemplateStep.MetadataListItem> result = new ArrayList<ChooseTemplateStep.MetadataListItem>();
    for (AppEngineTemplates.TemplateInfo template : templates) {
      result.add(new ChooseTemplateStep.MetadataListItem(template.getFile(), template.getMetadata()));
    }
    return result;
  }

  @Override
  public boolean supportsGlobalWizard() {
    return false;
  }
}
