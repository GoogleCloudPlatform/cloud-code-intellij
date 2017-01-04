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

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.ui.CustomizableComboBox;
import com.google.cloud.tools.intellij.ui.CustomizableComboBoxPopup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;

import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Created by eshaul on 12/14/16.
 */
public class RepositorySelector extends CustomizableComboBox implements CustomizableComboBoxPopup {

  private JBPopup popup;
  private RepositoryPanel panel;
  private String cloudProject;
  private CredentialedUser user;


  public RepositorySelector(@Nullable String cloudProject, @Nullable CredentialedUser user) {
    this.cloudProject = cloudProject;
    this.user = user;
  }

  public void setCloudProject(String cloudProject) {
    this.cloudProject = cloudProject;
  }

  public void setUser(CredentialedUser user) {
    this.user = user;
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (popup == null || popup.isDisposed()) {
      panel = new RepositoryPanel(cloudProject);

      ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(panel, null); // todo change focus param
      popup = popupBuilder.createPopup();
    }
    if (!popup.isVisible()) {
      popup.show(showTarget);
    }
  }

  @Override
  public void hidePopup() {
    if (isPopupVisible()) {
      popup.closeOk(null);
    }
  }

  @Override
  public boolean isPopupVisible() {
    return popup != null && !popup.isDisposed() && popup.isVisible();
  }

  @Override
  protected CustomizableComboBoxPopup getPopup() {
    return this;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return 240;
  }

  private class RepositoryPanel extends JPanel {

    private JTree repositoryTree;
    private DefaultMutableTreeNode projectRootNode;
    private DefaultTreeModel treeModel;

    public RepositoryPanel(String cloudProject) {
      projectRootNode = new DefaultMutableTreeNode("root");
      treeModel = new DefaultTreeModel(projectRootNode);

      ProjectRepositoriesModelItem repositories = new ProjectRepositoriesModelItem(cloudProject, user);
      treeModel.insertNodeInto(repositories, projectRootNode, 0);

      repositoryTree = new JTree(treeModel);
      repositoryTree.setRootVisible(false);
      repositoryTree.setOpaque(false);
      repositoryTree.expandRow(0);

      repositoryTree.addTreeSelectionListener(event -> {
        DefaultMutableTreeNode node
            = (DefaultMutableTreeNode) repositoryTree.getLastSelectedPathComponent();
        if (node != null && node instanceof RepositoryModelItem) {
          RepositoryModelItem repoNode = (RepositoryModelItem) node;
          RepositorySelector.this.setText(repoNode.getRepositoryName());
          ApplicationManager.getApplication().invokeLater(RepositorySelector.this::hidePopup);
        }
      });

      JBScrollPane scrollPane = new JBScrollPane();
      scrollPane.setViewportView(repositoryTree);
      // TODO update this
      scrollPane.setPreferredSize(new Dimension(240, getPreferredPopupHeight()));
      scrollPane.setBorder(BorderFactory.createEmptyBorder());

//          projectRootNode.getChildCount()); // TODO is the count needed?

      this.setPreferredSize(new Dimension(240, getPreferredPopupHeight()));
      add(scrollPane, BorderLayout.CENTER);
    }


  }
}
