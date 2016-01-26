/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.login.ui;

import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The Google Login Panel that displays the currently logged in users and buttons to
 * add a new user and sign out a logged in user.
 */
public class GoogleLoginUsersPanel extends JPanel implements ListSelectionListener {
  private static final String PLAY_CONSOLE_URL = "https://play.google.com/apps/publish/#ProfilePlace";
  private static final String CLOUD_CONSOLE_URL = "https://console.developers.google.com/";
  private static final String LEARN_MORE_URL = "https://developers.google.com/cloud/devtools/android_studio_templates/";
  private static final String ADD_ACCOUNT = "Add Account";
  private static final String SIGN_IN = "Sign In";
  private static final String SIGN_OUT = "Sign Out";
  private static final int MAX_VISIBLE_ROW_COUNT = 3;

  private JBList myList;
  private DefaultListModel myListModel;
  private JButton mySignOutButton;
  private boolean myValueChanged;
  private boolean myIgnoreSelection;

  public GoogleLoginUsersPanel() {
    super(new BorderLayout());

    int indexToSelect = initializeUsers();
    final UsersListCellRenderer usersListCellRenderer = new UsersListCellRenderer();

    //Create the list that displays the users and put it in a scroll pane.
    myList = new JBList(myListModel) {
      @Override
      public Dimension getPreferredScrollableViewportSize() {
        int numUsers = myListModel.size();
        Dimension superPreferredSize = super.getPreferredScrollableViewportSize();
        if(numUsers <= 1) {
          return superPreferredSize;
        }

        if(GoogleLogin.getInstance().getActiveUser() == null){
          return superPreferredSize;
        } else if(!isActiveUserInVisibleArea()) {
          return superPreferredSize;
        } else {
          // if there is an active user in the visible area
          int usersToShow = numUsers > MAX_VISIBLE_ROW_COUNT ? MAX_VISIBLE_ROW_COUNT : numUsers;
          int scrollHeight = ((usersToShow - 1) *  usersListCellRenderer.getMainPanelHeight())
            + usersListCellRenderer.getActivePanelHeight();
          return new Dimension((int)superPreferredSize.getWidth(), scrollHeight);
        }
      }
    };

    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setSelectedIndex(indexToSelect);
    myList.addListSelectionListener(this);
    myList.setVisibleRowCount(getVisibleRowCount());
    myList.setCellRenderer(usersListCellRenderer);
    JBScrollPane listScrollPane = new JBScrollPane(myList);

    myList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        myList.updateUI();

        if (myListModel.getSize() == 1 && (myListModel.get(0) instanceof NoUsersListItem)) {
          // When there are no users available
          if (usersListCellRenderer.inLearnMoreUrl(mouseEvent.getPoint())) {
            BrowserUtil.browse(LEARN_MORE_URL);
          }
        }
        else {
          // When users are available
          if (!myValueChanged) {
            // Clicking on an already active user
            int index = myList.locationToIndex(mouseEvent.getPoint());
            if (index >= 0) {
              boolean inPlayUrl = usersListCellRenderer.inPlayConsoleUrl(mouseEvent.getPoint(), index);
              if (inPlayUrl) {
                BrowserUtil.browse(PLAY_CONSOLE_URL);
              }
              else {
                boolean inCloudUrl = usersListCellRenderer.inCloudConsoleUrl(mouseEvent.getPoint(), index);
                if (inCloudUrl) {
                  BrowserUtil.browse(CLOUD_CONSOLE_URL);
                }
              }
            }
          }
        }
        myValueChanged = false;
      }
    });

    myList.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseMoved(MouseEvent mouseEvent) {
        // Determine if the user under the cursor is an active user, a non-active user or a non-user
        int index = myList.locationToIndex(mouseEvent.getPoint());
        if (index >= 0) {
          // If current object is the non-user list item, use default cursor
          Object currentObject = myListModel.get(index);
          if (currentObject instanceof NoUsersListItem) {
            if (usersListCellRenderer.inLearnMoreUrl(mouseEvent.getPoint())) {
              myList.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            else {
              myList.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
            return;
          }

          if (((UsersListItem)currentObject).isActiveUser()) {
            // Active user
            boolean inPlayUrl = usersListCellRenderer.inPlayConsoleUrl(mouseEvent.getPoint(), index);
            boolean inCloudUrl = usersListCellRenderer.inCloudConsoleUrl(mouseEvent.getPoint(), index);
            if (inPlayUrl || inCloudUrl) {
              myList.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
              myList.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
          } else {
            // For non-active user
            myList.setCursor(new Cursor(Cursor.HAND_CURSOR));
          }
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
      }
    });

    boolean noUsersAvailable = (myListModel.getSize() == 1) && (myListModel.get(0) instanceof NoUsersListItem);
    JButton addAccountButton = new JButton(noUsersAvailable ? SIGN_IN : ADD_ACCOUNT);
    addAccountButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GoogleLogin.getInstance().logIn();
      }
    });
    addAccountButton.setHorizontalAlignment(SwingConstants.LEFT);

    mySignOutButton = new JButton(SIGN_OUT);
    mySignOutButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GoogleLogin.getInstance().logOut(true);
      }
    });

    if (myList.isSelectionEmpty()) {
      mySignOutButton.setEnabled(false);
    } else {
      // If list contains the NoUsersListItem place holder
      // sign out button should be hidden
      if (noUsersAvailable) {
        mySignOutButton.setVisible(false);
      } else {
        mySignOutButton.setEnabled(true);
      }
    }

    //Create a panel to hold the buttons
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.add(addAccountButton);
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(mySignOutButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    add(listScrollPane, BorderLayout.CENTER);
    add(buttonPane, BorderLayout.PAGE_END);
  }

  //This method is required by ListSelectionListener.
  @Override
  public void valueChanged(ListSelectionEvent e) {
    if(myIgnoreSelection) {
      return;
    }
    myValueChanged = true;
    if (!e.getValueIsAdjusting()) {
      if (myList.getSelectedIndex() == -1) {
        mySignOutButton.setEnabled(false);
      } else {
        mySignOutButton.setEnabled(true);

        // Make newly selected value the active value
        UsersListItem selectedUser = (UsersListItem)myListModel.get(myList.getSelectedIndex());
        if(!selectedUser.isActiveUser()) {
          GoogleLogin.getInstance().setActiveUser(selectedUser.getUserEmail());
        }

        // Change order of elements in the list so that the
        // active user becomes the first element in the list
        myIgnoreSelection = true;
        try {
          myListModel.remove(myList.getSelectedIndex());
          myListModel.add(0, selectedUser);

          // Re-select the active user
          myList.setSelectedIndex(0);
        } finally {
          myIgnoreSelection = false;
        }
      }
    }
  }

  public JBList getList() {
    return myList;
  }

  private int initializeUsers() {
    LinkedHashMap<String, CredentialedUser> allUsers = GoogleLogin.getInstance().getAllUsers();
    myListModel = new DefaultListModel();

    int activeUserIndex = allUsers.size();
    for(CredentialedUser aUser : allUsers.values()) {
      myListModel.addElement(new UsersListItem(aUser));
      if(aUser.isActive()) {
        activeUserIndex = myListModel.getSize() - 1;
      }
    }

    if(myListModel.getSize() == 0) {
      // Add no user panel
      myListModel.addElement(NoUsersListItem.INSTANCE);
    } else if ((activeUserIndex != 0) && (activeUserIndex < myListModel.getSize())) {
      // Change order of elements in the list so that the
      // active user becomes the first element in the list
      UsersListItem activeUser = (UsersListItem)myListModel.remove(activeUserIndex);
      myListModel.add(0, activeUser);
      activeUserIndex = 0;
    }

    return activeUserIndex;
  }

  private int getVisibleRowCount(){
    if (myListModel == null) {
      return 0;
    }

    int size = myListModel.getSize();
    if(size >= MAX_VISIBLE_ROW_COUNT) {
      return MAX_VISIBLE_ROW_COUNT;
    } else if (size == 0) {
      return 3;
    } else {
      return  size;
    }
  }

  private boolean isActiveUserInVisibleArea() {
    int max = myListModel.getSize() < MAX_VISIBLE_ROW_COUNT ?
      myListModel.getSize() : MAX_VISIBLE_ROW_COUNT;

    for(int i = 0; i < max; i++){
      if(((UsersListItem)myListModel.get(i)).isActiveUser()) {
        return true;
      }
    }
    return false;
  }
}
