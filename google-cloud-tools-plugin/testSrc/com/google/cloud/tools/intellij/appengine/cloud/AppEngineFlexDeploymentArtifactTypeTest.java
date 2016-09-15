/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.file.Paths;

/**
 * Test cases for {@link AppEngineFlexDeploymentArtifactType}.
 */
public class AppEngineFlexDeploymentArtifactTypeTest {

  @Test
  public void testTypeForPath_jar() throws Exception {
    assertEquals(AppEngineFlexDeploymentArtifactType.JAR,
        AppEngineFlexDeploymentArtifactType.typeForPath(Paths.get("some", "path", "to", "a.jar")));
  }

  @Test
  public void testTypeForPath_war() throws Exception {
    assertEquals(AppEngineFlexDeploymentArtifactType.WAR,
        AppEngineFlexDeploymentArtifactType.typeForPath(Paths.get("some", "path", "to", "a.war")));
  }

  @Test
  public void testTypeForPath_anythingElse() throws Exception {
    assertEquals(AppEngineFlexDeploymentArtifactType.UNKNOWN,
        AppEngineFlexDeploymentArtifactType.typeForPath(Paths.get("some", "path", "to", "a.txt")));
  }
}