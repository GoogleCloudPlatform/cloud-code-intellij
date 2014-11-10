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
package com.google.gct.idea.git;

import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.login.CredentialedUser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Shows a dialog that has one entry value which is a GCP project using the project selector.
 * The title and ok button text is passed into the constructor.
 */
public class ChooseProjectDialog extends DialogWrapper {

  private JPanel myRootPanel;
  private ProjectSelector myProjectSelector;
  private String myProjectId;
  private CredentialedUser myCredentialedUser;

  public ChooseProjectDialog(@NotNull Project project, @NotNull String title, @NotNull String okText) {
    super(project, true);
    init();
    setTitle(title);
    setOKButtonText(okText);
    setOKActionEnabled(false);
  }

  /**
   * @return the project ID selected by the user.
   */
  @NotNull
  public String getProjectId() {
    return myProjectId;
  }

  /**
   * @return the credentialeduser that owns the ID returned from {@link #getProjectId()}.
   */
  @Nullable
  public CredentialedUser getCredentialedUser() {
    return myCredentialedUser;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "ChooseProjectDialog";
  }

  private void createUIComponents() {
    myProjectSelector = new ProjectSelector();
    myProjectSelector.setMinimumSize(new Dimension(300, 0));
    myProjectSelector.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        setOKActionEnabled(myProjectSelector.getSelectedUser() != null);
      }
    });
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myProjectSelector;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myRootPanel;
  }

  @Override
  protected void doOKAction() {
    myProjectId = myProjectSelector.getText();
    myCredentialedUser = myProjectSelector.getSelectedUser();
    super.doOKAction();
  }

}
