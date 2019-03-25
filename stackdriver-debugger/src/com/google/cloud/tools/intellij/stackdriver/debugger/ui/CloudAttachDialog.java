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

package com.google.cloud.tools.intellij.stackdriver.debugger.ui;

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerService;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.stackdriver.debugger.CloudDebugProcessState;
import com.google.cloud.tools.intellij.stackdriver.debugger.ProjectRepositoryState;
import com.google.cloud.tools.intellij.stackdriver.debugger.ProjectRepositoryValidator;
import com.google.cloud.tools.intellij.stackdriver.debugger.StackdriverDebuggerBundle;
import com.google.cloud.tools.intellij.stackdriver.debugger.SyncResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import git4idea.actions.BasicAction;
import git4idea.branch.GitBrancher;
import git4idea.commands.GitCommand;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CloudAttachDialog shows a dialog allowing the user to select a target (module & version) and
 * debug.
 */
public class CloudAttachDialog extends DialogWrapper {

  private static final Logger LOG = Logger.getInstance(CloudAttachDialog.class);

  private final Project project;
  private final ProjectDebuggeeBinding wireup;
  private ProjectRepositoryValidator projectRepositoryValidator;
  private JComboBox targetSelector; // Module & version combo box
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
  private ProjectSelector projectSelector;

