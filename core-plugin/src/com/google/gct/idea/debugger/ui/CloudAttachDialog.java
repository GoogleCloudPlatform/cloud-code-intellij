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
import com.google.gct.idea.debugger.ProjectRepositoryState;
import com.google.gct.idea.debugger.ProjectRepositoryValidator;
import com.google.gct.idea.debugger.SyncResult;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.stats.UsageTrackerProvider;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.HashMap;
import git4idea.actions.BasicAction;
import git4idea.branch.GitBrancher;
import git4idea.commands.GitCommand;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * CloudAttachDialog shows a dialog allowing the user to select a module and debug.
 */
public class CloudAttachDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(CloudAttachDialog.class);

  private final Project myProject;
  private final ProjectDebuggeeBinding myWireup;
  private JComboBox myDebuggeeTarget; // Module selector
  private ProjectSelector myElysiumProjectId; // Project combo box
  private JBLabel myInfoPanel;
  private String myOriginalBranchName;
  private JPanel myPanel;
  private CloudDebugProcessState myProcessResultState;
  private GitRepository mySourceRepository;
  private String myStashMessage = null;
  private SyncResult mySyncResult;
  private JBCheckBox mySyncStashCheckbox;
  private JBLabel myWarningLabel;
  private JBLabel myWarningLabel2;

  public CloudAttachDialog(@NotNull Project project) {
    super(project, true);

    myProject = project;
    init();
    initValidation();
    setTitle(GctBundle.getString("clouddebug.attachtitle"));
    setOKButtonText(GctBundle.getString("clouddebug.attach"));
    mySyncStashCheckbox.setVisible(false);
    mySyncStashCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (mySyncStashCheckbox.isVisible()) {
          myWarningLabel.setVisible(!mySyncStashCheckbox.isSelected());
          myWarningLabel2.setVisible(!mySyncStashCheckbox.isSelected());
          myInfoPanel.setVisible(mySyncStashCheckbox.isSelected());
          if (mySyncStashCheckbox.isSelected()) {
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

    myWarningLabel.setVisible(false);
    myWarningLabel.setFont(new Font(myWarningLabel.getFont().getName(), Font.BOLD,
                                    myWarningLabel.getFont().getSize() - 1));
    myWarningLabel.setForeground(JBColor.RED);
    myWarningLabel2.setVisible(false);
    myWarningLabel2.setFont(new Font(myWarningLabel2.getFont().getName(), Font.PLAIN,
                                     myWarningLabel.getFont().getSize() - 1));
    myWarningLabel2.setText(GctBundle.getString("clouddebug.sourcedoesnotmatch"));

    myInfoPanel.setFont(new Font(myWarningLabel2.getFont().getName(), Font.PLAIN,
                                 myWarningLabel.getFont().getSize() - 1));
    Border paddingBorder = BorderFactory.createEmptyBorder(2, 0, 2, 0);
    myInfoPanel.setBorder(paddingBorder);

    Window myWindow = getWindow();
    if (myWindow != null) {
      myWindow.setPreferredSize(new Dimension(355, 175));
    }
    BasicAction.saveAll();

    myWireup = new ProjectDebuggeeBinding(myElysiumProjectId, myDebuggeeTarget);
    myDebuggeeTarget.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        buildResult();
        checkSyncStashState();
        setOKActionEnabled(doValidate() == null);
      }
    });

    setOKActionEnabled(isContinued() || doValidate() == null);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  protected void doOKAction() {
    if (getOKAction().isEnabled()) {
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "start.session", null);
      // TODO : add source context tracking info
      if (mySyncStashCheckbox.isSelected()) {
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

    if (Strings.isNullOrEmpty(myElysiumProjectId.getText())) {
      return new ValidationInfo(GctBundle.getString("clouddebug.noprojectid"));
    }

    if (myDebuggeeTarget.getSelectedItem() == null) {
      return new ValidationInfo(GctBundle.getString("clouddebug.nomodule"));
    }

    return null;
  }

  @Nullable
  public CloudDebugProcessState getResultState() {
    return myProcessResultState;
  }

  public void setInputState(@Nullable CloudDebugProcessState inputState) {
    myWireup.setInputState(inputState);
  }

  private void buildResult() {
    myProcessResultState = myWireup.buildResult(myProject);
    ProjectRepositoryState repositoryState = ProjectRepositoryState.fromProcessState(myProcessResultState);
    repositoryState.setStashMessage(myStashMessage);
    repositoryState.setSourceRepository(mySourceRepository);
    repositoryState.setOriginalBranchName(myOriginalBranchName);
  }

  /**
   * Checks whether a stash or sync is needed based on the chosen target and local state.
   */
  private void checkSyncStashState() {
    if (myProcessResultState == null) {
      LOG.error("unexpected result state during a check sync stash state");
      return;
    }
    mySyncResult = new ProjectRepositoryValidator(myProcessResultState).checkSyncStashState();

    if (mySyncResult.needsStash() && mySyncResult.needsSync()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      mySyncStashCheckbox.setVisible(true);
      assert mySyncResult.getTargetSyncSHA() != null;
      mySyncStashCheckbox.setText(
          GctBundle.getString("clouddebug.stash.local.changes.and.sync", mySyncResult.getTargetSyncSHA().substring(0, 7)));
      mySyncStashCheckbox.setSelected(true);
      myWarningLabel.setVisible(false);
      myWarningLabel2.setVisible(false);
      myInfoPanel.setVisible(true);
    }
    else if (mySyncResult.needsStash()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      mySyncStashCheckbox.setVisible(true);
      mySyncStashCheckbox.setText(GctBundle.getString("clouddebug.stashbuttontext"));
      mySyncStashCheckbox.setSelected(true);
      myWarningLabel.setVisible(false);
      myWarningLabel2.setVisible(false);
      myInfoPanel.setVisible(true);
    }
    else if (mySyncResult.needsSync()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      mySyncStashCheckbox.setVisible(true);
      assert mySyncResult.getTargetSyncSHA() != null;
      mySyncStashCheckbox.setText("Sync to " + mySyncResult.getTargetSyncSHA().substring(0, 7));
      mySyncStashCheckbox.setSelected(true);
      myWarningLabel.setVisible(false);
      myWarningLabel2.setVisible(false);
      myInfoPanel.setVisible(true);
    }
    else if (mySyncResult.needsSync() && mySyncResult.getTargetSyncSHA() == null) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continueanyway")
          : GctBundle.getString("clouddebug.attach.anyway"));
      myWarningLabel.setVisible(true);
      myWarningLabel2.setVisible(true);
      myInfoPanel.setVisible(true);
      myWarningLabel2.setText(GctBundle.getString("clouddebug.no.matching.sha"));
    }
    else if (!mySyncResult.hasRemoteRepository()) {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continueanyway")
          : GctBundle.getString("clouddebug.attach.anyway"));
      myWarningLabel.setVisible(true);
      myWarningLabel2.setVisible(true);
      myInfoPanel.setVisible(true);
      if (mySyncResult.getRepositoryType() != null) {
        myWarningLabel2.setText(GctBundle.getString("clouddebug.repositories.are.not.supported",
            mySyncResult.getRepositoryType()));
      }
      else {
        myWarningLabel2.setText(GctBundle.getString("clouddebug.no.remote.repository"));
      }
    }
    else {
      setOKButtonText(isContinued()
          ? GctBundle.getString("clouddebug.continuesession")
          : GctBundle.getString("clouddebug.attach"));
      mySyncStashCheckbox.setVisible(false);
      myWarningLabel.setVisible(false);
      myWarningLabel2.setVisible(false);
      myInfoPanel.setVisible(true);
    }
  }

  private boolean isContinued() {
    CloudDebugProcessState state = myWireup.getInputState();
    return state != null && state.getCurrentServerBreakpointList().size() > 0;
  }

  private void refreshAndClose() {
    buildResult();
    close(OK_EXIT_CODE);
  }

  private boolean stash() {
    if (!mySyncResult.hasLocalRepository()) {
      LOG.error("unexpected null local repro in call to stash");
      return false;
    }

    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    if (changeListManager.isFreezedWithNotification("Can not stash changes now")) return false;

    final GitLineHandler handler = new GitLineHandler(myProject, mySourceRepository.getRoot(), GitCommand.STASH);
    handler.addParameters("save");
    handler.addParameters("--keep-index");
    String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
    myStashMessage = "Cloud Debugger saved changes from branch " + myOriginalBranchName + " at " + date;
    handler.addParameters(myStashMessage);
    AccessToken token = DvcsUtil.workingTreeChangeStarted(myProject);
    try {
      GitHandlerUtil.doSynchronously(handler, GitBundle.getString("stashing.title"), handler.printableCommandLine());
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(myProject, token);
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

    mySourceRepository = mySyncResult.getLocalRepository();

    if (mySyncResult.needsStash() || mySyncResult.needsSync()) {
      if (mySourceRepository.getCurrentBranch() != null) {
        myOriginalBranchName = mySourceRepository.getCurrentBranch().getName();
      }
      else {
        myOriginalBranchName = mySourceRepository.getCurrentRevision();
      }
    }

    if (mySyncResult.needsStash()) {
      if (!stash()) {
        return;
      }
    }

    if (!Strings.isNullOrEmpty(mySyncResult.getTargetSyncSHA())) {
      //try to check out that revision.
      final GitBrancher brancher = ServiceManager.getService(myProject, GitBrancher.class);
      if (mySourceRepository == null) {
        LOG.error("unexpected null source repo with a target SHA.");
        return;
      }
      assert mySyncResult.getTargetSyncSHA() != null;
      brancher.checkout(mySyncResult.getTargetSyncSHA(), Collections.singletonList(mySourceRepository), new Runnable() {
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

  /**
   * This binding between the project and debuggee is refactored out to make it reusable in the future.
   */
  private static class ProjectDebuggeeBinding {
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
      ProjectDebuggeeBinding.DebugTarget selectedItem =
        (ProjectDebuggeeBinding.DebugTarget)myDebugeeTarget.getSelectedItem();
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

                  if (debuggees != null && debuggees.getDebuggees() != null) {
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

    public static class DebugTarget {
      private static final String MODULE = "module";

      private final Debuggee myDebuggee;
      private String myDescription;
      private long myMinorVersion = 0;
      private String myModule;
      private String myVersion;

      public DebugTarget(@NotNull Debuggee debuggee, @NotNull String projectName) {
        myDebuggee = debuggee;
        if (myDebuggee.getLabels() != null) {
          myDescription = "";
          myModule = "";
          myVersion = "";
          String minorVersion = "";

          //Get the module name, major version and minor version strings.
          for (Map.Entry<String, String> entry : myDebuggee.getLabels().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(MODULE)) {
              myModule = entry.getValue();
            }
            else if (entry.getKey().equalsIgnoreCase("minorversion")) {
              minorVersion = entry.getValue();
            }
            else if (entry.getKey().equalsIgnoreCase("version")) {
              myVersion = entry.getValue();
            }
            else {
              //This is fallback logic where we dump the labels verbatim if they
              //change from underneath us.
              myDescription += String.format("%s:%s", entry.getKey(),entry.getValue());
            }
          }

          //Build a description from the strings.
          if (!Strings.isNullOrEmpty(myModule)) {
            myDescription = GctBundle.getString("clouddebug.version.with.module.format",
                myModule, myVersion);
          }
          else if (!Strings.isNullOrEmpty(myVersion)) {
            myDescription = GctBundle.getString("clouddebug.versionformat", myVersion);
          }

          //Record the minor version.  We only show the latest minor version.
          try {
            if (!Strings.isNullOrEmpty(minorVersion)) {
              myMinorVersion = Long.parseLong(minorVersion);
            }
          }
          catch(NumberFormatException ex) {
            LOG.warn("unable to parse minor version: " + minorVersion);
          }
        }

        //Finally if nothing worked (maybe labels aren't enabled?), we fall
        //back to the old logic of using description with the project name stripped out.
        if (Strings.isNullOrEmpty(myDescription))
        {
          myDescription = myDebuggee.getDescription();
          if (myDescription != null &&
              !Strings.isNullOrEmpty(projectName) &&
              myDescription.startsWith(projectName + "-")) {
            myDescription = myDescription.substring(projectName.length() + 1);
          }
        }
      }

      public String getId() {
        return myDebuggee.getId();
      }

      @Override
      public String toString() {
        return myDescription;
      }

      public long getMinorVersion() {
        return myMinorVersion;
      }

      public String getModule() {
        return myModule;
      }

      public String getVersion() {
        return myVersion;
      }
    }
  }
}
