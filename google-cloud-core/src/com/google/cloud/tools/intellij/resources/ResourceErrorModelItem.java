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

package com.google.cloud.tools.intellij.resources;

import javax.swing.tree.DefaultMutableTreeNode;
import org.jetbrains.annotations.NotNull;

/**
 * This model item represents a node shown when an error occurs attempting to query a resource
 * manager.
 */
public class ResourceErrorModelItem extends DefaultMutableTreeNode {

  private String errorMessage;

  public ResourceErrorModelItem(@NotNull String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
