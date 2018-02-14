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

package com.google.cloud.tools.intellij.project;

import com.google.cloud.tools.intellij.GoogleCloudCoreMessageBundle;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.google.cloud.tools.intellij.resources.BaseGoogleLoginUi;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;

/** Sign in panel shown in {@link ProjectSelectionDialog} when no users are logged in. */
class ProjectSelectorSignInPanel extends BaseGoogleLoginUi {

  ProjectSelectorSignInPanel() {
    super(GoogleCloudCoreMessageBundle.message("cloud.project.selector.signin.message"));
    init();
  }

  private void init() {
    JButton signInButton = new JButton(new SignInAction());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridy = 2; // below sign in text.
    constraints.anchor = GridBagConstraints.NORTHWEST;
    add(signInButton, constraints);
  }

  private static final class SignInAction extends AbstractAction {
    private SignInAction() {
      putValue(Action.NAME, AccountMessageBundle.message("login.panel.sign.in.button.text"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Services.getLoginService().logIn();
    }
  }
}
