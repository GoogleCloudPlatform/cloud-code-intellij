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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.PlatformTestCase;

import java.io.File;

public class FlexibleFacetEditorTest extends PlatformTestCase {

  private File javaYaml;
  private File customYaml;
  private File invalidYaml;
  private File dockerfile;
  private FlexibleFacetEditor editor;
  private AppEngineFlexibleFacetConfiguration facetConfiguration;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    javaYaml = createTempFile("java.yaml", "runtime: java");
    customYaml = createTempFile("custom.yaml", "runtime: custom");
    invalidYaml = createTempFile("invalid.yaml", "runtime: custom\nenv_variables:\n  'ASD");
    dockerfile = createTempFile("Dockerfile", "");

    facetConfiguration = new AppEngineFlexibleFacetConfiguration();
  }

  public void testToggleDockerfileSection() throws ConfigurationException {
    facetConfiguration.setAppYamlPath("");
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    facetConfiguration.setAppYamlPath("");
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    facetConfiguration.setAppYamlPath(null);
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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

  public void testValidateConfiguration_malformedYAML() {
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
    editor.getAppYaml().setText(invalidYaml.getPath());

    try {
      editor.apply();
      fail("YAML is malformed.");
    } catch (ConfigurationException ce) {
      assertEquals("The selected app.yaml file is malformed.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_YAMLIsDirectory() {
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
    editor.getAppYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfile().isEnabled());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getErrorMessage().isVisible());
    assertTrue(editor.getNoDockerfileLabel().isVisible());
    editor.apply();
  }

  public void testValidateConfiguration_customRuntimeNoDockerfile() {
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
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
    editor = new FlexibleFacetEditor(facetConfiguration, getProject());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerfile().setText(dockerfile.getPath());
    assertTrue(editor.getDockerfile().isEnabled());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getErrorMessage().isVisible());
    assertFalse(editor.getNoDockerfileLabel().isVisible());
    editor.apply();
  }
}
