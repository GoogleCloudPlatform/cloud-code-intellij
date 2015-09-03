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
package com.google.gct.idea.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.model.CloudRepoSourceContext;
import com.google.api.services.clouddebugger.model.Debuggee;
import com.google.api.services.clouddebugger.model.ListDebuggeesResponse;
import com.google.api.services.clouddebugger.model.SourceContext;
import com.google.gct.idea.util.GctBundle;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.xmlb.annotations.Transient;
import git4idea.GitPlatformFacade;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.branch.GitBrancher;
import git4idea.changes.GitChangeUtils;
import git4idea.commands.*;
import git4idea.config.GitVersionSpecialty;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.stash.GitStashUtils;
import git4idea.ui.StashInfo;
import git4idea.util.GitUIUtil;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import git4idea.util.UntrackedFilesNotifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE;

/**
 * This class validates current source state and restores it using git stash.
 */
public class ProjectRepositoryValidator {
  private static final Logger LOG = Logger.getInstance(ProjectRepositoryValidator.class);
  private final CloudDebugProcessState myProcessState;
  private final ProjectRepositoryState myRepoState;
  private Debugger myCloudDebuggerClient;

  public ProjectRepositoryValidator(@NotNull CloudDebugProcessState processState) {
    myProcessState = processState;
    myRepoState = ProjectRepositoryState.fromProcessState(processState);
  }

  /**
   * Compares the current source tree with the state described by the Cloud Debugger api. Only Git is currently
   * supported.
   */
  @NotNull
  @Transient
  public SyncResult checkSyncStashState() {
    if (myProcessState.getProject() == null) {
      return new SyncResult(/*isinvalid*/true, /*needsstash*/ false, /*needssync*/ false,
                            /*isdeterminable*/ true, /*target SHA*/ null, /*target repo*/ null);
    }
    GitRepositoryManager manager = GitUtil.getRepositoryManager(myProcessState.getProject());
    List<GitRepository> repositories = manager.getRepositories();
    CloudRepoSourceContext cloudRepo = null;

    boolean foundDebuggee = false;
    if (getCloudDebuggerClient() != null &&
        !com.google.common.base.Strings.isNullOrEmpty(myProcessState.getProjectNumber())) {
      ListDebuggeesResponse debuggees;
      try {
        debuggees = getCloudDebuggerClient().debuggees().list().setProject(myProcessState.getProjectNumber()).execute();
        for (Debuggee debuggee : debuggees.getDebuggees()) {
          if (myProcessState.getDebuggeeId() != null && myProcessState.getDebuggeeId().equals(debuggee.getId())) {
            foundDebuggee = true;
            List<SourceContext> contexts = debuggee.getSourceContexts();
            if (contexts != null) {
              for (SourceContext sourceContext : contexts) {
                cloudRepo = sourceContext.getCloudRepo();
                if (cloudRepo != null) {
                  break;
                }
              }
            }
          }
        }
      }
      catch (IOException ex) {
        LOG.warn("Error detecting server side source context", ex);
      }
    }

    if (!foundDebuggee) {
      return new SyncResult(/*isinvalid*/true,
                            /*needsstash*/ false,
                            /*needssync*/ false,
                            /*isdeterminable*/ true,
                            /*target SHA*/ null,
                            /*target repo*/ null);
    }

    GitRepository targetLocalRepo = null;
    if (cloudRepo != null) {
      for (GitRepository repository : repositories) {
        try {
          GitChangeUtils.resolveReference(myProcessState.getProject(), repository.getRoot(), cloudRepo.getRevisionId());
          targetLocalRepo = repository;
          break;
        }
        catch (VcsException ex) {
          LOG.warn("cloud revision not found in local repo.  continuing search...");
        }
      }
    }

    boolean needsStash = false;
    boolean needsSync = false;
    String syncSHA = null;

    if (targetLocalRepo != null) {
      //check for local changes.
      try {
        if (GitUtil.hasLocalChanges(true, myProcessState.getProject(), targetLocalRepo.getRoot()) ||
            GitUtil.hasLocalChanges(false, myProcessState.getProject(), targetLocalRepo.getRoot())) {
          needsStash = true;
        }
        if (!Strings.isNullOrEmpty(targetLocalRepo.getCurrentRevision()) &&
            !Strings.isNullOrEmpty(cloudRepo.getRevisionId()) &&
            targetLocalRepo.getCurrentRevision() != null &&
            !targetLocalRepo.getCurrentRevision().equals(cloudRepo.getRevisionId())) {
          syncSHA = cloudRepo.getRevisionId();
          needsSync = true;
        }

      }
      catch (VcsException vcsException) {
        LOG.error("Error detecting local changes during attach", vcsException);
      }
    }

    return new SyncResult(/*isinvalid*/ false, needsStash, needsSync, targetLocalRepo != null, syncSHA,
                          targetLocalRepo);
  }

