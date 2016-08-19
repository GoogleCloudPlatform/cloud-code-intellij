/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.gwt;

import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdkService;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.run.GwtDevModeServer;
import com.intellij.javaee.appServerIntegrations.ApplicationServer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import javax.swing.Icon;

/**
 * @author nik
 */
public class AppEngineGwtServer extends GwtDevModeServer {

  public AppEngineGwtServer(@NotNull ApplicationServer server) {
    super("app-engine:" + server.getName(), server.getName());
  }

  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @Override
  public void patchParameters(@NotNull JavaParameters parameters, String originalOutputDir,
      @NotNull GwtFacet gwtFacet) {
    final ParametersList programParameters = parameters.getProgramParametersList();
    programParameters.add("-server");
    programParameters.add("com.google.appengine.tools.development.gwt.AppEngineLauncher");

    final AppEngineSdkService sdkService = AppEngineSdkService.getInstance();
    sdkService.patchJavaParametersForDevServer(parameters.getVMParametersList());

    //actually these jars are added by AppEngine dev server automatically. But they need to be
    // added to classpath before gwt-dev.jar, because otherwise wrong jsp compiler version will be
    // used (see IDEA-63068)
    if (sdkService.getLibraries() != null) {
      for (File jar : ArrayUtil.mergeArrays(sdkService.getLibraries(),
          sdkService.getJspLibraries())) {
        parameters.getClassPath().addFirst(FileUtil.toSystemIndependentName(jar.getAbsolutePath()));
      }
    }

    if (sdkService.getToolsApiJarFile() != null) {
      parameters.getClassPath().add(sdkService.getToolsApiJarFile());
    }
  }
}
