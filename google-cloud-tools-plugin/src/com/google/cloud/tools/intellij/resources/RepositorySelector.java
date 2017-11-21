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

package com.google.cloud.tools.intellij.resources;

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.ui.CustomizableComboBox;
import com.google.cloud.tools.intellij.ui.CustomizableComboBoxPopup;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a repository selector UI widget allowing the user to select a repository associated with
 * a given Google Cloud project.
 */
public class RepositorySelector extends CustomizableComboBox implements CustomizableComboBoxPopup {

  private static final Logger logger = Logger.getInstance(RepositorySelector.class);
  private static final int SELECTOR_HEIGHT = 140;
  private static final int SELECTOR_WIDTH = 400;
  private static final String PANETHEON_CREATE_REPO_URL_PREFIX =
      "https://console.cloud.google.com/code/develop/repo?project=";
  private JBPopup popup;
  private JPanel panel;
  private ProjectRepositoriesModelItem repositories;
  private String cloudProject;
  private CredentialedUser user;
  private boolean canCreateRepository;

  public RepositorySelector(
      @Nullable String cloudProject, @Nullable CredentialedUser user, boolean canCreateRepository) {
    this.cloudProject = cloudProject;
    this.user = user;
    this.canCreateRepository = canCreateRepository;

    getTextField()
        .getEmptyText()
        .setText(GctBundle.message("cloud.repository.selector.placeholder.text"));
  }

  @Nullable
  public String getSelectedRepository() {
    if (StringUtil.isEmpty(getText()) || repositories == null) {
      return null;
    }

    Enumeration repos = repositories.children();
    while (repos.hasMoreElements()) {
      TreeNode repo = (TreeNode) repos.nextElement();

      if (repo instanceof RepositoryModelItem
          && getText().equals(((RepositoryModelItem) repo).getRepositoryId())) {
        return getText();
      }
    }

    return null;
  }

  public void setCloudProject(String cloudProject) {
    this.cloudProject = cloudProject;
  }

  public void setUser(CredentialedUser user) {
    this.user = user;
  }

  public void loadRepositories() {
    loadRepositories(null /*onComplete*/);
  }

  public void loadRepositories(@Nullable Runnable onComplete) {
    if (user == null || cloudProject == null) {
      return;
    }

    if (repositories == null) {
      repositories = new ProjectRepositoriesModelItem();
    }

    repositories.loadRepositories(cloudProject, user, onComplete);
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (user != null) {
      if (popup == null || popup.isDisposed()) {
        panel = new RepositoryPanel();

        ComponentPopupBuilder popupBuilder =
            JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null);
        popup = popupBuilder.createPopup();
      }
      if (!popup.isVisible()) {
        popup.show(showTarget);
      }
    } else {
      panel = new ProjectNotSelectedPanel();

      ComponentPopupBuilder popupBuilder =
          JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null);
      popup = popupBuilder.createPopup();
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

  /**
   * Returns the width of the repository select popup to show. The popup show have a minimum width
   * and then expand with the repository textfield if the user manually stretches the dialog.
   */
  private int getPopupWidth() {
    int actualWidth = this.getTextField().getWidth();
    return Math.max(SELECTOR_WIDTH, actualWidth);
  }

  @Override
  protected int getPreferredPopupHeight() {
    return SELECTOR_HEIGHT;
  }

  @VisibleForTesting
  public JPanel getPanel() {
    return panel;
  }

  @VisibleForTesting
  public ProjectRepositoriesModelItem getRepositories() {
    return repositories;
  }

  @VisibleForTesting
  public class ProjectNotSelectedPanel extends JPanel {

    private static final int HEIGHT = 30;

    ProjectNotSelectedPanel() {
      setPreferredSize(new Dimension(RepositorySelector.this.getTextField().getWidth(), HEIGHT));
      JLabel warning = new JBLabel();
      warning.setFont(new Font(getFont().getFontName(), Font.ITALIC, getFont().getSize()));
      warning.setText(GctBundle.message("cloud.repository.selector.missing.project.error"));
      add(warning);
    }
  }

  @VisibleForTesting
  public class RepositoryPanel extends JPanel {

    private JTree repositoryTree;
    private DefaultMutableTreeNode projectRootNode;
    private DefaultTreeModel treeModel;

    RepositoryPanel() {
      projectRootNode = new DefaultMutableTreeNode("root");
      treeModel = new DefaultTreeModel(projectRootNode);

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      repositoryTree = new JTree(treeModel);
      repositoryTree.setRowHeight(0);
      repositoryTree.setRootVisible(false);
      repositoryTree.setOpaque(false);
      repositoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      repositoryTree.expandRow(0);

      RepositorySelectorRenderer renderer = new RepositorySelectorRenderer();
      renderer.setOpenIcon(GoogleCloudToolsIcons.CLOUD);
      renderer.setClosedIcon(GoogleCloudToolsIcons.CLOUD);
      renderer.setLeafIcon(null);
      repositoryTree.setCellRenderer(renderer);

      repositoryTree.addTreeSelectionListener(
          event -> {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) repositoryTree.getLastSelectedPathComponent();
            if (node != null && node instanceof RepositoryModelItem) {
              RepositoryModelItem repoNode = (RepositoryModelItem) node;
              RepositorySelector.this.setText(repoNode.getRepositoryId());
              ApplicationManager.getApplication().invokeLater(RepositorySelector.this::hidePopup);
            }
          });

      JBScrollPane scrollPane = new JBScrollPane();
      scrollPane.setPreferredSize(new Dimension(getPopupWidth(), getPreferredPopupHeight()));
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
        newRepositoryButton.setText(GctBundle.message("cloud.repository.selector.create.button"));
        newRepositoryButton.addActionListener(
            event -> {
              try {
                Desktop.getDesktop()
                    .browse(URI.create(PANETHEON_CREATE_REPO_URL_PREFIX + cloudProject));
              } catch (IOException e) {
                logger.error(GctBundle.message("cloud.repository.selector.create.url.error"));
              }
            });

        buttonPanel.add(newRepositoryButton);
      }

      JButton refreshButton = new JButton();
      refreshButton.setIcon(GoogleCloudToolsIcons.REFRESH);
      refreshButton.addActionListener(
          event -> {
            refresh();
          });

      buttonPanel.add(Box.createHorizontalGlue());
      buttonPanel.add(refreshButton);
      bottomPane.add(buttonPanel, BorderLayout.PAGE_END);
      add(bottomPane);

      refresh();
    }

    private void refresh() {
      if (repositories == null) {
        repositories = new ProjectRepositoriesModelItem();
      }

      setLoader();

      loadRepositories(
          () ->
              ApplicationManager.getApplication()
                  .invokeAndWait(
                      () -> {
                        treeModel.insertNodeInto(repositories, projectRootNode, 0);
                        treeModel.reload();
                        repositoryTree.expandRow(0);
                      },
                      ModalityState.stateForComponent(RepositorySelector.this)));
    }

    private void setLoader() {
      ApplicationManager.getApplication()
          .invokeAndWait(
              () -> {
                repositories.removeAllChildren();
                repositories.add(new ResourceLoadingModelItem());
                treeModel.insertNodeInto(repositories, projectRootNode, 0);
                treeModel.reload();
                repositoryTree.expandRow(0);
              });
    }

    @VisibleForTesting
    public DefaultTreeModel getTreeModel() {
      return treeModel;
    }
  }
}
