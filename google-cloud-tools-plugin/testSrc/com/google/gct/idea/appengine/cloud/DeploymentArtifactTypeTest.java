/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;

/**
 * Test cases for {@link DeploymentArtifactType}.
 */
public class DeploymentArtifactTypeTest {

  @Test
  public void testTypeForPath_jar() throws Exception {
    assertEquals(DeploymentArtifactType.JAR,
        DeploymentArtifactType.typeForPath(new File("/some/path/to/a.jar")));
  }

  @Test
  public void testTypeForPath_war() throws Exception {
    assertEquals(DeploymentArtifactType.WAR,
        DeploymentArtifactType.typeForPath(new File("/some/path/to/a.war")));
  }

  @Test
  public void testTypeForPath_anythingElse() throws Exception {
    assertEquals(DeploymentArtifactType.UNKNOWN,
        DeploymentArtifactType.typeForPath(new File("/some/path/to/a.txt")));
  }
}