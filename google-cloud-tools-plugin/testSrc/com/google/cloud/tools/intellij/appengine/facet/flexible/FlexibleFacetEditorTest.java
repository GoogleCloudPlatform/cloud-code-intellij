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
    editor.getAppYamlField().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("java", editor.getRuntimePanel().getLabelText());
    // custom yaml
    editor.getAppYamlField().setText(customYaml.getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("custom", editor.getRuntimePanel().getLabelText());
  }

  public void testValidateConfiguration_noYAML() {
    facetConfiguration.setAppYamlPath("");
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    assertTrue(editor.getAppYamlErrorPanel().isVisible());
    assertFalse(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getRuntimePanel().isVisible());
  }

  public void testValidateConfiguration_nullYAML() {
    facetConfiguration.setAppYamlPath(null);
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    assertTrue(editor.getAppYamlErrorPanel().isVisible());
    assertFalse(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getRuntimePanel().isVisible());
  }

  public void testValidateConfiguration_malformedYAML() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(invalidYaml.getPath());
    assertTrue(editor.getAppYamlErrorPanel().isVisible());
    assertFalse(editor.getRuntimePanel().isVisible());
    assertFalse(editor.getDockerfilePanel().isVisible());
  }

  public void testValidateConfiguration_YAMLIsDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(javaYaml.getParentFile().getPath());
    assertTrue(editor.getAppYamlErrorPanel().isVisible());
    assertFalse(editor.getRuntimePanel().isVisible());
  }

  public void testValidateConfiguration_javaRuntime() throws ConfigurationException {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(javaYaml.getPath());
    assertFalse(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getAppYamlErrorPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("java", editor.getRuntimePanel().getLabelText());
    editor.apply();
  }

  public void testValidateConfiguration_customRuntimeNoDockerDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(customYaml.getPath());
    editor.getDockerDirectory().setText("");
    assertFalse(editor.getAppYamlErrorPanel().isVisible());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getDockerfileErrorPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("custom", editor.getRuntimePanel().getLabelText());
  }

  public void testValidateConfiguration_customRuntimeNullDockerDirectory() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(null);
    assertFalse(editor.getAppYamlErrorPanel().isVisible());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getDockerfileErrorPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("custom", editor.getRuntimePanel().getLabelText());
  }

  public void testValidateConfiguration_customRuntimeDockerDirectoryIsFile() {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(dockerfile.getPath());
    assertFalse(editor.getAppYamlErrorPanel().isVisible());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertTrue(editor.getDockerfileErrorPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("custom", editor.getRuntimePanel().getLabelText());
  }

  public void testValidateConfiguration_customRuntime() throws ConfigurationException {
    FlexibleFacetEditor editor = new FlexibleFacetEditor(facetConfiguration, getModule());
    editor.getAppYamlField().setText(customYaml.getPath());
    editor.getDockerDirectory().setText(dockerfile.getParentFile().getPath());
    assertTrue(editor.getDockerfilePanel().isVisible());
    assertFalse(editor.getAppYamlErrorPanel().isVisible());
    assertFalse(editor.getDockerfileErrorPanel().isVisible());
    editor.apply();
  }
}
