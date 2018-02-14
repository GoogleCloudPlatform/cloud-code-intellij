/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.debugger;

import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.v2.model.CloudRepoSourceContext;
import com.google.api.services.clouddebugger.v2.model.Debuggee;
import com.google.api.services.clouddebugger.v2.model.GerritSourceContext;
import com.google.api.services.clouddebugger.v2.model.GitSourceContext;
import com.google.api.services.clouddebugger.v2.model.ListDebuggeesResponse;
import com.google.api.services.clouddebugger.v2.model.SourceContext;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.util.GctBundle;
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
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitHandler;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLineHandlerAdapter;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.commands.GitSimpleHandler;
import git4idea.commands.GitUntrackedFilesOverwrittenByOperationDetector;
import git4idea.config.GitVersionSpecialty;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.stash.GitStashUtils;
import git4idea.ui.StashInfo;
import git4idea.util.GitUIUtil;
import git4idea.util.GitUntrackedFilesHelper;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** This class validates current source state and restores it using git stash. */
public class ProjectRepositoryValidator {

  private static final Logger LOG = Logger.getInstance(ProjectRepositoryValidator.class);
  private final CloudDebugProcessState processState;
  private final ProjectRepositoryState repoState;
  private Debugger cloudDebuggerClient;

  public ProjectRepositoryValidator(@NotNull CloudDebugProcessState processState) {
    this.processState = processState;
    repoState = ProjectRepositoryState.fromProcessState(processState);
  }

  /**
   * Compares the current source tree with the state described by the Cloud Debugger api. Only local
   * and cloud repo Git repositories are supported.
   */
  @NotNull
  @Transient
  public SyncResult checkSyncStashState() {
    if (processState.getProject() == null) {
      return new SyncResult(
          /*isInvalid*/ true,
          /*needsStash*/ false,
          /*needsSync*/ false,
          /*target SHA*/ null,
          /*target repo*/ null,
          /* cloud repo */ false,
          /* repoType */ null);
    }
    GitRepositoryManager manager = GitUtil.getRepositoryManager(processState.getProject());
    List<GitRepository> repositories = manager.getRepositories();
    CloudRepoSourceContext cloudRepo = null;
    GerritSourceContext gerritRepo = null;
    GitSourceContext otherGitRepo = null;
    String repoType = null;

    boolean foundDebuggee = false;
    if (getCloudDebuggerClient() != null
        && !com.google.common.base.Strings.isNullOrEmpty(processState.getProjectNumber())) {
      ListDebuggeesResponse debuggees;
      try {
        debuggees =
            getCloudDebuggerClient()
                .debuggees()
                .list()
                .setProject(processState.getProjectNumber())
                .setClientVersion(
                    ServiceManager.getService(PluginInfoService.class)
                        .getClientVersionForCloudDebugger())
                .execute();
        for (Debuggee debuggee : debuggees.getDebuggees()) {
          if (processState.getDebuggeeId() != null
              && processState.getDebuggeeId().equals(debuggee.getId())) {
            // implicit assumption this doesn't happen more than once
            foundDebuggee = true;
            List<SourceContext> contexts = debuggee.getSourceContexts();
            if (contexts != null) {
              for (SourceContext sourceContext : contexts) {
                cloudRepo = sourceContext.getCloudRepo();
                gerritRepo = sourceContext.getGerrit();
                otherGitRepo = sourceContext.getGit();
                if (cloudRepo != null) {
                  // shouldn't be more than one repo but if there is, we'll prefer cloud repos
                  break;
                } else if (sourceContext.getCloudWorkspace() != null) {
                  repoType = GctBundle.getString("clouddebug.workspace");
                }
              }
            }
          }
        }
      } catch (IOException ex) {
        LOG.warn("Error detecting server side source context", ex);
      }
    }

    if (!foundDebuggee) {
      return new SyncResult(
          /*isinvalid*/ true,
          /*needsstash*/ false,
          /*needssync*/ false,
          /*target SHA*/ null,
          /*target repo*/ null,
          /* hasCloudRepository */ false,
          /* repoType */ GctBundle.getString("clouddebug.unknown.repository.type"));
    }

    GitRepository targetLocalRepo = null;
    String revisionId = null;

    // shouldn't be more than one repo but if there is, we pick cloud repos
    if (cloudRepo != null) {
      revisionId = cloudRepo.getRevisionId();
      repoType = GctBundle.getString("clouddebug.cloud.repository");
    } else if (gerritRepo != null) {
      revisionId = gerritRepo.getRevisionId();
      repoType = GctBundle.getString("clouddebug.gerrit");
    } else if (otherGitRepo != null) {
      revisionId = otherGitRepo.getRevisionId();
      repoType = GctBundle.getString("clouddebug.nongoogle.git");
    }

    if (revisionId != null) {
      for (GitRepository repository : repositories) {
        try {
          GitChangeUtils.resolveReference(
              processState.getProject(), repository.getRoot(), revisionId);
          targetLocalRepo = repository;
          break;
        } catch (VcsException ex) {
          LOG.warn("cloud revision not found in local repo.  continuing search...");
        }
      }
    }

    boolean needsStash = false;
    boolean needsSync = false;
    String syncSha = null;

    if (targetLocalRepo != null) {
      // check for local changes.
      try {
        if (GitUtil.hasLocalChanges(true, processState.getProject(), targetLocalRepo.getRoot())
            || GitUtil.hasLocalChanges(
                false, processState.getProject(), targetLocalRepo.getRoot())) {
          needsStash = true;
        }
        if (!Strings.isNullOrEmpty(targetLocalRepo.getCurrentRevision())
            && !Strings.isNullOrEmpty(revisionId)
            && targetLocalRepo.getCurrentRevision() != null
            && !targetLocalRepo.getCurrentRevision().equals(revisionId)) {
          syncSha = revisionId;
          needsSync = true;
        }

      } catch (VcsException vcsException) {
        LOG.error("Error detecting local changes during attach", vcsException);
      }
    }

    boolean hasRemoteRepository = cloudRepo != null || gerritRepo != null || otherGitRepo != null;
    return new SyncResult(
        /*isinvalid*/ false,
        needsStash,
        needsSync,
        syncSha,
        targetLocalRepo,
        hasRemoteRepository,
        repoType);
  }

