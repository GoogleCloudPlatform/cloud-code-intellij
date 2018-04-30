/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.facet.flexible;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.mock.MockVirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import org.jetbrains.yaml.YAMLFileType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link AppEngineFlexibleFrameworkDetector}. */
public class AppEngineFlexibleFrameworkDetectorTest {

  private static final String VALID_APP_ENGINE_FLEX_YAML_CONTENT = "runtime: java";
  private static final String NOT_APP_ENGINE_FLEX_YAML_CONTENT =
      "spring:\n  application:\n  name: jhipsterSampleApplication";

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private AppEngineFlexibleFrameworkDetector detector;
  private ElementPattern<FileContent> pattern;

  @Before
  public void setUp() {
    detector = new AppEngineFlexibleFrameworkDetector();
    pattern = detector.createSuitableFilePattern();
  }

  @Test
  public void detectorFileType_Yaml() {
    assertThat(detector.getFileType()).isEqualTo(YAMLFileType.YML);
  }

  @Test
  public void createSuitableFilePattern_withInvalidFilename_doesNotMatch() {
    // unaccepted file name pattern, although correct content.
    MockVirtualFile invalidNameFile = new MockVirtualFile("myApp.yaml");
    FileContent wrongFile =
        new FileContentImpl(
            invalidNameFile, VALID_APP_ENGINE_FLEX_YAML_CONTENT, System.currentTimeMillis());

    assertThat(pattern.accepts(wrongFile)).isFalse();
  }

  @Test
  public void createSuitableFilePattern_appYml_withInvalidContent_doesNotMatch() {
    MockVirtualFile invalidYamlFile = new MockVirtualFile("app.yml");
    FileContent invalidYamlFileContent =
        new FileContentImpl(
            invalidYamlFile, NOT_APP_ENGINE_FLEX_YAML_CONTENT, System.currentTimeMillis());

    assertThat(pattern.accepts(invalidYamlFileContent)).isFalse();
  }

  @Test
  public void createSuitableFilePattern_appYaml_withInvalidContent_doesNotMatch() {
    MockVirtualFile invalidYamlFile = new MockVirtualFile("app.yaml");
    FileContent invalidYamlFileContent =
        new FileContentImpl(
            invalidYamlFile, NOT_APP_ENGINE_FLEX_YAML_CONTENT, System.currentTimeMillis());

    assertThat(pattern.accepts(invalidYamlFileContent)).isFalse();
  }

  @Test
  public void createSuitableFilePattern_appYml_withValidContent_matches() {
    MockVirtualFile validAppEngineFlexFile = new MockVirtualFile("app.yml");
    FileContent validAppEngineFlexFileContent =
        new FileContentImpl(
            validAppEngineFlexFile, VALID_APP_ENGINE_FLEX_YAML_CONTENT, System.currentTimeMillis());

    assertThat(pattern.accepts(validAppEngineFlexFileContent)).isTrue();
  }

  @Test
  public void createSuitableFilePattern_appYaml_withValidContent_matches() {
    MockVirtualFile validAppEngineFlexFile = new MockVirtualFile("app.yaml");
    FileContent validAppEngineFlexFileContent =
        new FileContentImpl(
            validAppEngineFlexFile, VALID_APP_ENGINE_FLEX_YAML_CONTENT, System.currentTimeMillis());

    assertThat(pattern.accepts(validAppEngineFlexFileContent)).isTrue();
  }
}
