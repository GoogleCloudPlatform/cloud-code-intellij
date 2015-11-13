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
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * An empty Google Login Panel that displays an option to log in at the bottom.
 */
public class GoogleLoginEmptyPanel extends JPanel {
  private static final String ADD_ACCOUNT = "Add Account";
  private static final String SIGN_IN = "Sign In";
  private JBScrollPane myContentScrollPane;
  private JPanel myBottomPane;

  public GoogleLoginEmptyPanel() {
    super(new BorderLayout());

    myContentScrollPane = new JBScrollPane();
    JButton addAccountButton = new JButton(needsToSignIn() ? SIGN_IN : ADD_ACCOUNT);
    addAccountButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doLogin();
      }
    });
    addAccountButton.setHorizontalAlignment(SwingConstants.LEFT);

    //Create a panel to hold the buttons
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.add(addAccountButton);
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    myBottomPane = new JPanel();
    buttonPane.add(myBottomPane);

    add(myContentScrollPane, BorderLayout.CENTER);
    add(buttonPane, BorderLayout.PAGE_END);
  }

  private static boolean needsToSignIn() {
    Map<String, CredentialedUser> users = GoogleLogin.getInstance().getAllUsers();
    return users == null || users.isEmpty();
  }

  protected void doLogin() {
    GoogleLogin.getInstance().logIn();
  }

  protected JBScrollPane getContentPane() {
    return myContentScrollPane;
  }

  protected JPanel getBottomPane() { return myBottomPane; }
}
