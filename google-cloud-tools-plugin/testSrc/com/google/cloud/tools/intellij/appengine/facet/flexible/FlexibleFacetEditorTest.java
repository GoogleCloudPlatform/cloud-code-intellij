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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import java.awt.Color;
import java.io.File;

public class FlexibleFacetEditorTest extends PlatformTestCase {

  private File javaYaml;
  private File customYaml;
  private File dockerfile;
  private FlexibleFacetEditor editor;

  public void setUp() throws Exception {
    super.setUp();

    javaYaml = createTempFile("java.yaml", "runtime: java");
    customYaml = createTempFile("custom.yaml", "runtime: custom");
    dockerfile = createTempFile("Dockerfile", "");

    AppEngineDeploymentConfiguration deploymentConfiguration =
        new AppEngineDeploymentConfiguration();
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
  }

  public void testSetDockerfileVisibility() {
    // no yaml
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfile().isVisible());
    assertFalse(editor.getGenDockerfileButton().isVisible());
    // java yaml
    editor.getYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getGenDockerfileButton().isVisible());
    // custom yaml
    editor.getYaml().setText(customYaml.getPath());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getGenDockerfileButton().isVisible());
  }

  public void testValidateConfiguration() {
    // no yaml
    assertEquals(Color.RED, editor.getYaml().getTextField().getForeground());
    assertTrue(editor.getFilesWarningLabel().isVisible());
    // java yaml
    editor.getYaml().setText(javaYaml.getPath());
    assertEquals(Color.BLACK, editor.getYaml().getTextField().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    // custom yaml, no dockerfile
    editor.getYaml().setText(customYaml.getPath());
    assertEquals(Color.BLACK, editor.getYaml().getTextField().getForeground());
    assertEquals(Color.RED, editor.getDockerfile().getTextField().getForeground());
    assertTrue(editor.getFilesWarningLabel().isVisible());
    // custom yaml, dockerfile is a directory
    editor.getDockerfile().setText(dockerfile.getParentFile().getPath());
    assertEquals(Color.BLACK, editor.getYaml().getTextField().getForeground());
    assertEquals(Color.RED, editor.getDockerfile().getTextField().getForeground());
    assertTrue(editor.getFilesWarningLabel().isVisible());
    // custom yaml, correct dockerfile
    editor.getDockerfile().setText(dockerfile.getPath());
    assertEquals(Color.BLACK, editor.getYaml().getTextField().getForeground());
    assertEquals(Color.BLACK, editor.getDockerfile().getTextField().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
  }
}
