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
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * The Google Login Panel that displays the currently logged in users and buttons to
 * add a new user and sign out a logged in user.
 */
public class GoogleLoginUsersPanel extends JPanel implements ListSelectionListener {
  private JBList list;
  private DefaultListModel listModel;

  private static final int MAX_VISIBLE_ROW_COUNT = 3;
  private static final String addAccountString = "Add Account";
  private static final String signOutString = "Sign Out";
  private JButton signOutButton;
  private JButton addAccountButton;

  public GoogleLoginUsersPanel() {
    super(new BorderLayout());

    int indexToSelect = initializeUsers();

    //Create the list that displays the users and put it in a scroll pane.
    list = new JBList(listModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(indexToSelect);
    list.addListSelectionListener(this);
    list.setVisibleRowCount(getVisibleRowCount());
    list.setCellRenderer(new UsersListCellRenderer());
    JBScrollPane listScrollPane = new JBScrollPane(list);

    addAccountButton = new JButton(addAccountString);
    AddAccountListener addAccountListener = new AddAccountListener();
    addAccountButton.addActionListener(addAccountListener);
    addAccountButton.setHorizontalAlignment(SwingConstants.LEFT);

    signOutButton = new JButton(signOutString);
    signOutButton.addActionListener(new SignOutListener());

    if(list.isSelectionEmpty()) {
      signOutButton.setEnabled(false);
    } else {
      signOutButton.setEnabled(true);
    }

    //Create a panel to hold the buttons
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.add(addAccountButton);
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(signOutButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    add(listScrollPane, BorderLayout.CENTER);
    add(buttonPane, BorderLayout.PAGE_END);
  }

  /**
   * The action listener for {@code signOutButton}
   */
  class SignOutListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      //This method can be called only if there's a valid selection
      int index = list.getSelectedIndex();

      boolean signedOut = GoogleLogin.getInstance().logOut();
      if(signedOut) {
        // remove logged out user
        listModel.remove(index);
        if (listModel.getSize() == 0) {
          signOutButton.setEnabled(false);

        }
      }
    }
  }

  /**
   * The action listener for {@code addAccountButton}
   */
  class AddAccountListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      boolean successful = GoogleLogin.getInstance().logIn();

      if(!successful) {
        return;
      }

      // Add new user/active user
      CredentialedUser activeUser = GoogleLogin.getInstance().getActiveUser();
      if(activeUser == null) {
        return;
      }

      if(alreadyInList(activeUser.getEmail())) {
        int index = listModel.lastIndexOf(activeUser.getEmail());
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
      } else {
        // Add new user
        listModel.add(0, new UsersListItem(activeUser));
        list.setSelectedIndex(0);
        list.ensureIndexIsVisible(0);
      }
    }

    protected boolean alreadyInList(String name) {
      return listModel.contains(name);
    }
  }

  //This method is required by ListSelectionListener.
  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting() == false) {

      if (list.getSelectedIndex() == -1) {
        signOutButton.setEnabled(false);

      } else {
        signOutButton.setEnabled(true);

        // Make newly selected value the active value
        UsersListItem selectedUser = (UsersListItem)listModel.get(list.getSelectedIndex());
        if(!selectedUser.isActiveUser()) {
          GoogleLogin.getInstance().setActiveUser(selectedUser.getUserEmail());
        }
      }
    }
  }

  public JBList getList() {
    return list;
  }

  private int initializeUsers() {
    Map<String, CredentialedUser> allUsers = GoogleLogin.getInstance().getAllUsers();
    listModel = new DefaultListModel();

    int activeUserIndex = allUsers.size();
    for(CredentialedUser aUser : allUsers.values()) {
      listModel.addElement(new UsersListItem(aUser));
      if(aUser.isActive()) {
        activeUserIndex = listModel .getSize() - 1;
      }
    }

    return activeUserIndex;
  }

  private int getVisibleRowCount(){
    if (listModel == null) {
      return 0;
    }

    int size = listModel.getSize();
    if(size >= MAX_VISIBLE_ROW_COUNT) {
      return MAX_VISIBLE_ROW_COUNT;
    } else if (size == 0) {
      return 3;
    } else {
      return  size;
    }
  }
}