  @Nullable
  protected Debugger getCloudDebuggerClient() {
    if (myCloudDebuggerClient == null) {
      myCloudDebuggerClient = CloudDebuggerClient.getLongTimeoutClient(myProcessState);
    }
    return myCloudDebuggerClient;
  }

  @SuppressWarnings("ConstantConditions")
  public void hardRefresh() {
    if (myRepoState.hasSourceRepository()) {
      List<VirtualFile> list = VfsUtil.markDirty(true, true, myRepoState.getSourceRepository().getRoot());
      if (!list.isEmpty()) {
        LocalFileSystem.getInstance().refreshFiles(list, false, true, null);
      }
    }
  }

  /**
   * Returns true if this state has a valid debug client that can poll for snapshot information.
   */
  @Transient
  public boolean isValidDebuggee() {
    SyncResult result = checkSyncStashState();
    return result.isValidDebuggee();
  }

  public void restoreToOriginalState(final @NotNull Project project) {
    if (myRepoState.hasSourceRepository()) {
      assert myRepoState.getSourceRepository() != null;
      final VirtualFile root = myRepoState.getSourceRepository().getRoot();

      //check for an unstash requirement.
      final Ref<StashInfo> targetStash = new Ref<StashInfo>();
      if (!Strings.isNullOrEmpty(myRepoState.getStashMessage())) {
        GitStashUtils.loadStashStack(project, root, new Consumer<StashInfo>() {
          @Override
          public void consume(StashInfo stashInfo) {
            if (!Strings.isNullOrEmpty(stashInfo.getMessage()) &&
                stashInfo.getMessage().equals(myRepoState.getStashMessage())) {
              targetStash.set(stashInfo);
            }
          }
        });
      }

      // If an unstash is required, we will always have an original branch name as well.
      if (!Strings.isNullOrEmpty(myRepoState.getOriginalBranchName())) {
        assert myRepoState.getOriginalBranchName() != null;
        String branchDisplayName = myRepoState.getOriginalBranchName();
        if (branchDisplayName.length() > 10) {
          branchDisplayName = branchDisplayName.substring(0, 7) + "...";
        }
        if (Messages.showYesNoDialog(GctBundle.getString("clouddebug.restorestash", branchDisplayName),
                                     GctBundle.getString("clouddebug.restorechanges.title"),
                                     Messages.getInformationIcon()) == Messages.YES) {
          final GitBrancher brancher = ServiceManager.getService(project, GitBrancher.class);
          brancher
            .checkout(myRepoState.getOriginalBranchName(), Collections.singletonList(myRepoState.getSourceRepository()),
                      new Runnable() {
                        @Override
                        public void run() {
                          myRepoState.getSourceRepository().update();
                          if (!targetStash.isNull()) {
                            unstash(project, targetStash, root);
                          }
                        }
                      });
        }
      }
    }
  }

  private static void addStashParameter(@NotNull Project project, @NotNull GitHandler handler, @NotNull String stash) {
    GitVcs vcs = GitVcs.getInstance(project);
    if (vcs != null && GitVersionSpecialty.NEEDS_QUOTES_IN_STASH_NAME.existsIn(vcs.getVersion())) {
      handler.addParameters(GeneralCommandLine.inescapableQuote(stash));
    }
    else {
      handler.addParameters(stash);
    }
  }

