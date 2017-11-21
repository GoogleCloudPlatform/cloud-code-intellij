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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import static com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType.JAR;
import static com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType.UNKNOWN;
import static com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType.WAR;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link StagedArtifactNameLegacySupport}. */
@RunWith(JUnit4.class)
public final class StagedArtifactNameLegacySupportTest {

  @Test
  public void getTargetName_withJarArtifactType_doesReturnTargetJar() {
    assertThat(StagedArtifactNameLegacySupport.getTargetName(JAR)).isEqualTo("target.jar");
  }

  @Test
  public void getTargetName_withWarArtifactType_doesReturnTargetWar() {
    assertThat(StagedArtifactNameLegacySupport.getTargetName(WAR)).isEqualTo("target.war");
  }

  @Test
  public void getTargetName_withUnknownArtifactType_doesReturnTarget() {
    assertThat(StagedArtifactNameLegacySupport.getTargetName(UNKNOWN)).isEqualTo("target");
  }
}
