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
package com.google.gct.idea.elysium;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * A dialog that prompts the user to select a {@link com.google.gct.login.GoogleLogin}
 * or click "Login Manually" to continue without {@link com.google.gct.login.GoogleLogin} credentials.
 */
public class SelectUserDialog extends DialogWrapper {

  private JPanel rootPanel;
  private UserSelector login;
  private String selectedUser;

  public SelectUserDialog(@Nullable Project project, @NotNull String title) {
    super(project, true);
    init();
    setTitle(title);

    login.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        setOKActionEnabled(login.getSelectedUser() != null);
      }
    });
    setOKButtonText(GctBundle.message("select.user.continue"));
    setCancelButtonText(GctBundle.message("select.user.manuallogin"));
    setOKActionEnabled(false);
    Window window = getWindow();
    if (window != null) {
      window.setPreferredSize(new Dimension(400, 125));
    }
  }

  @NotNull
  public String getSelectedUser() {
    if (!Strings.isNullOrEmpty(selectedUser)) {
      return selectedUser;
    }
    return login.getSelectedUser() != null ? login.getSelectedUser().getEmail() : "";
  }

  @VisibleForTesting
  public void setSelectedUser(@NotNull String selectedUser) {
    this.selectedUser = selectedUser;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return login;
  }

  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }
}
