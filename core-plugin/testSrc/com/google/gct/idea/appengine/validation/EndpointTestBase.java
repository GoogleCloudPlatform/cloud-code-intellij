/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.gct.idea.appengine.validation;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import java.io.File;

public abstract class EndpointTestBase extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    String homePath = new File(".").getAbsolutePath();
    String homePathParent = homePath.substring(0, homePath.lastIndexOf('/'));
    return homePathParent + FileUtil.toSystemDependentName("/testData/");
  }
}
