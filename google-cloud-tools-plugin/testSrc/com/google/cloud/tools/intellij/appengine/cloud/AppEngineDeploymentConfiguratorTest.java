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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentEditor;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardDeploymentEditor;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.RemoteServer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineDeploymentConfiguratorTest extends BasePluginTestCase {
  @Mock
  Project project;
  @Mock
  AppEngineDeployable deployable;
  @Mock
  RemoteServer<AppEngineServerConfiguration> server;
  @Mock
  AppEngineProjectService projectService;
  @Mock
  ModuleManager moduleManager;
  @Mock
  Module module;
  @Mock
  FacetManager facetManager;

  @Before
  public void setUp() {
    registerService(AppEngineProjectService.class, projectService);

    when(projectService.getServiceNameFromAppEngineWebXml(project, deployable))
        .thenReturn("service");
    when(projectService.getFlexibleRuntimeFromAppYaml(isA(String.class))).thenReturn(
        Optional.of(FlexibleRuntime.JAVA));
    when(project.getComponent(ModuleManager.class)).thenReturn(moduleManager);
    when(moduleManager.getModules()).thenReturn(new Module[]{module});
    when(module.getComponent(FacetManager.class)).thenReturn(facetManager);
    when(facetManager.getFacetByType(AppEngineFlexibleFacetType.ID)).thenReturn(null);
  }

  @Test
  public void testCreateEditor_flexible() {
    when(deployable.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);
    AppEngineDeploymentConfigurator configurator = new AppEngineDeploymentConfigurator(project);

    assertTrue(configurator.createEditor(deployable, server) instanceof AppEngineFlexibleDeploymentEditor);
  }

  @Test
  public void testCreateEditor_standard() {
    when(deployable.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);
    AppEngineDeploymentConfigurator configurator = new AppEngineDeploymentConfigurator(project);

    assertTrue(configurator.createEditor(deployable, server) instanceof AppEngineStandardDeploymentEditor);
  }
}