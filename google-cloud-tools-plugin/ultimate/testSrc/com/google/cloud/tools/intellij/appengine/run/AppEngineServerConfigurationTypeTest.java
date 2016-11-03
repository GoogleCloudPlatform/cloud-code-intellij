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

package com.google.cloud.tools.intellij.appengine.run;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.server.run.AppEngineServerConfigurationType;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestCase;

import org.picocontainer.MutablePicoContainer;

import java.util.Collection;

/**
 * Tests for {@link AppEngineServerConfigurationType}.
 */
public class AppEngineServerConfigurationTypeTest extends PlatformTestCase {
  private AppEngineAssetProvider assetProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    assetProvider = mock(AppEngineAssetProvider.class);

    applicationContainer.unregisterComponent(AppEngineAssetProvider.class.getName());

    applicationContainer.registerComponentInstance(
        AppEngineAssetProvider.class.getName(), assetProvider);
  }

  public void testIsApplicable_notAppEngineStandardApp() {
    when(assetProvider.loadAppEngineStandardWebXml(any(Project.class), any(Collection.class)))
        .thenReturn(null);
    AppEngineServerConfigurationType configurationType
        = AppEngineServerConfigurationType.getInstance();

    ConfigurationFactory factory = configurationType.getConfigurationFactories()[0];
    assertFalse(factory.isApplicable(getProject()));
  }

  public void testIsApplicable_appEngineStandardApp() {
    when(assetProvider.loadAppEngineStandardWebXml(any(Project.class), any(Collection.class)))
        .thenReturn(mock(XmlFile.class));

    AppEngineServerConfigurationType configurationType
        = AppEngineServerConfigurationType.getInstance();

    ConfigurationFactory factory = configurationType.getConfigurationFactories()[0];
    assertTrue(factory.isApplicable(getProject()));
  }
}
