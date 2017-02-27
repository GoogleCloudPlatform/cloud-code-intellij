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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.PlatformTestCase;

import java.io.File;

public class FlexibleFacetEditorTest extends PlatformTestCase {

  private File javaYaml;
  private File customYaml;
  private File dockerfile;
  private FlexibleFacetEditor editor;
  private AppEngineDeploymentConfiguration deploymentConfiguration;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    javaYaml = createTempFile("java.yaml", "runtime: java");
    customYaml = createTempFile("custom.yaml", "runtime: custom");
    dockerfile = createTempFile("Dockerfile", "");

    deploymentConfiguration = new AppEngineDeploymentConfiguration();
  }

  public void testToggleDockerfileSection() throws ConfigurationException {
    deploymentConfiguration.setAppYamlPath("");
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    // no yaml
    assertFalse(editor.getDockerfile().isEnabled());
    assertFalse(editor.getGenDockerfileButton().isEnabled());
    assertFalse(editor.getNoDockerfileLabel().isVisible());
    // java yaml
    editor.getAppYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfile().isEnabled());
    assertFalse(editor.getGenDockerfileButton().isEnabled());
    assertTrue(editor.getNoDockerfileLabel().isVisible());
    // custom yaml
    editor.getAppYaml().setText(customYaml.getPath());
    assertTrue(editor.getDockerfile().isEnabled());
    assertTrue(editor.getGenDockerfileButton().isEnabled());
    assertFalse(editor.getNoDockerfileLabel().isVisible());
  }

  public void testValidateConfiguration_noYAML() {
    deploymentConfiguration.setAppYamlPath("");
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());

    try {
      editor.apply();
      fail("app.yaml can't be empty");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified app.yaml configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_nullYAML() {
    deploymentConfiguration.setAppYamlPath(null);
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());

    try {
      editor.apply();
      fail("app.yaml can't be empty");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified app.yaml configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_YAMLIsDirectory() {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(javaYaml.getParentFile().getPath());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());

    try {
      editor.apply();
      fail("app.yaml can't be empty");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified app.yaml configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_javaRuntime() throws ConfigurationException {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfile().isEnabled());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getErrorMessage().isVisible());
    assertTrue(editor.getNoDockerfileLabel().isVisible());
    editor.apply();
  }

  public void testValidateConfiguration_customRuntimeNoDockerfile() {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerfile().setText("");
    assertTrue(editor.getDockerfile().isEnabled());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals(
        "The specified Dockerfile configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());
    assertFalse(editor.getNoDockerfileLabel().isVisible());

    try {
      editor.apply();
      fail("Can't have runtime custom and no dockerfile.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Dockerfile configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntimeNullDockerfile() {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerfile().setText(null);
    assertTrue(editor.getDockerfile().isEnabled());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals(
        "The specified Dockerfile configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());
    assertFalse(editor.getNoDockerfileLabel().isVisible());

    try {
      editor.apply();
      fail("Can't have runtime custom and no dockerfile.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Dockerfile configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntimeDockerfileIsDirectory() {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerfile().setText(dockerfile.getParentFile().getPath());
    assertTrue(editor.getDockerfile().isEnabled());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getErrorMessage().isVisible());
    assertEquals("The specified Dockerfile configuration file does not exist or is not a valid file.",
        editor.getErrorMessage().getText());
    assertFalse(editor.getNoDockerfileLabel().isVisible());

    try {
      editor.apply();
      fail("Dockerfile can't be a directory.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Dockerfile configuration file does not exist or is not a valid file.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntime() throws ConfigurationException {
    editor = new FlexibleFacetEditor(deploymentConfiguration, getProject());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerfile().setText(dockerfile.getPath());
    assertTrue(editor.getDockerfile().isEnabled());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getErrorMessage().isVisible());
    assertFalse(editor.getNoDockerfileLabel().isVisible());
    editor.apply();
  }
}
