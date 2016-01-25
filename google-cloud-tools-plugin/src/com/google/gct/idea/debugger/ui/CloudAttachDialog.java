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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gct.idea.debugger.CloudDebugProcessState;
import com.google.gct.idea.debugger.ProjectRepositoryState;
import com.google.gct.idea.debugger.ProjectRepositoryValidator;
import com.google.gct.idea.debugger.SyncResult;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.GoogleLogin;
import com.google.gct.stats.UsageTrackerProvider;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import git4idea.actions.BasicAction;
import git4idea.branch.GitBrancher;
import git4idea.commands.GitCommand;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;

/**
 * CloudAttachDialog shows a dialog allowing the user to select a module and debug.
 */
public class CloudAttachDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(CloudAttachDialog.class);

  private final Project project;
  private final ProjectDebuggeeBinding wireup;
  private ProjectRepositoryValidator projectRepositoryValidator;
  private JComboBox moduleSelector;
  private ProjectSelector elysiumProjectSelector; // Project combo box
  private JBLabel infoMessage;
  private String originalBranchName;
  private JPanel panel;
  private CloudDebugProcessState processResultState;
  private GitRepository sourceRepository;
  private String stashMessage = null;
  private SyncResult syncResult;
  private JBCheckBox syncStashCheckbox;
  private JBLabel warningHeader;
  private JBLabel warningMessage;

  public CloudAttachDialog(@NotNull Project project,
                           @VisibleForTesting ProjectDebuggeeBinding wireup) {
    super(project, true);

    this.project = project;
    init();
    initValidation();
    setTitle(GctBundle.getString("clouddebug.attachtitle"));
    setOKButtonText(GctBundle.getString("clouddebug.attach"));
    syncStashCheckbox.setVisible(false);
    syncStashCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (syncStashCheckbox.isVisible()) {
          warningHeader.setVisible(!syncStashCheckbox.isSelected());
          warningMessage.setVisible(!syncStashCheckbox.isSelected());
          infoMessage.setVisible(syncStashCheckbox.isSelected());
          if (syncStashCheckbox.isSelected()) {
            setOKButtonText(isContinued()
                            ? GctBundle.getString("clouddebug.continuesession")
                            : GctBundle.getString("clouddebug.attach"));
          }
          else {
            setOKButtonText(isContinued()
                            ? GctBundle.getString("clouddebug.continueanyway")
                            : GctBundle.getString("clouddebug.attach.anyway"));
          }
        }
      }
    });

    warningHeader.setVisible(false);
    warningHeader.setFont(new Font(warningHeader.getFont().getName(), Font.BOLD,
                                    warningHeader.getFont().getSize() - 1));
    warningHeader.setForeground(JBColor.RED);
    warningMessage.setVisible(false);
    warningMessage.setFont(new Font(warningMessage.getFont().getName(), Font.PLAIN,
                                     warningHeader.getFont().getSize() - 1));
    warningMessage.setText(GctBundle.getString("clouddebug.sourcedoesnotmatch"));

    infoMessage.setFont(new Font(warningMessage.getFont().getName(), Font.PLAIN,
                                 warningHeader.getFont().getSize() - 1));
    Border paddingBorder = BorderFactory.createEmptyBorder(2, 0, 2, 0);
    infoMessage.setBorder(paddingBorder);

    Window myWindow = getWindow();
    if (myWindow != null) {
      myWindow.setPreferredSize(new Dimension(355, 175));
    }
    BasicAction.saveAll();

    this.wireup = wireup == null ?
        new ProjectDebuggeeBinding(elysiumProjectSelector, moduleSelector, getOKAction()) : wireup;
    moduleSelector.setEnabled(false);
    moduleSelector.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(moduleSelector.isEnabled()) {
          buildResult();
          checkSyncStashState();
        }
        else {
          warningHeader.setVisible(false);
          warningMessage.setVisible(false);
        }
      }
    });

    setOKActionEnabled(isContinued() || doValidate() == null);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  @Override
  protected void doOKAction() {
    if (getOKAction().isEnabled()) {
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "start.session", null);
      // TODO : add source context tracking info
      if (syncStashCheckbox.isSelected()) {
        syncOrStash();
      }
      else {
        buildResult();
        close(OK_EXIT_CODE);  // We close before kicking off the update so it doesn't interfere with
        // the output window coming to focus.
      }
    }
  }

  @Override
  protected ValidationInfo doValidate() {
    // These should not normally occur.
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      return new ValidationInfo(GctBundle.getString("clouddebug.nologin"));
    }

    if (Strings.isNullOrEmpty(elysiumProjectSelector.getText())) {
      return new ValidationInfo(GctBundle.getString("clouddebug.noprojectid"),
          elysiumProjectSelector);
    }

    if (!moduleSelector.isEnabled() && moduleSelector.getItemCount() > 0) {
      return new ValidationInfo(GctBundle.getString("clouddebug.selectvalidproject"),
          elysiumProjectSelector);
    }

    if (moduleSelector.getSelectedItem() == null && moduleSelector.getItemCount() > 0) {
      return new ValidationInfo(GctBundle.getString("clouddebug.selectvalidproject"),
          moduleSelector);
    }

    return null;
  }

  @Nullable
  public CloudDebugProcessState getResultState() {
    return processResultState;
  }

  public void setInputState(@Nullable CloudDebugProcessState inputState) {
    wireup.setInputState(inputState);
  }

  @VisibleForTesting
  ProjectSelector getElysiumProjectSelector() {
    return elysiumProjectSelector;
  }

  @VisibleForTesting
  JComboBox getModuleSelector() {
    return moduleSelector;
  }

  @VisibleForTesting
  JLabel getWarningHeader() {
    return warningHeader;
  }

  @VisibleForTesting
  JLabel getWarningMessage() {
    return warningMessage;
  }

  @VisibleForTesting
  void setProjectRepositoryValidator(ProjectRepositoryValidator projectRepositoryValidator) {
    this.projectRepositoryValidator = projectRepositoryValidator;
  }

  private void buildResult() {
    processResultState = wireup.buildResult(project);
    ProjectRepositoryState repositoryState = ProjectRepositoryState.fromProcessState(
        processResultState);
    repositoryState.setStashMessage(stashMessage);
    repositoryState.setSourceRepository(sourceRepository);
    repositoryState.setOriginalBranchName(originalBranchName);
  }

  /**
   * Checks whether a stash or sync is needed based on the chosen target and local state.
   */
  private void checkSyncStashState() {
    if (processResultState == null) {
      LOG.error("unexpected result state during a check sync stash state");
      return;
    }

    syncResult = projectRepositoryValidator == null ?
        new ProjectRepositoryValidator(processResultState).checkSyncStashState() :
          projectRepositoryValidator.checkSyncStashState();

    if (syncResult.needsStash() && syncResult.needsSync()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      syncStashCheckbox.setVisible(true);
      assert syncResult.getTargetSyncSHA() != null;
      syncStashCheckbox.setText(
          GctBundle.getString("clouddebug.stash.local.changes.and.sync", syncResult.getTargetSyncSHA().substring(0, 7)));
      syncStashCheckbox.setSelected(true);
      warningHeader.setVisible(false);
      warningMessage.setVisible(false);
      infoMessage.setVisible(true);
    }
    else if (syncResult.needsStash()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      syncStashCheckbox.setVisible(true);
      syncStashCheckbox.setText(GctBundle.getString("clouddebug.stashbuttontext"));
      syncStashCheckbox.setSelected(true);
      warningHeader.setVisible(false);
      warningMessage.setVisible(false);
      infoMessage.setVisible(true);
    }
    else if (syncResult.needsSync() && syncResult.getTargetSyncSHA() == null) {
      setOKButtonText(isContinued()
              ? GctBundle.getString("clouddebug.continueanyway")
              : GctBundle.getString("clouddebug.attach.anyway"));
      warningHeader.setVisible(true);
      warningMessage.setVisible(true);
      infoMessage.setVisible(true);
      warningMessage.setText(GctBundle.getString("clouddebug.no.matching.sha"));
    }
    else if (syncResult.needsSync()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      syncStashCheckbox.setVisible(true);
      assert syncResult.getTargetSyncSHA() != null;
      syncStashCheckbox.setText("Sync to " + syncResult.getTargetSyncSHA().substring(0, 7));
      syncStashCheckbox.setSelected(true);
      warningHeader.setVisible(false);
      warningMessage.setVisible(false);
      infoMessage.setVisible(true);
    }
    else if (!syncResult.hasRemoteRepository()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continueanyway")
          : GctBundle.getString("clouddebug.attach.anyway"));
      warningHeader.setVisible(true);
      warningMessage.setVisible(true);
      infoMessage.setVisible(true);
      if (syncResult.getRepositoryType() != null) {
        warningMessage.setText(GctBundle.getString("clouddebug.repositories.are.not.supported",
            syncResult.getRepositoryType()));
      }
      else {
        warningMessage.setText(GctBundle.getString("clouddebug.no.remote.repository"));
      }
    }
    else {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      syncStashCheckbox.setVisible(false);
      warningHeader.setVisible(false);
      warningMessage.setVisible(false);
      infoMessage.setVisible(true);
    }
  }

  private boolean isContinued() {
    CloudDebugProcessState state = wireup.getInputState();
    return state != null && state.getCurrentServerBreakpointList().size() > 0;
  }

  private void refreshAndClose() {
    buildResult();
    close(OK_EXIT_CODE);
  }

  private boolean stash() {
    if (!syncResult.hasLocalRepository()) {
      LOG.error("unexpected null local repro in call to stash");
      return false;
    }

    final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    if (changeListManager.isFreezedWithNotification("Can not stash changes now")) return false;

    final GitLineHandler handler = new GitLineHandler(project, sourceRepository.getRoot(), GitCommand.STASH);
    handler.addParameters("save");
    handler.addParameters("--keep-index");
    String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
    stashMessage = "Cloud Debugger saved changes from branch " + originalBranchName + " at " + date;
    handler.addParameters(stashMessage);
    AccessToken token = DvcsUtil.workingTreeChangeStarted(project);
    try {
      GitHandlerUtil.doSynchronously(handler, GitBundle.getString("stashing.title"), handler.printableCommandLine());
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(project, token);
    }
    return true;
  }

  /**
   * Performs the actual sync/stash needed before attaching.
   */
  private void syncOrStash() {
    // When the user edits a document in intelliJ, there are spurious updates to the timestamp of
    // the document for an unspecified amount of time (even though there are no real edits).
    // So, we save-all right before we stash to (help) ensure we don't get a conflict dialog.
    // The conflict dialog happens when the timestamps of the document and file are mismatched.
    // So when we do the git operations, we want the document and file timestamps to match exactly.
    BasicAction.saveAll();

    sourceRepository = syncResult.getLocalRepository();

    if (syncResult.needsStash() || syncResult.needsSync()) {
      if (sourceRepository.getCurrentBranch() != null) {
        originalBranchName = sourceRepository.getCurrentBranch().getName();
      }
      else {
        originalBranchName = sourceRepository.getCurrentRevision();
      }
    }

    if (syncResult.needsStash()) {
      if (!stash()) {
        return;
      }
    }

    if (!Strings.isNullOrEmpty(syncResult.getTargetSyncSHA())) {
      //try to check out that revision.
      final GitBrancher brancher = ServiceManager.getService(project, GitBrancher.class);
      if (sourceRepository == null) {
        LOG.error("unexpected null source repo with a target SHA.");
        return;
      }
      assert syncResult.getTargetSyncSHA() != null;
      brancher.checkout(syncResult.getTargetSyncSHA(), false, Collections.singletonList(
          sourceRepository), new Runnable() {
                          @Override
                          public void run() {
                            refreshAndClose();
                          }
                        });
    }
    else {
      refreshAndClose();
    }
  }
}