  /** Initializes the cloud debugger dialog. */
  public CloudAttachDialog(
      @NotNull Project project, @VisibleForTesting ProjectDebuggeeBinding wireup) {
    super(project, true);

    this.project = project;
    init();
    initValidation();
    setTitle(StackdriverDebuggerBundle.getString("clouddebug.attachtitle"));
    setOKButtonText(StackdriverDebuggerBundle.getString("clouddebug.attach"));

    infoMessage.setVisible(true);
    syncStashCheckbox.setVisible(false);
    syncStashCheckbox.addActionListener(
        event -> {
          if (syncStashCheckbox.isVisible()) {
            warningHeader.setVisible(!syncStashCheckbox.isSelected());
            warningMessage.setVisible(!syncStashCheckbox.isSelected());
            // Show force attach text if the user chooses not to sync/stash
            setOkText(!syncStashCheckbox.isSelected());
          }
        });

    warningHeader.setVisible(false);
    warningHeader.setFont(
        new Font(
            warningHeader.getFont().getName(), Font.BOLD, warningHeader.getFont().getSize() - 1));
    warningHeader.setForeground(JBColor.RED);
    warningMessage.setVisible(false);
    warningMessage.setFont(
        new Font(
            warningMessage.getFont().getName(), Font.PLAIN, warningHeader.getFont().getSize() - 1));
    warningMessage.setText(StackdriverDebuggerBundle.getString("clouddebug.sourcedoesnotmatch"));

    infoMessage.setFont(
        new Font(
            warningMessage.getFont().getName(), Font.PLAIN, warningHeader.getFont().getSize() - 1));
    Border paddingBorder = BorderFactory.createEmptyBorder(2, 0, 2, 0);
    infoMessage.setBorder(paddingBorder);

    Window window = getWindow();
    if (window != null) {
      window.setPreferredSize(new Dimension(355, 175));
    }
    BasicAction.saveAll();

    this.wireup =
        wireup == null
            ? new ProjectDebuggeeBinding(projectSelector, targetSelector, getOKAction())
            : wireup;
    targetSelector.setEnabled(false);
    targetSelector.addActionListener(
        event -> {
          if (targetSelector.isEnabled()) {
            buildResult();
            checkSyncStashState();
          } else {
            warningHeader.setVisible(false);
            warningMessage.setVisible(false);
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
      UsageTrackerService.getInstance().trackEvent(GctTracking.CLOUD_DEBUGGER_START_SESSION).ping();
      // TODO : add source context tracking info
      if (syncStashCheckbox.isSelected()) {
        syncOrStash();
      } else {
        buildResult();
        close(OK_EXIT_CODE); // We close before kicking off the update so it doesn't interfere with
        // the output window coming to focus.
      }
    }
  }

  @Override
  protected ValidationInfo doValidate() {
    // These should not normally occur.
    if (!Services.getLoginService().isLoggedIn()) {
      return new ValidationInfo(StackdriverDebuggerBundle.getString("clouddebug.nologin"));
    }

    CloudProject selectedCloudProject = projectSelector.getSelectedProject();
    if (selectedCloudProject == null) {
      return new ValidationInfo(
          StackdriverDebuggerBundle.getString("clouddebug.noprojectid"), projectSelector);
    }

    // validation should run only after the query for debug targets has results
    // assumption: either an ErrorHolder or one or more DebugTargets are added to the selector when
    //             the result is available
    if (!targetSelector.isEnabled()) {
      if (targetSelector.getItemCount() > 0) {
        if (targetSelector.getSelectedItem() instanceof ErrorHolder) {
          return new ValidationInfo(
              ((ErrorHolder) targetSelector.getSelectedItem()).getErrorMessage(), projectSelector);
        } else {
          return new ValidationInfo(
              StackdriverDebuggerBundle.getString("clouddebug.nomodulesfound"), projectSelector);
        }
      } else if (wireup.isCdbQueried()) {
        // We went to CDB and detected no debuggees.
        return new ValidationInfo(
            StackdriverDebuggerBundle.getString("clouddebug.debug.targets.accessdenied"),
            projectSelector);
      }
    }

    // validation should run only after the query for debug targets has results
    // assumption: either an ErrorHolder or one or more DebugTargets are added to the selector when
    //             the result is available
    if (targetSelector.getSelectedItem() == null && targetSelector.getItemCount() > 0) {
      return new ValidationInfo(
          StackdriverDebuggerBundle.getString("clouddebug.nomodulesfound"), targetSelector);
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
  ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  @VisibleForTesting
  JComboBox getTargetSelector() {
    return targetSelector;
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
  JBCheckBox getSyncStashCheckbox() {
    return syncStashCheckbox;
  }

  @VisibleForTesting
  void setProjectRepositoryValidator(ProjectRepositoryValidator projectRepositoryValidator) {
    this.projectRepositoryValidator = projectRepositoryValidator;
  }

  private void buildResult() {
    processResultState = wireup.buildResult(project);
    ProjectRepositoryState repositoryState =
        ProjectRepositoryState.fromProcessState(processResultState);
    repositoryState.setStashMessage(stashMessage);
    repositoryState.setSourceRepository(sourceRepository);
    repositoryState.setOriginalBranchName(originalBranchName);
  }

  /** Checks whether a stash or sync is needed based on the chosen target and local state. */
  private void checkSyncStashState() {
    if (processResultState == null) {
      LOG.error("unexpected result state during a check sync stash state");
      return;
    }

    syncResult =
        projectRepositoryValidator == null
            ? new ProjectRepositoryValidator(processResultState).checkSyncStashState()
            : projectRepositoryValidator.checkSyncStashState();

    // reset state
    syncStashCheckbox.setVisible(false);
    syncStashCheckbox.setSelected(false);
    warningHeader.setVisible(false);
    warningMessage.setVisible(false);
    checkBackgroundSessions();

    if (syncResult.needsStash() && syncResult.needsSync()) {
      setOkText(false);
      syncStashCheckbox.setVisible(true);
      assert syncResult.getTargetSyncSha() != null;
      syncStashCheckbox.setText(
          StackdriverDebuggerBundle.getString(
              "clouddebug.stash.local.changes.and.sync",
              syncResult.getTargetSyncSha().substring(0, 7)));
      syncStashCheckbox.setSelected(true);
    } else if (syncResult.needsStash()) {
      setOkText(false);
      syncStashCheckbox.setVisible(true);
      syncStashCheckbox.setText(StackdriverDebuggerBundle.getString("clouddebug.stashbuttontext"));
      syncStashCheckbox.setSelected(true);
    } else if (syncResult.needsSync() && syncResult.getTargetSyncSha() == null) {
      setOkText(true);
      warningHeader.setVisible(true);
      warningMessage.setVisible(true);
      warningMessage.setText(StackdriverDebuggerBundle.getString("clouddebug.no.matching.sha"));
    } else if (syncResult.needsSync()) {
      setOkText(false);
      syncStashCheckbox.setVisible(true);
      assert syncResult.getTargetSyncSha() != null;
      syncStashCheckbox.setText("Sync to " + syncResult.getTargetSyncSha().substring(0, 7));
      syncStashCheckbox.setSelected(true);
    } else if (!syncResult.hasRemoteRepository()) {
      setOkText(true);
      warningHeader.setVisible(true);
      warningMessage.setVisible(true);
      if (syncResult.getRepositoryType() != null) {
        warningMessage.setText(
            StackdriverDebuggerBundle.getString(
                "clouddebug.repositories.are.not.supported", syncResult.getRepositoryType()));
      } else {
        warningMessage.setText(
            StackdriverDebuggerBundle.getString("clouddebug.no.remote.repository"));
      }
    } else {
      setOkText(false);
    }
  }

  private void setOkText(boolean showForcedWording) {
    if (showForcedWording) {
      setOKButtonText(
          isContinued() && targetMatchesCurrentState()
              ? StackdriverDebuggerBundle.getString("clouddebug.continueanyway")
              : StackdriverDebuggerBundle.getString("clouddebug.attach.anyway"));
    } else {
      setOKButtonText(
          isContinued() && targetMatchesCurrentState()
              ? StackdriverDebuggerBundle.getString("clouddebug.continuesession")
              : StackdriverDebuggerBundle.getString("clouddebug.attach"));
    }
  }

  private void checkBackgroundSessions() {
    boolean hasUnselectedBackgroundSessions = isContinued() && !targetMatchesCurrentState();
    if (hasUnselectedBackgroundSessions) {
      warningHeader.setVisible(true);
      warningMessage.setVisible(true);
      warningMessage.setText(
          StackdriverDebuggerBundle.getString("clouddebug.terminate.background"));
    }
  }

  private boolean isContinued() {
    CloudDebugProcessState state = wireup.getInputState();
    return state != null && state.getCurrentServerBreakpointList().size() > 0;
  }

  private boolean targetMatchesCurrentState() {
    CloudDebugProcessState state = wireup.getInputState();

    return state != null
        && targetSelector != null
        && targetSelector.getSelectedItem() != null
        && StringUtil.equals(
            state.getDebuggeeId(), ((DebugTarget) targetSelector.getSelectedItem()).getId());
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
    if (changeListManager.isFreezedWithNotification("Can not stash changes now")) {
      return false;
    }

    final GitLineHandler handler =
        new GitLineHandler(project, sourceRepository.getRoot(), GitCommand.STASH);
    handler.addParameters("save");
    handler.addParameters("--keep-index");
    String date =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
    stashMessage = "Cloud Debugger saved changes from branch " + originalBranchName + " at " + date;
    handler.addParameters(stashMessage);
    AccessToken token = DvcsUtil.workingTreeChangeStarted(project);
    try {
      GitHandlerUtil.doSynchronously(
          handler, GitBundle.getString("stashing.title"), handler.printableCommandLine());
    } finally {
      token.finish();
    }
    return true;
  }

  /** Performs the actual sync/stash needed before attaching. */
  private void syncOrStash() {
    // When the user edits a document in intelliJ, there are spurious updates to the timestamp of
    // the document for an unspecified amount of time (even though there are no real edits).
    // So, we save-all right before we stash to (help) ensure we don't get a conflict dialog.
    // The conflict dialog happens when the timestamps of the document and file are mismatched.
    // So when we do the git operations, we want the document and file timestamps to match exactly.
    BasicAction.saveAll();

    if (syncResult == null) {
      checkSyncStashState();
    }

    sourceRepository = syncResult.getLocalRepository();

    if (syncResult.needsStash() || syncResult.needsSync()) {
      if (sourceRepository.getCurrentBranch() != null) {
        originalBranchName = sourceRepository.getCurrentBranch().getName();
      } else {
        originalBranchName = sourceRepository.getCurrentRevision();
      }
    }

    if (syncResult.needsStash() && !stash()) {
      return;
    }

    if (!Strings.isNullOrEmpty(syncResult.getTargetSyncSha())) {
      // try to check out that revision.
      final GitBrancher brancher = ServiceManager.getService(project, GitBrancher.class);
      if (sourceRepository == null) {
        LOG.error("unexpected null source repo with a target SHA.");
        return;
      }
      assert syncResult.getTargetSyncSha() != null;
      brancher.checkout(
          syncResult.getTargetSyncSha(),
          false,
          Collections.singletonList(sourceRepository),
          this::refreshAndClose);
    } else {
      refreshAndClose();
    }
  }

  private void createUIComponents() {
    projectSelector = new ProjectSelector(project);
  }
}