  @Nullable
  protected Debugger getCloudDebuggerClient() {
    if (cloudDebuggerClient == null) {
      cloudDebuggerClient = CloudDebuggerClient.getLongTimeoutClient(processState);
    }
    return cloudDebuggerClient;
  }

  /** Refresh the repository files. */
  @SuppressWarnings("ConstantConditions")
  public void hardRefresh() {
    if (repoState.hasSourceRepository()) {
      List<VirtualFile> list =
          VfsUtil.markDirty(true, true, repoState.getSourceRepository().getRoot());
      if (!list.isEmpty()) {
        LocalFileSystem.getInstance().refreshFiles(list, false, true, null);
      }
    }
  }

  /** Returns true if this state has a valid debug client that can poll for snapshot information. */
  @Transient
  public boolean isValidDebuggee() {
    SyncResult result = checkSyncStashState();
    return result.isValidDebuggee();
  }

  /** Restore the repository to its original state. */
  public void restoreToOriginalState(final @NotNull Project project) {
    if (repoState.hasSourceRepository()) {
      assert repoState.getSourceRepository() != null;
      final VirtualFile root = repoState.getSourceRepository().getRoot();

      // check for an unstash requirement.
      final Ref<StashInfo> targetStash = new Ref<StashInfo>();
      if (!Strings.isNullOrEmpty(repoState.getStashMessage())) {
        GitStashUtils.loadStashStack(
            project,
            root,
            new Consumer<StashInfo>() {
              @Override
              public void consume(StashInfo stashInfo) {
                if (!Strings.isNullOrEmpty(stashInfo.getMessage())
                    && stashInfo.getMessage().equals(repoState.getStashMessage())) {
                  targetStash.set(stashInfo);
                }
              }
            });
      }

      // If an unstash is required, we will always have an original branch name as well.
      if (!Strings.isNullOrEmpty(repoState.getOriginalBranchName())) {
        assert repoState.getOriginalBranchName() != null;
        String branchDisplayName = repoState.getOriginalBranchName();
        if (branchDisplayName.length() > 10) {
          branchDisplayName = branchDisplayName.substring(0, 7) + "...";
        }
        if (Messages.showYesNoDialog(
                GctBundle.getString("clouddebug.restorestash", branchDisplayName),
                GctBundle.getString("clouddebug.restorechanges.title"),
                Messages.getInformationIcon())
            == Messages.YES) {
          final GitBrancher brancher = ServiceManager.getService(project, GitBrancher.class);
          brancher.checkout(
              repoState.getOriginalBranchName(),
              false,
              Collections.singletonList(repoState.getSourceRepository()),
              new Runnable() {
                @Override
                public void run() {
                  repoState.getSourceRepository().update();
                  if (!targetStash.isNull()) {
                    unstash(project, targetStash, root);
                  }
                }
              });
        }
      }
    }
  }

