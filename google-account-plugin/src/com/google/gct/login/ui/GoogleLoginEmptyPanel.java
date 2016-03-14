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
import com.google.gct.login.Services;

import com.intellij.ui.components.JBScrollPane;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * An empty Google Login Panel that displays an option to log in at the bottom.
 */
public class GoogleLoginEmptyPanel extends JPanel {
  private static final String ADD_ACCOUNT = "Add Account";
  private static final String SIGN_IN = "Sign In";
  private JBScrollPane contentScrollPane;
  private JPanel bottomPane;

  public GoogleLoginEmptyPanel() {
    super(new BorderLayout());

    contentScrollPane = new JBScrollPane();
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

    bottomPane = new JPanel();
    buttonPane.add(bottomPane);

    add(contentScrollPane, BorderLayout.CENTER);
    add(buttonPane, BorderLayout.PAGE_END);
  }

  private static boolean needsToSignIn() {
    Map<String, CredentialedUser> users = Services.getLoginService().getAllUsers();
    return users == null || users.isEmpty();
  }

  protected void doLogin() {
    Services.getLoginService().logIn();
  }

  protected JBScrollPane getContentPane() {
    return contentScrollPane;
  }

  protected JPanel getBottomPane() { return bottomPane; }
}
