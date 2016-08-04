/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.appengine.converter;

import com.google.cloud.tools.intellij.appengine.converter.BaseConverterTest;

import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.intellij.conversion.ProjectConversionTestUtil;
import com.intellij.conversion.impl.ProjectConversionUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for {@link com.google.cloud.tools.intellij.appengine.converter.AppEngineFacetConverterProvider}
 */
public class AppEngineFacetConverterTest extends BaseConverterTest {

  public void testConvert() throws IOException {
    testConvert("conversion/appEngineFacet");
  }

}
