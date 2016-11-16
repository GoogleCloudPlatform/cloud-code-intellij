/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.jps.it;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.intellij.jps.GenRepoInfoFileModuleBuilder;
import com.google.cloud.tools.intellij.jps.model.JpsStackdriverModuleExtension;
import com.google.cloud.tools.intellij.jps.model.impl.JpsStackdriverModuleExtensionImpl;
import com.google.cloud.tools.intellij.jps.model.impl.StackdriverProperties;

import org.jetbrains.jps.builders.JpsBuildTestCase;
import org.jetbrains.jps.model.module.JpsModule;

/**
 * Stackdriver source context generation integration test.
 *
 * <p>Requires Cloud SDK CLI (i.e., gcloud) to be installed.
 */
public class GenRepoInfoFileModuleBuilderIntegrationTest extends JpsBuildTestCase {

  private JpsModule module1;
  private JpsModule module2;
  private JpsModule module3;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Start module with 3 projects
    createFile("src/A.java", "class A{}");
    module1 = addStackdriverModule("module1", true, false);
    module2 = addStackdriverModule("module2", true, true);
    module3 = addStackdriverModule("module3", false, true);
  }

  public void testGenRepoInfoFile() {
    // Build the project
    makeAll();

    // Assert there are 2 sets of source context files.
    assertCompiled(GenRepoInfoFileModuleBuilder.NAME, "out/production/module1/source-context.json");
    assertCompiled(GenRepoInfoFileModuleBuilder.NAME, "out/production/module2/source-context.json");
  }

  private JpsModule addStackdriverModule(String name, boolean genSourceContext,
      boolean ignoreErrors) {
    JpsModule newModule = addModule(name, this.getOrCreateProjectDir().toString());
    StackdriverProperties properties = new StackdriverProperties(genSourceContext, ignoreErrors,
        new CloudSdk.Builder().build().getSdkPath().toString(),
        this.getOrCreateProjectDir().toString());
    JpsStackdriverModuleExtension extension = new JpsStackdriverModuleExtensionImpl(properties);
    newModule.getContainer().setChild(JpsStackdriverModuleExtensionImpl.ROLE, extension);
    return newModule;
  }
}
