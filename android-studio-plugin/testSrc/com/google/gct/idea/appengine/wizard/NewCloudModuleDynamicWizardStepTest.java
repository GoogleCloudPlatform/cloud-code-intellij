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

import com.android.tools.idea.wizard.AndroidStudioWizardPath;
import com.android.tools.idea.wizard.ModuleTemplate;
import com.android.tools.idea.wizard.NewModuleWizardDynamic;
import com.android.tools.idea.wizard.WizardConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class NewCloudModuleDynamicWizardStepTest extends AndroidTestCase {

  public void testIsValidModuleName() throws Exception {
    assertTrue(NewCloudModuleDynamicWizardStep.isValidModuleName("app"));
    assertTrue(NewCloudModuleDynamicWizardStep.isValidModuleName("lib"));
    assertFalse(NewCloudModuleDynamicWizardStep.isValidModuleName("123:456"));
    assertFalse(NewCloudModuleDynamicWizardStep.isValidModuleName("$boot"));
    for (String s : WizardConstants.INVALID_WINDOWS_FILENAMES) {
      assertFalse(NewCloudModuleDynamicWizardStep.isValidModuleName(s));
    }
  }

  /**
   * This tests that the Cloud module appears in new module for an existing project.
   * TODO: add step through code (note that finishing would require an appengine sdk download)
   */
  public void testCloudOnAddModule() throws Exception {
    TestableNewModuleWizardDynamic wizard = new TestableNewModuleWizardDynamic(myModule.getProject(), null);
    try {
      wizard.init();
      NewCloudModuleDynamicWizardPath cloudPath = null;
      for (AndroidStudioWizardPath path : wizard.getPaths()) {
        if (path instanceof NewCloudModuleDynamicWizardPath) {
          cloudPath = (NewCloudModuleDynamicWizardPath)path;
          break;
        }
      }
      assertNotNull(cloudPath);
      ModuleTemplate template = cloudPath.myModuleTemplates.get(0);
      assertNotNull(template);
      wizard.getState().put(WizardConstants.SELECTED_MODULE_TYPE_KEY, template);

      assertTrue(wizard.containsStep(NewCloudModuleDynamicWizardStep.STEP_NAME, true));
    }
    finally {
      Disposer.dispose(wizard.getDisposable());
    }
  }

  public void testCloudOnNewProject() throws Exception {
    TestableNewModuleWizardDynamic wizard = new TestableNewModuleWizardDynamic(null, null);
    try {
      wizard.init();
      NewCloudModuleDynamicWizardPath cloudPath = null;
      for (AndroidStudioWizardPath path : wizard.getPaths()) {
        if (path instanceof NewCloudModuleDynamicWizardPath) {
          cloudPath = (NewCloudModuleDynamicWizardPath)path;
          break;
        }
      }
      assertNull(cloudPath);
    }
    finally {
      Disposer.dispose(wizard.getDisposable());
    }
  }

  static class TestableNewModuleWizardDynamic extends NewModuleWizardDynamic {

    public TestableNewModuleWizardDynamic(@Nullable Project project, @Nullable Module module) {
      super(project, module);
    }

    public ArrayList<AndroidStudioWizardPath> getPaths() {
      return myPaths;
    }
  }

}
