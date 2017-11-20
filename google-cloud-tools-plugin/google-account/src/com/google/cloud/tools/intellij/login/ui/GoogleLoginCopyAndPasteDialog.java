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

package com.google.cloud.tools.intellij.login.ui;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dialog to get the verification code from the user. This dialog will provide the users with an
 * authentication URL to navigate to and a text box to paste the token they get when they log into
 * Google via an external browser.
 */
// TODO: set a maximum size for the dialog
public class GoogleLoginCopyAndPasteDialog extends DialogWrapper {

  private static final String TITLE = AccountMessageBundle.message("login.copyandpaste.title.text");
  private static final String SUB_TITLE_1 = AccountMessageBundle
      .message("login.copyandpaste.subtitle.1.text");
  private static final String SUB_TITLE_2 = AccountMessageBundle
      .message("login.copyandpaste.subtitle.2.text");
  private static final String ERROR_MESSAGE = AccountMessageBundle
      .message("login.copyandpaste.error.message.text");

  private String verificationCode = "";
  private String urlString;
  private JTextField codeTextField;

  /**
   * Initialize the verification code dialog.
   */
  public GoogleLoginCopyAndPasteDialog(GoogleAuthorizationCodeRequestUrl requestUrl,
      String message) {
    super(true);
    urlString = requestUrl.build();

    if (message != null) {
      setTitle(message);
    } else {
      setTitle(TITLE);
    }

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    // Add the instructions
    mainPanel.add(Box.createVerticalStrut(5));
    mainPanel.add(new JLabel(SUB_TITLE_1));
    mainPanel.add(new JLabel(SUB_TITLE_2));

    // Add the url label and the url text
    JPanel urlPanel = new JPanel(new BorderLayout());
    urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));

    JLabel urlLabel = new JLabel(
        AccountMessageBundle.message("login.copyandpaste.login.url.label.text"));
    JTextField urlTextField = createUrlText();

    // Add the verification code label and text box
    JPanel codePanel = new JPanel();
    codePanel.setLayout(new BoxLayout(codePanel, BoxLayout.LINE_AXIS));

    createCodeText();

    urlLabel.setLabelFor(urlTextField);
    urlPanel.add(urlLabel);
    urlPanel.add(urlTextField);

    JLabel codeLabel = new JLabel(
        AccountMessageBundle.message("login.copyandpaste.verification.code.label.text"));
    codeLabel.setLabelFor(codeTextField);
    codePanel.add(codeLabel);
    codePanel.add(codeTextField);

    // Add to main panel
    mainPanel.add(Box.createVerticalStrut(10));
    mainPanel.add(urlPanel);
    mainPanel.add(Box.createVerticalStrut(5));
    mainPanel.add(codePanel);
    mainPanel.add(Box.createVerticalStrut(5));

    return mainPanel;
  }

  @Override
  @NotNull
  protected Action getOKAction() {
    myOKAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
        verificationCode = codeTextField.getText();
      }
    };
    return myOKAction;
  }

  @Override
  @Nullable
  protected ValidationInfo doValidate() {
    if (codeTextField.getText().isEmpty()) {
      return new ValidationInfo(ERROR_MESSAGE, codeTextField);
    }
    return null;
  }


  @NotNull
  public String getVerificationCode() {
    return verificationCode;
  }


  private JTextField createUrlText() {
    final JTextField urlTextField = new JTextField(urlString);
    urlTextField.setBorder(null);
    urlTextField.setEditable(false);
    urlTextField.setBackground(UIUtil.getLabelBackground());

    // Add context menu to Url String
    JPopupMenu popup = new JPopupMenu();
    urlTextField.add(popup);
    urlTextField.setComponentPopupMenu(popup);

    JMenuItem copyMenu = new JMenuItem(
        AccountMessageBundle.message("login.copyandpaste.url.copy.text"));
    copyMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        urlTextField.copy();
      }
    });

    popup.add(copyMenu);
    return urlTextField;
  }


  private void createCodeText() {
    codeTextField = new JTextField();

    // Add context menu to Url String
    JPopupMenu popup = new JPopupMenu();
    codeTextField.add(popup);
    codeTextField.setComponentPopupMenu(popup);

    JMenuItem copyMenu = new JMenuItem(
        AccountMessageBundle.message("login.copyandpaste.url.paste.text"));
    copyMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        codeTextField.paste();
      }
    });

    popup.add(copyMenu);
  }

}
