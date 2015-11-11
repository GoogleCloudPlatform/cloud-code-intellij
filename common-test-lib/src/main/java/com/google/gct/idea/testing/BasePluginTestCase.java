package com.google.gct.idea.testing;
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.junit.After;
import org.junit.Before;
import org.picocontainer.MutablePicoContainer;

/**
 * Test base class that provides a mock Intellij application and project.
 */
public class BasePluginTestCase {

  protected Project project;
  protected MutablePicoContainer applicationContainer;

  @Before
  public final void setup() {
    TestUtils.createMockApplication();
    applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();
    project = TestUtils.mockProject(applicationContainer);
  }

  /**
   * Register your mock implementations here before executing your test cases.
   */
  public <T> void register(Class<T> clazz, T instance) {
    applicationContainer.registerComponentInstance(clazz.getName(), instance);
  }

  @After
  public final void tearDown() {
    TestUtils.disposeMockApplication();
  }

  public final Project getProject() {
    return project;
  }
}
