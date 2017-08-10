/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.testing;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.DefaultPluginDescriptor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.TestRunnerUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.picocontainer.MutablePicoContainer;

/**
 * Test base class that provides a mock Intellij application and project.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BasePluginTestCase {

  protected Project project;
  protected MutablePicoContainer applicationContainer;

  private ExtensionsAreaImpl extensionsArea;

  /**
   * Sets up the container.
   */
  @Before
  public final void setup() {
    // prevent memory leak error
    TestRunnerUtil.replaceIdeEventQueueSafely();

    Disposable disposableParent = TestUtils.createMockApplication();
    applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();
    project = TestUtils.mockProject(applicationContainer);
    Extensions.cleanRootArea(disposableParent);
    extensionsArea = (ExtensionsAreaImpl) Extensions.getRootArea();
  }

  /**
   * Register your mock implementations here before executing your test cases.
   */
  protected <T> void registerService(Class<T> clazz, T instance) {
    applicationContainer.registerComponentInstance(clazz.getName(), instance);
  }

  /**
   * Register your extension points for test here.
   */
  protected <N, T extends N> ExtensionPointImpl<T> registerExtensionPoint(
      @NotNull ExtensionPointName<N> name,
      @NotNull Class<T> type) {
    extensionsArea.registerExtensionPoint(
        name.getName(),
        type.getName(),
        new DefaultPluginDescriptor(PluginId.getId(type.getName()), type.getClassLoader()));
    return extensionsArea.getExtensionPoint(name.getName());
  }

  @After
  public final void tearDown() {
    TestUtils.disposeMockApplication();
  }

  public final Project getProject() {
    return project;
  }
}
