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
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

import static com.android.tools.idea.wizard.ScopedStateStore.*;

/**
 * The global path used in the "create a new AppEngine module" wizard process.
 */
public class NewCloudModuleDynamicWizardPath extends DynamicWizardPath implements NewModuleDynamicPath {
  static final Key<String> KEY_MODULE_NAME = createKey(CloudModuleUtils.ATTR_MODULE_NAME, Scope.PATH, String.class);
  static final Key<String> KEY_PACKAGE_NAME = WizardConstants.PACKAGE_NAME_KEY;
  static final Key<String> KEY_CLIENT_MODULE_NAME = createKey("myClientModuleName", Scope.PATH, String.class);
  static final Key<File> KEY_TEMPLATE_FILE = createKey("myTemplateFile", Scope.WIZARD, File.class);
  static final Key<TemplateMetadata> KEY_TEMPLATE_METADATA = createKey("myTemplateMetadata", Scope.WIZARD, TemplateMetadata.class);
  private static final String PATH_NAME = "New Cloud Module Path";

  private List<CloudTemplateUtils.TemplateInfo> myTemplates;
  private List<ModuleTemplate> myModuleTemplates;

  @Override
  protected void init() {
    assert getProject() != null;
    addStep(new NewCloudModuleDynamicWizardStep(getProject(), myWizard.getDisposable()));
    if (myState.get(KEY_MODULE_NAME) == null) {
      myState.put(KEY_MODULE_NAME, "");
    }
    if (myState.get(KEY_PACKAGE_NAME) == null) {
      myState.put(KEY_PACKAGE_NAME, "");
    }
    myTemplates = CloudTemplateUtils.getTemplates();
  }

  @NotNull
  @Override
  public String getPathName() {
    return PATH_NAME;
  }

  @NotNull
  @Override
  public Iterable<ModuleTemplate> getModuleTemplates() {
    myModuleTemplates = Lists.newArrayList();
    for (CloudTemplateUtils.TemplateInfo template : myTemplates) {
      myModuleTemplates.add(new NewCloudModuleTemplate(template));
    }
    return myModuleTemplates;
  }

  @Override
  public boolean isPathVisible() {
    final ModuleTemplate moduleTemplate = myWizard.getState().get(WizardConstants.SELECTED_MODULE_TYPE_KEY);
    return moduleTemplate != null && myModuleTemplates != null && myModuleTemplates.contains(moduleTemplate);
  }

  @Override
  public boolean performFinishingActions() {
    final File templateFile = myState.get(KEY_TEMPLATE_FILE);
    final String newModuleName = myState.get(KEY_MODULE_NAME);
    final String packageName = myState.get(KEY_PACKAGE_NAME);
    final String clientModuleName = myState.get(KEY_CLIENT_MODULE_NAME);
    assert getProject() != null && templateFile != null && newModuleName != null && packageName != null;
    CloudModuleUtils.createModule(getProject(), templateFile, newModuleName, packageName, clientModuleName);
    return true;
  }

  /**
   * Each instance of this class represents an item that can be selected in the first page of the
   * New Module wizard.
   */
  private static class NewCloudModuleTemplate implements ModuleTemplate {
    private final String myName;
    private final String myDescription;
    private final File myFile;
    private final TemplateMetadata myMetadata;

    public NewCloudModuleTemplate(@NotNull CloudTemplateUtils.TemplateInfo templateInfo) {
      myName = templateInfo.getMetadata().getTitle();
      myDescription = templateInfo.getMetadata().getDescription();
      myFile = templateInfo.getFile();
      myMetadata = templateInfo.getMetadata();
    }

    @Override
    public String getName() {
      return myName;
    }

    @Nullable
    @Override
    public String getDescription() {
      return myDescription;
    }

    @Nullable
    @Override
    public Icon getIcon() {
      return null;
    }

    @Override
    public void updateWizardStateOnSelection(ScopedStateStore state) {
      state.put(KEY_TEMPLATE_FILE, myFile);
      state.put(KEY_TEMPLATE_METADATA, myMetadata);
    }

    @Override
    public boolean isGalleryModuleType() {
      return false;
    }

    @Nullable
    @Override
    public FormFactorUtils.FormFactor getFormFactor() {
      return null;
    }
  }
}
