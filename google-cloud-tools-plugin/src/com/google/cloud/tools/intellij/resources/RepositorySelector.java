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
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;

import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * Defines a repository selector UI widget allowing the user to select a repository associated
 * with a given Google Cloud project.
 */
public class RepositorySelector extends CustomizableComboBox implements CustomizableComboBoxPopup {

  private JBPopup popup;
  private RepositoryPanel panel;
  private String cloudProject;
  private CredentialedUser user;
  private boolean canCreateRepository;


  public RepositorySelector(@Nullable String cloudProject,
      @Nullable CredentialedUser user, boolean canCreateRepository) {
    this.cloudProject = cloudProject;
    this.user = user;
    this.canCreateRepository = canCreateRepository;
  }

  public void setCloudProject(String cloudProject) {
    this.cloudProject = cloudProject;
  }

  public void setUser(CredentialedUser user) {
    this.user = user;
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (user != null) {
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
    return 140;
  }

  private class RepositoryPanel extends JPanel {

    private JTree repositoryTree;
    private DefaultMutableTreeNode projectRootNode;
    private DefaultTreeModel treeModel;
    private ProjectRepositoriesModelItem repositories;
    private String selectedRepositoryId;

    RepositoryPanel(String cloudProject) {
      repositories
          = new ProjectRepositoriesModelItem(cloudProject, user);
      projectRootNode = new DefaultMutableTreeNode("root");
      treeModel = new DefaultTreeModel(projectRootNode);

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      repositoryTree = new JTree(treeModel);
      repositoryTree.setRowHeight(0);
      repositoryTree.setRootVisible(false);
      repositoryTree.setOpaque(false);
      repositoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      repositoryTree.expandRow(0);

      RepositorySelectorRenderer renderer = new RepositorySelectorRenderer(repositoryTree);
//      DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
      renderer.setOpenIcon(GoogleCloudToolsIcons.CLOUD);
      renderer.setClosedIcon(GoogleCloudToolsIcons.CLOUD);
      renderer.setLeafIcon(null);
      repositoryTree.setCellRenderer(renderer);

      repositoryTree.addTreeSelectionListener(event -> {
        DefaultMutableTreeNode node
            = (DefaultMutableTreeNode) repositoryTree.getLastSelectedPathComponent();
        if (node != null && node instanceof RepositoryModelItem) {
          RepositoryModelItem repoNode = (RepositoryModelItem) node;
          RepositorySelector.this.setText(repoNode.getRepositoryId());
          selectedRepositoryId = repoNode.getRepositoryId();
          ApplicationManager.getApplication().invokeLater(RepositorySelector.this::hidePopup);
        }
        // todo handle if its instance of loader or error
      });

      JBScrollPane scrollPane = new JBScrollPane();
      scrollPane.setPreferredSize(new Dimension(400, getPreferredPopupHeight()));
      scrollPane.setViewportView(repositoryTree);
      scrollPane.setBorder(BorderFactory.createEmptyBorder());

      add(scrollPane);

      JPanel bottomPane = new JPanel();
      JPanel buttonPanel = new JPanel();

      bottomPane.setLayout(new BorderLayout());
      bottomPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      if (canCreateRepository) {
        JButton newRepositoryButton = new JButton();
        newRepositoryButton.setText("Create a new cloud repository");
        newRepositoryButton.addActionListener(event -> {
          try {
            Desktop.getDesktop().browse(URI.create("http://www.google.com"));
          } catch (IOException e) {
            // todo
          }
        });

        buttonPanel.add(newRepositoryButton);
      }

      JButton refreshButton = new JButton();
      refreshButton.setIcon(GoogleCloudToolsIcons.REFRESH);
      refreshButton.addActionListener(event -> {
        refresh(true);
      });

      buttonPanel.add(Box.createHorizontalGlue());
      buttonPanel.add(refreshButton);
      bottomPane.add(buttonPanel, BorderLayout.PAGE_END);
      add(bottomPane);

      refresh(false);
    }

    private void refresh(boolean empty) {
      setLoader();

      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        repositories.loadRepositories(empty);

        ApplicationManager.getApplication().invokeAndWait(() -> {
          treeModel.insertNodeInto(repositories, projectRootNode, 0);
          treeModel.reload();
          repositoryTree.expandRow(0);
        }, ModalityState.stateForComponent(RepositorySelector.this));
      });
    }

    private void setLoader() {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        repositories.removeAllChildren();
        repositories.add(new ResourceLoadingModelItem());
        treeModel.insertNodeInto(repositories, projectRootNode, 0);
        treeModel.reload();
        repositoryTree.expandRow(0);
      });
    }
  }
}
