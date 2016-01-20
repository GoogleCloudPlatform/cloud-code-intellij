/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger.ui;

import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.model.Debuggee;
import com.google.api.services.clouddebugger.model.ListDebuggeesResponse;
import com.google.common.base.Strings;
import com.google.gct.idea.debugger.CloudDebugProcessState;
import com.google.gct.idea.debugger.CloudDebuggerClient;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.CredentialedUser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.containers.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * This binding between the project and debuggee is refactored out to make it reusable in the future.
 */
class ProjectDebuggeeBinding {
  private static final Logger LOG = Logger.getInstance(ProjectDebuggeeBinding.class);
  private final JComboBox myDebugeeTarget;
  private final ProjectSelector myElysiumProjectId;
  private Debugger myCloudDebuggerClient = null;
  private CredentialedUser myCredentialedUser = null;
  private CloudDebugProcessState myInputState;

  public ProjectDebuggeeBinding(@NotNull ProjectSelector elysiumProjectId, @NotNull JComboBox debugeeTarget) {
    myElysiumProjectId = elysiumProjectId;
    myDebugeeTarget = debugeeTarget;

    myElysiumProjectId.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        refreshDebugTargetList();
      }
    });

    myElysiumProjectId.addModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        refreshDebugTargetList();
      }
    });
  }

  @NotNull
  public CloudDebugProcessState buildResult(Project project) {
    Long number = myElysiumProjectId.getProjectNumber();
    String projectNumberString = number != null ? number.toString() : null;
    DebugTarget selectedItem = (DebugTarget)myDebugeeTarget.getSelectedItem();
    String savedDebuggeeId = selectedItem != null ? selectedItem.getId() : null;
    String savedProjectDescription = myElysiumProjectId.getText();

    return new CloudDebugProcessState(myCredentialedUser != null ? myCredentialedUser.getEmail() : null,
        savedDebuggeeId,
        savedProjectDescription,
        projectNumberString,
        project);
  }

  @Nullable
  public Debugger getCloudDebuggerClient() {
    CredentialedUser credentialedUser = myElysiumProjectId.getSelectedUser();
    if (myCredentialedUser == credentialedUser) {
      return myCloudDebuggerClient;
    }

    myCredentialedUser = credentialedUser;
    myCloudDebuggerClient =
        myCredentialedUser != null ? CloudDebuggerClient.getLongTimeoutClient(myCredentialedUser.getEmail()) : null;

    return myCloudDebuggerClient;
  }

  @Nullable
  public CloudDebugProcessState getInputState() {
    return myInputState;
  }

  public void setInputState(@Nullable CloudDebugProcessState inputState) {
    myInputState = inputState;
    if (myInputState != null) {
      myElysiumProjectId.setText(myInputState.getProjectName());
    }
  }

  /**
   * Refreshes the list of attachable debug targets based on the project selection.
   */
  @SuppressWarnings("unchecked")
  private void refreshDebugTargetList() {
    myDebugeeTarget.removeAllItems();
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        try {
          if (myElysiumProjectId.getProjectNumber() != null && getCloudDebuggerClient() != null) {
              final ListDebuggeesResponse debuggees =
                  getCloudDebuggerClient().debuggees().list()
                      .setProject(myElysiumProjectId.getProjectNumber().toString())
                      .execute();

            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                DebugTarget targetSelection = null;

                if (debuggees == null || debuggees.getDebuggees() == null || debuggees.getDebuggees().isEmpty()) {
                  myDebugeeTarget.setEnabled(false);
                  myDebugeeTarget.addItem(GctBundle.getString("clouddebug.nomodulesfound"));
                }
                else {
                  myDebugeeTarget.setEnabled(true);
                  Map<String, DebugTarget> perModuleCache = new HashMap<String, DebugTarget>();

                  for (Debuggee debuggee : debuggees.getDebuggees()) {
                    DebugTarget item = new DebugTarget(debuggee, myElysiumProjectId.getText());
                    if (!Strings.isNullOrEmpty(item.getModule()) &&
                        !Strings.isNullOrEmpty(item.getVersion())) {
                      //If we already have an existing item for that module+version, compare the minor
                      // versions and only use the latest minor version.
                      String key = String.format("%s:%s", item.getModule(), item.getVersion());
                      DebugTarget existing = perModuleCache.get(key);
                      if (existing != null && existing.getMinorVersion() > item.getMinorVersion()) {
                        continue;
                      }
                      if (existing != null) {
                        myDebugeeTarget.removeItem(existing);
                      }
                      perModuleCache.put(key, item);
                    }
                    if (myInputState != null && !Strings.isNullOrEmpty(myInputState.getDebuggeeId())) {
                      assert myInputState.getDebuggeeId() != null;
                      if (myInputState.getDebuggeeId().equals(item.getId())) {
                        targetSelection = item;
                      }
                    }
                    myDebugeeTarget.addItem(item);
                  }
                }
                if (targetSelection != null) {
                  myDebugeeTarget.setSelectedItem(targetSelection);
                }
              }
            });
          }
        } catch (IOException ex) {
          LOG.error("Error listing debuggees from Cloud Debugger API", ex);
        }
      }
    });
  }
}