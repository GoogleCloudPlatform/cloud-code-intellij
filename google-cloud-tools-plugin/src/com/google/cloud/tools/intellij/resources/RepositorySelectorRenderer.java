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

import com.intellij.ui.JBColor;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * TreeCellRenderer for nodes in the repository selector UI widget.
 */
public class RepositorySelectorRenderer extends DefaultTreeCellRenderer {

  private RepositoryItem repositoryItem;
  private ResourceSelectorLoadingItem loadingItem;
  private ResourceSelectorEmptyItem emptyItem;
  private ResourceSelectorErrorItem errorItem;

  public RepositorySelectorRenderer() {
    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    repositoryItem = new RepositoryItem();
    loadingItem = new ResourceSelectorLoadingItem(defaultRenderer.getBackgroundNonSelectionColor(),
        defaultRenderer.getTextNonSelectionColor());
    emptyItem = new ResourceSelectorEmptyItem();
    errorItem = new ResourceSelectorErrorItem(JBColor.RED);
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
      boolean expanded, boolean leaf, int row, boolean hasFocus) {
    if (value instanceof RepositoryModelItem) {
      repositoryItem.setText(((RepositoryModelItem) value).getRepositoryId());
      return repositoryItem;
    } else if (value instanceof ResourceLoadingModelItem) {
      loadingItem.snap();
      return loadingItem;
    } else if (value instanceof ResourceEmptyModelItem) {
      emptyItem.setText(((ResourceEmptyModelItem) value).getMessage());
      return emptyItem;
    } else if (value instanceof ResourceErrorModelItem) {
      errorItem.setText(((ResourceErrorModelItem) value).getErrorMessage());
      return errorItem;
    }

    return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
  }

}
