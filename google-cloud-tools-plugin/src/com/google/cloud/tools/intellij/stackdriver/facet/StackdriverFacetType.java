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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.compiler.StackdriverBuildManagerListener;
import com.google.cloud.tools.intellij.jps.model.impl.StackdriverProperties;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.compiler.server.BuildManagerListener;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class StackdriverFacetType
    extends FacetType<StackdriverFacet, StackdriverFacetConfiguration> {
  public static final String STRING_ID = "stackdriver";
  public static final FacetTypeId<StackdriverFacet> ID = new FacetTypeId<>(STRING_ID);

  public StackdriverFacetType() {
    super(ID, STRING_ID, GctBundle.getString("stackdriver.name"));
  }

  @Override
  public StackdriverFacetConfiguration createDefaultConfiguration() {
    return new StackdriverFacetConfiguration();
  }

  @Override
  public StackdriverFacet createFacet(@NotNull Module module, String name,
      @NotNull StackdriverFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
    // The following retrieves the Cloud SDK path at build time.
//    module.getProject().getMessageBus().connect()
//        .subscribe(BuildManagerListener.TOPIC, new StackdriverBuildManagerListener());
    CompilerManager.getInstance(module.getProject())
        .addBeforeTask(new GetCloudSdkPathCompileTask());
    return new StackdriverFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.STACKDRIVER;
  }

  class GetCloudSdkPathCompileTask implements CompileTask {

    @Override
    public boolean execute(CompileContext context) {
      for (Module module : ModuleManager.getInstance(context.getProject()).getModules()) {
        StackdriverProperties configuration = FacetManager.getInstance(module)
            .getFacetByType(StackdriverFacetType.ID).getConfiguration().getState();
        configuration.setCloudSdkPath(CloudSdkService.getInstance().getSdkHomePath());
      }
      return true;
    }
  }
}
