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
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    // no yaml
    assertFalse(editor.getDockerfilePanel().isVisible());
    // java yaml
    editor.getAppYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfilePanel().isVisible());
    // custom yaml
    editor.getAppYaml().setText(customYaml.getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
  }

  public void testValidateConfiguration_noYAML() {
    facetConfiguration.setAppYamlPath("");
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getAppYamlErrorMessage().getText());
    assertFalse(editor.getDockerfilePanel().isVisible());

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
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getAppYamlErrorMessage().getText());
    assertFalse(editor.getDockerfilePanel().isVisible());

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
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(invalidYaml.getPath());

    try {
      editor.apply();
      fail("YAML is malformed.");
    } catch (ConfigurationException ce) {
      assertEquals("The selected app.yaml file is malformed.",
          ce.getMessage());
    }

    assertFalse(editor.getDockerfilePanel().isVisible());
  }

  public void testValidateConfiguration_YAMLIsDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(javaYaml.getParentFile().getPath());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals("The specified app.yaml configuration file does not exist or is not a valid file.",
        editor.getAppYamlErrorMessage().getText());

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
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getAppYamlErrorMessage().isVisible());
    editor.apply();
  }

  public void testValidateConfiguration_customRuntimeNoDockerDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerDirectory().setText("");
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals(
        "The specified Docker directory does not exist or it is not a valid directory.",
        editor.getAppYamlErrorMessage().getText());

    try {
      editor.apply();
      fail("Can't have runtime custom and no dockerfile.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Docker directory does not exist or it is not a valid directory.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntimeNullDockerDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(null);
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals(
        "The specified Docker directory does not exist or it is not a valid directory.",
        editor.getAppYamlErrorMessage().getText());

    try {
      editor.apply();
      fail("Can't have runtime custom and no dockerfile.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Docker directory does not exist or it is not a valid directory.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntimeDockerDirectoryIsDirectory()
      throws ConfigurationException {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(dockerfile.getParentFile().getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getAppYamlErrorMessage().isVisible());
    editor.apply();
  }

  public void testValidateConfiguration_customRuntimeDockerDirectoryIsFile() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(dockerfile.getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getErrorIcon().isVisible());
    assertTrue(editor.getAppYamlErrorMessage().isVisible());
    assertEquals("The specified Docker directory does not exist or it is not a valid "
            + "directory.", editor.getAppYamlErrorMessage().getText());

    try {
      editor.apply();
      fail("Docker directory can't be a file.");
    } catch (ConfigurationException ce) {
      assertEquals(
          "The specified Docker directory does not exist or it is not a valid directory.",
          ce.getMessage());
    }
  }

  public void testValidateConfiguration_customRuntime() throws ConfigurationException {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYaml().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(dockerfile.getParentFile().getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getErrorIcon().isVisible());
    assertFalse(editor.getAppYamlErrorMessage().isVisible());
    editor.apply();
  }

  public void testDefaultDockerfileType() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    assertTrue(editor.getWarRadioButton().isSelected());
    assertFalse(editor.getJarRadioButton().isSelected());
  }
}
