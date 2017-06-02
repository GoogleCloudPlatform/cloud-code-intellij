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

import com.google.common.annotations.VisibleForTesting;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Configuration panel for selecting the type of artifact to be referenced in the Dockerfile.
 */
public class DockerfileArtifactTypePanel {

  private JPanel panel;
  private JRadioButton jarRadioButton;
  private JRadioButton warRadioButton;

  private static final boolean IS_WAR_DOCKERFILE_DEFAULT = true;

  public DockerfileArtifactTypePanel() {
    ButtonGroup dockerfileTypeGroup = new ButtonGroup();
    dockerfileTypeGroup.add(jarRadioButton);
    dockerfileTypeGroup.add(warRadioButton);
    warRadioButton.setSelected(IS_WAR_DOCKERFILE_DEFAULT);
    jarRadioButton.setSelected(!IS_WAR_DOCKERFILE_DEFAULT);
  }

  public AppEngineFlexibleDeploymentArtifactType getArtifactType() {
    return warRadioButton.isSelected()
        ? AppEngineFlexibleDeploymentArtifactType.WAR
        : AppEngineFlexibleDeploymentArtifactType.JAR;
  }

  public JPanel getPanel() {
    return panel;
  }

  @VisibleForTesting
  JRadioButton getJarRadioButton() {
    return jarRadioButton;
  }

  @VisibleForTesting
  JRadioButton getWarRadioButton() {
    return warRadioButton;
  }
}
