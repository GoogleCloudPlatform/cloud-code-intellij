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

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import org.jetbrains.yaml.YAMLFileType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests {@link AppEngineFlexibleFrameworkDetector} for valid/invalid App Engine Flex detection. */
public class AppEngineFlexibleFrameworkDetectorTest {

  private final String validAppEngineFlexYamlString =
          AppEngineFlexibleFrameworkDetector.APP_ENGINE_REQUIRED_YAML + " java";

  @Before
  public void setUp() {
    FileTypeRegistry mockFileTypeRegistry = mock(FileTypeRegistry.class);
    // mock file type check routine. getInstance() is static, has to replace it with IJ Getter mock.
    FileTypeRegistry.ourInstanceGetter = () -> mockFileTypeRegistry;
    when(mockFileTypeRegistry.getFileTypeByFile(any(VirtualFile.class))).thenReturn(YAMLFileType.YML);
  }

  @Test
  public void testIncompleteAppYamlDetection() {
    AppEngineFlexibleFrameworkDetector detector = new AppEngineFlexibleFrameworkDetector();
    ElementPattern<FileContent> pattern = detector.createSuitableFilePattern();

    // unaccepted file name pattern, although correct content.
    MockVirtualFile invalidNameFile = new MockVirtualFile("myApp.yaml", validAppEngineFlexYamlString);
    FileContent wrongFile = new FileContentImpl(invalidNameFile, validAppEngineFlexYamlString, System.currentTimeMillis());
    Assert.assertFalse(pattern.accepts(wrongFile));

    // valid name, does not have required YAML contents.
    String notAppEngineFlexYamlString = "spring:\n  application:\n  name: jhipsterSampleApplication";
    for (String validFileName: AppEngineFlexibleFrameworkDetector.APP_ENGINE_FLEX_PROJECT_FILES) {
      MockVirtualFile invalidYamlFile = new MockVirtualFile(validFileName);
      FileContent invalidYamlFileContent =
              new FileContentImpl(invalidYamlFile, notAppEngineFlexYamlString, System.currentTimeMillis());
      Assert.assertFalse(pattern.accepts(invalidYamlFileContent));
    }
  }

  @Test
  public void testValidAppYamlDetection() {
    // test we require YAML files for detection.
    AppEngineFlexibleFrameworkDetector detector = new AppEngineFlexibleFrameworkDetector();
    Assert.assertEquals(YAMLFileType.YML, detector.getFileType());
    ElementPattern<FileContent> pattern = detector.createSuitableFilePattern();

    // check all valid names, with valid app engine line.
    for (String validFileName: AppEngineFlexibleFrameworkDetector.APP_ENGINE_FLEX_PROJECT_FILES) {
      MockVirtualFile validAppEngineFlexFile = new MockVirtualFile(validFileName);
      FileContent validAppEngineFlexFileContent =
              new FileContentImpl(validAppEngineFlexFile, validAppEngineFlexYamlString, System.currentTimeMillis());
      Assert.assertTrue(pattern.accepts(validAppEngineFlexFileContent));
    }
  }

}