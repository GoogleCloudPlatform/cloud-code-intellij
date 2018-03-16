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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.GoogleCloudCoreMessageBundle;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IGoogleLoginCompletedCallback;
import com.google.cloud.tools.intellij.login.IntegratedIntellijGoogleLoginService;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginEmptyPanel;
import com.google.cloud.tools.intellij.ui.CustomizableComboBox;
import com.google.cloud.tools.intellij.ui.CustomizableComboBoxPopup;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jetbrains.annotations.Nullable;

/**
 * A custom combobox that allows the user to select a GoogleLogin and also signin/add-account all
 * within a single control.
 */
public class UserSelector extends CustomizableComboBox implements CustomizableComboBoxPopup {

  private static final int PREFERRED_HEIGHT = 240;
  private static final int POPUP_HEIGHTFRAMESIZE = 50;
  private static final int MIN_WIDTH = 450;

  private JBPopup popup;

  public UserSelector() {
    getTextField().setCursor(Cursor.getDefaultCursor());
    getTextField()
        .getEmptyText()
        .setText(GoogleCloudCoreMessageBundle.message("select.user.emptytext"));
  }

  /**
   * Returns the selected credentialed user for the project id represented by getText(). Note that
   * if the ProjectSelector is created with queryOnExpand, this value could be {@code null} even if
   * {@link #getText()} represents a valid project because the user has not expanded the owning
   * {@link IntegratedIntellijGoogleLoginService}.
   */
  @Nullable
  public CredentialedUser getSelectedUser() {
    if (Strings.isNullOrEmpty(getText())) {
      return null;
    }

    for (CredentialedUser user : Services.getLoginService().getAllUsers().values()) {
      if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(getText())) {
        return user;
      }
    }

    return null;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return !needsToSignIn()
        ? PREFERRED_HEIGHT
        : BaseGoogleLoginUi.PREFERRED_HEIGHT + POPUP_HEIGHTFRAMESIZE;
  }

  @Override
  protected CustomizableComboBoxPopup getPopup() {
    return this;
  }

  private static boolean needsToSignIn() {
    Map<String, CredentialedUser> users = Services.getLoginService().getAllUsers();

    return users.isEmpty();
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (popup == null || popup.isDisposed()) {
      PopupPanel popupPanel = new PopupPanel();

      popupPanel.initializeContent(getText());
      ComponentPopupBuilder popup =
          JBPopupFactory.getInstance()
              .createComponentPopupBuilder(popupPanel, popupPanel.getInitialFocus());
      this.popup = popup.createPopup();
    }
    if (!popup.isVisible()) {
      popup.show(showTarget);
    }
  }

  /**
   * The custom popup panel for user selection hosts a listbox of users surrounded with an option to
   * add an account.
   */
  private class PopupPanel extends GoogleLoginEmptyPanel implements ListCellRenderer {

    private JBList jbList;
    private ProjectSelectorCredentialedUser projectSelectorCredentialedUser;
    private UserSelectorGoogleLogin userSelectorGoogleLogin;
    private int hoverIndex = -1;

    public PopupPanel() {
      projectSelectorCredentialedUser = new ProjectSelectorCredentialedUser();
      projectSelectorCredentialedUser.setOpaque(true);
      userSelectorGoogleLogin = new UserSelectorGoogleLogin();
    }

    public JComponent getInitialFocus() {
      return jbList;
    }

    public void initializeContent(@Nullable String selectedItem) {
      DefaultListModel model = new DefaultListModel();
      jbList = new JBList(model);

      jbList.setOpaque(false);
      jbList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      jbList.setCellRenderer(this);
      for (CredentialedUser user : Services.getLoginService().getAllUsers().values()) {
        model.addElement(user);
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(selectedItem)) {
          jbList.setSelectedValue(user, true);
        }
      }

      if (model.getSize() == 0) {
        model.addElement(new EmptyMarker());
      }

      getContentPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      getContentPane().setViewportView(jbList);
      jbList.addListSelectionListener(
          new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
              Object user = jbList.getSelectedValue();
              if (user != null && user instanceof CredentialedUser) {
                UserSelector.this.setText(((CredentialedUser) user).getEmail());
                SwingUtilities.invokeLater(
                    new Runnable() {
                      @Override
                      public void run() {
                        UserSelector.this.hidePopup();
                      }
                    });
              }
            }
          });

      jbList.addMouseMotionListener(
          new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
              Point point = new Point(me.getX(), me.getY());
              int index = jbList.locationToIndex(point);
              if (index != hoverIndex) {
                int oldIndex = hoverIndex;
                hoverIndex = index;
                if (oldIndex >= 0) {
                  jbList.repaint(jbList.getUI().getCellBounds(jbList, oldIndex, oldIndex));
                }
                if (hoverIndex >= 0) {
                  if (jbList.getSelectedIndex() >= 0) {
                    jbList.clearSelection();
                  }
                  jbList.repaint(jbList.getUI().getCellBounds(jbList, hoverIndex, hoverIndex));
                }
              }
            }
          });

      jbList.requestFocusInWindow();
      int preferredWidth = UserSelector.this.getWidth();
      setPreferredSize(
          new Dimension(Math.max(MIN_WIDTH, preferredWidth), getPreferredPopupHeight()));
    }

    @Override
    protected void doLogin() {
      Services.getLoginService()
          .logIn(
              null,
              new IGoogleLoginCompletedCallback() {
                @Override
                public void onLoginCompleted() {
                  SwingUtilities.invokeLater(
                      new Runnable() {
                        @SuppressWarnings("ConstantConditions")
                        // This suppresses a nullref warning for
                        // GoogleLogin.getInstance().getActiveUser().
                        @Override
                        public void run() {
                          if (Services.getLoginService().getActiveUser() != null) {
                            UserSelector.this.setText(
                                Services.getLoginService().getActiveUser().getEmail());
                          }
                        }
                      });
                }
              });
    }

    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (value instanceof EmptyMarker) {
        return userSelectorGoogleLogin;
      }

      CredentialedUser targetUser = (CredentialedUser) value;
      if (targetUser != null) {
        projectSelectorCredentialedUser.initialize(
            targetUser.getPicture(), targetUser.getName(), targetUser.getEmail());
      } else {
        projectSelectorCredentialedUser.initialize(null, "", null);
      }

      if (isSelected || cellHasFocus || index == hoverIndex) {
        projectSelectorCredentialedUser.setBackground(list.getSelectionBackground());
        projectSelectorCredentialedUser.setForeground(list.getSelectionForeground());
      } else {
        projectSelectorCredentialedUser.setBackground(list.getBackground());
        projectSelectorCredentialedUser.setForeground(list.getForeground());
      }

      return projectSelectorCredentialedUser;
    }

    /** This class marks an empty credential list, giving us an indication to show the signin UI. */
    class EmptyMarker {}
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
}
