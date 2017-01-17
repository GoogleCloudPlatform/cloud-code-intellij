/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.resources;

import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTextField;

import git4idea.repo.GitRepository;

/**
 * UI panel for selecting a remote name for a GCP repository.
 */
public class RepositoryRemotePanel {

  private JTextField remoteNameField;
  private JPanel remotePanel;

  private GitRepository gitRepository;

  private static final String CLOUD_SOURCE_REPO_REMOTE_PREFIX = "cloud-platform-";

  public RepositoryRemotePanel(@Nullable GitRepository gitRepository) {
    this.gitRepository = gitRepository;

    remoteNameField.setEnabled(false);
  }

  public void update(String cloudRepository) {
    if (!StringUtil.isEmpty(cloudRepository)) {
      remoteNameField.setEnabled(true);
      remoteNameField.setText(getRemoteNameSuggestion(cloudRepository));
    } else {
      remoteNameField.setEnabled(false);
      remoteNameField.setText("");
    }
  }

  public JTextField getRemoteNameField() {
    return remoteNameField;
  }

  public String getText() {
    return remoteNameField.getText();
  }

  /**
   * Auto-populates the remote name field with a suggested remote name.
   *
   * If there is no remote named "origin", then this is the default suggestion.
   * Otherwise, it follows the strategy of prefixing the cloud repository name with a GCP namespace.
   */
  private String getRemoteNameSuggestion(String cloudRepository) {
    if (gitRepository != null && hasOriginRemote()) {
      return CLOUD_SOURCE_REPO_REMOTE_PREFIX + cloudRepository;
    } else {
      return "origin";
    }
  }

  private boolean hasOriginRemote() {
    return gitRepository.getRemotes()
        .stream()
        .anyMatch(remote ->
            "origin".equals(remote.getName()));
  }
}