  private void unstash(final @NotNull Project project,
                       @NotNull final Ref<StashInfo> targetStash,
                       @NotNull final VirtualFile root) {
    if (myRepoState.getSourceRepository() == null ||
        myRepoState.getOriginalBranchName() == null ||
        (!myRepoState.getOriginalBranchName().equals(myRepoState.getSourceRepository().getCurrentBranchName()) &&
         !myRepoState.getOriginalBranchName().equals(myRepoState.getSourceRepository().getCurrentRevision()))) {
      Messages.showErrorDialog(GctBundle.getString("clouddebug.erroroncheckout", myRepoState.getOriginalBranchName()),
                               "Error");
      return;
    }
    final GitLineHandler handler = new GitLineHandler(project, root, GitCommand.STASH);
    handler.addParameters("apply");
    handler.addParameters("--index");
    addStashParameter(project, handler, targetStash.get().getStash());
    final AtomicBoolean conflict = new AtomicBoolean();

    handler.addLineListener(new GitLineHandlerAdapter() {
      @Override
      public void onLineAvailable(String line, Key outputType) {
        if (line.contains("Merge conflict")) {
          conflict.set(true);
        }
      }
    });
    GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector =
      new GitUntrackedFilesOverwrittenByOperationDetector(root);
    GitLocalChangesWouldBeOverwrittenDetector localChangesDetector =
      new GitLocalChangesWouldBeOverwrittenDetector(root, MERGE);
    handler.addLineListener(untrackedFilesDetector);
    handler.addLineListener(localChangesDetector);

    AccessToken token = DvcsUtil.workingTreeChangeStarted(project);
    try {
      final Ref<GitCommandResult> result = Ref.create();
      ProgressManager.getInstance()
        .run(new Task.Modal(handler.project(), GitBundle.getString("unstash.unstashing"), false) {
          @Override
          public void run(@NotNull final ProgressIndicator indicator) {
            indicator.setIndeterminate(true);
            handler
              .addLineListener(new GitHandlerUtil.GitLineHandlerListenerProgress(indicator, handler, "stash", false));
            Git git = ServiceManager.getService(Git.class);
            result.set(git.runCommand(new Computable.PredefinedValueComputable<GitLineHandler>(handler)));
          }
        });

      ServiceManager.getService(project, GitPlatformFacade.class).hardRefresh(root);
      GitCommandResult res = result.get();
      if (conflict.get()) {
        Messages
          .showDialog(GctBundle.getString("clouddebug.unstashmergeconflicts"), "Merge Conflicts", new String[]{"Ok"}, 0,
                      Messages.getErrorIcon());

      }
      else if (untrackedFilesDetector.wasMessageDetected()) {
        UntrackedFilesNotifier
          .notifyUntrackedFilesOverwrittenBy(project, root, untrackedFilesDetector.getRelativeFilePaths(), "unstash",
                                             null);
      }
      else if (localChangesDetector.wasMessageDetected()) {
        LocalChangesWouldBeOverwrittenHelper
          .showErrorDialog(project, root, "unstash", localChangesDetector.getRelativeFilePaths());
      }
      else if (!res.success()) {
        GitUIUtil.showOperationErrors(project, handler.errors(), handler.printableCommandLine());
      }
      else if (res.success()) {
        ProgressManager.getInstance().run(
          new Task.Modal(project, GctBundle.getString("clouddebug.removestashx", targetStash.get().getStash()), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              if (myProject == null) {
                return;
              }
              final GitSimpleHandler h = new GitSimpleHandler(myProject, root, GitCommand.STASH);
              h.addParameters("drop");
              addStashParameter(project, h, targetStash.get().getStash());
              try {
                h.run();
                h.unsilence();
              }
              catch (final VcsException ex) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    GitUIUtil.showOperationError(myProject, ex, h.printableCommandLine());
                  }
                });
              }
            }
          });

      }
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(project, token);
    }
  }

  /**
   * The SyncResult fully describes the result from checking the current users' source with what the cloud debugger is
   * attached to on the server.
   */
  public static class SyncResult {
    private final boolean myInvalidDebuggee;
    private final boolean myIsDeterminable;
    private final boolean myNeedsStash;
    private final boolean myNeedsSync;
    private final GitRepository myTargetRepository;
    private final String myTargetSyncSHA;

    private SyncResult(boolean invalidDebuggee,
                       boolean needsStash,
                       boolean needsSync,
                       boolean isDeterminable,
                       @Nullable String targetSyncSHA,
                       @Nullable GitRepository targetRepository) {
      myInvalidDebuggee = invalidDebuggee;
      myNeedsStash = needsStash;
      myNeedsSync = needsSync;
      myIsDeterminable = isDeterminable;
      myTargetSyncSHA = targetSyncSHA;
      myTargetRepository = targetRepository;
    }

    @Nullable
    public GitRepository getTargetRepository() {
      return myTargetRepository;
    }

    @Nullable
    public String getTargetSyncSHA() {
      return myTargetSyncSHA;
    }

    public boolean isDeterminable() {
      return myIsDeterminable;
    }

    public boolean isValidDebuggee() {
      return !myInvalidDebuggee;
    }

    public boolean isValidSource() {
      return !myInvalidDebuggee && !myNeedsStash && !myNeedsSync;
    }

    public boolean needsStash() {
      return myNeedsStash;
    }

    public boolean needsSync() {
      return myNeedsSync;
    }
  }
}
