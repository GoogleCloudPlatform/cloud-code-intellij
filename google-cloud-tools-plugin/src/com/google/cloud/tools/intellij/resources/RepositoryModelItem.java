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

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by eshaul on 12/29/16.
 */
public class RepositoryModelItem extends DefaultMutableTreeNode {

  private String repositoryName;

  public RepositoryModelItem(String repositoryName) {
    super(repositoryName);
    this.repositoryName = repositoryName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }
}