  private static void addStashParameter(
      @NotNull Project project, @NotNull GitHandler handler, @NotNull String stash) {
    GitVcs vcs = GitVcs.getInstance(project);
    if (vcs != null && GitVersionSpecialty.NEEDS_QUOTES_IN_STASH_NAME.existsIn(vcs.getVersion())) {
      handler.addParameters(GeneralCommandLine.inescapableQuote(stash));
    } else {
      handler.addParameters(stash);
    }
  }

  private void unstash(
      final @NotNull Project project,
      @NotNull final Ref<StashInfo> targetStash,
      @NotNull final VirtualFile root) {
    if (repoState.getSourceRepository() == null
        || repoState.getOriginalBranchName() == null
        || (!repoState
                .getOriginalBranchName()
                .equals(repoState.getSourceRepository().getCurrentBranchName())
            && !repoState
                .getOriginalBranchName()
                .equals(repoState.getSourceRepository().getCurrentRevision()))) {
      Messages.showErrorDialog(
          GctBundle.getString("clouddebug.erroroncheckout", repoState.getOriginalBranchName()),
          "Error");
      return;
    }
    final GitLineHandler handler = new GitLineHandler(project, root, GitCommand.STASH);
    handler.addParameters("apply");
    handler.addParameters("--index");
    addStashParameter(project, handler, targetStash.get().getStash());
    final AtomicBoolean conflict = new AtomicBoolean();

    handler.addLineListener(
        new GitLineHandlerAdapter() {
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
          .run(
              new Task.Modal(handler.project(), GitBundle.getString("unstash.unstashing"), false) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                  indicator.setIndeterminate(true);
                  handler.addLineListener(
                      new GitHandlerUtil.GitLineHandlerListenerProgress(
                          indicator, handler, "stash", false));
                  Git git = ServiceManager.getService(Git.class);
                  result.set(
                      git.runCommand(
                          new Computable.PredefinedValueComputable<GitLineHandler>(handler)));
                }
              });

      ServiceManager.getService(project, GitPlatformFacade.class).hardRefresh(root);
      GitCommandResult res = result.get();
      if (conflict.get()) {
        Messages.showDialog(
            GctBundle.getString("clouddebug.unstashmergeconflicts"),
            "Merge Conflicts",
            new String[] {"Ok"},
            0,
            Messages.getErrorIcon());

      } else if (untrackedFilesDetector.wasMessageDetected()) {
        GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(
            project, root, untrackedFilesDetector.getRelativeFilePaths(), "unstash", null);
      } else if (localChangesDetector.wasMessageDetected()) {
        LocalChangesWouldBeOverwrittenHelper.showErrorDialog(
            project, root, "unstash", localChangesDetector.getRelativeFilePaths());
      } else if (!res.success()) {
        GitUIUtil.showOperationErrors(project, handler.errors(), handler.printableCommandLine());
      } else if (res.success()) {
        ProgressManager.getInstance()
            .run(
                new Task.Modal(
                    project,
                    GctBundle.getString("clouddebug.removestashx", targetStash.get().getStash()),
                    false) {
                  @Override
                  public void run(@NotNull ProgressIndicator indicator) {
                    if (project == null) {
                      return;
                    }
                    final GitSimpleHandler h =
                        new GitSimpleHandler(project, root, GitCommand.STASH);
                    h.addParameters("drop");
                    addStashParameter(project, h, targetStash.get().getStash());
                    try {
                      h.run();
                      h.unsilence();
                    } catch (final VcsException ex) {
                      ApplicationManager.getApplication()
                          .invokeLater(
                              new Runnable() {
                                @Override
                                public void run() {
                                  GitUIUtil.showOperationError(
                                      project, ex, h.printableCommandLine());
                                }
                              });
                    }
                  }
                });
      }
    } finally {
      DvcsUtil.workingTreeChangeFinished(project, token);
    }
  }
}
