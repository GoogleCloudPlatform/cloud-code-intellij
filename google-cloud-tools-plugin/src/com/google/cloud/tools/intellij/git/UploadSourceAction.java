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

package com.google.cloud.tools.intellij.git;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.TypeSafeDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashSet;
import com.intellij.vcsUtil.VcsFileUtil;

import git4idea.DialogManager;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.actions.BasicAction;
import git4idea.actions.GitInit;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitSimpleHandler;
import git4idea.config.GitConfigUtil;
import git4idea.config.GitVcsApplicationSettings;
import git4idea.config.GitVersion;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import git4idea.util.GitFileUtils;
import git4idea.util.GitUIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Action to trigger an upload to a GCP git repository.
 */
public class UploadSourceAction extends DumbAwareAction {

  private static final Logger LOG = Logger.getInstance(UploadSourceAction.class);

  public UploadSourceAction() {
    super(GctBundle.message("uploadtogcp.text"), GctBundle.message("uploadtogcp.description"),
        GoogleCloudToolsIcons.CLOUD);
  }

  @Override
  public void update(AnActionEvent event) {
    final Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null || project.isDefault()) {
      event.getPresentation().setVisible(false);
      event.getPresentation().setEnabled(false);
      return;
    }
    event.getPresentation().setVisible(true);
    event.getPresentation().setEnabled(true);
  }

  @Override
  public void actionPerformed(final AnActionEvent event) {
    UsageTrackerProvider.getInstance().trackEvent(GctTracking.VCS_UPLOAD).ping();

    final Project project = event.getData(CommonDataKeys.PROJECT);
    final VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

    if (project == null || project.isDisposed() || !isGitSupported(project)) {
      return;
    }

    uploadProjectToGoogleCloud(project, file);
  }

  private static void uploadProjectToGoogleCloud(@NotNull final Project project,
      @Nullable final VirtualFile file) {
    BasicAction.saveAll();
    project.save();

    final GitRepository gitRepository = getGitRepository(project, file);
    final boolean gitDetected = gitRepository != null;
    final VirtualFile root = gitDetected ? gitRepository.getRoot() : project.getBaseDir();

    // check for existing git repo
    boolean externalRemoteDetected = false;
    if (gitDetected) {
      final String gcpRemote = GcpHttpAuthDataProvider.findGcpRemoteUrl(gitRepository);
      if (gcpRemote != null) {
        Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.alreadyexists"), "Google");
        return;
      }
      externalRemoteDetected = !gitRepository.getRemotes().isEmpty();
    }

    ChooseProjectDialog dialog =
        new ChooseProjectDialog(project, GctBundle.message("uploadtogcp.selecttext"),
            GctBundle.message("uploadtogcp.oktext"));
    DialogManager.show(dialog);
    if (!dialog.isOK() || dialog.getCredentialedUser() == null || Strings
        .isNullOrEmpty(dialog.getProjectId())) {
      return;
    }

    final String projectId = dialog.getProjectId();
    final CredentialedUser user = dialog.getCredentialedUser();

    // finish the job in background
    final boolean finalExternalRemoteDetected = externalRemoteDetected;
    new Task.Backgroundable(project, GctBundle.message("uploadtogcp.backgroundtitle")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        // creating empty git repo if git is not initialized
        LOG.info("Binding local project with Git");
        if (!gitDetected) {
          LOG.info("No git detected, creating empty git repo");
          indicator.setText(GctBundle.message("uploadtogcp.indicatorinit"));
          if (!createEmptyGitRepository(project, root, indicator)) {
            return;
          }
        }

        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        final GitRepository repository = repositoryManager.getRepositoryForRoot(root);
        LOG.assertTrue(repository != null, "GitRepository is null for root " + root);
        if (repository == null) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.failedtocreategit"),
                  "Google");
            }
          });
          return;
        }

        final String remoteUrl = GcpHttpAuthDataProvider.getGcpUrl(projectId);
        final String remoteName = finalExternalRemoteDetected ? "cloud-platform" : "origin";

        LOG.info("Adding Google as a remote host");
        indicator.setText(GctBundle.message("uploadtogcp.addingremote"));
        if (!addGitRemote(project, repository, remoteName, remoteUrl)) {
          return;
        }

        boolean succeeded = false;
        try {
          PropertiesComponent.getInstance(project)
              .setValue(GcpHttpAuthDataProvider.GCP_USER, user.getEmail());

          LOG.info("Fetching from Google remote");
          indicator.setText(GctBundle.message("uploadtogcp.fetching"));
          if (!fetchGit(project, indicator, repository, remoteName) || hasRemoteBranch(project,
              repository, remoteName, projectId)) {
            return;
          }

          // create sample commit for binding project
          if (!performFirstCommitIfRequired(project, root, repository, indicator)) {
            return;
          }

          //git push origin master
          LOG.info("Pushing to Google master");
          indicator.setText(GctBundle.message("uploadtogcp.pushingtotgcp"));
          if (!pushCurrentBranch(project, repository, remoteName, remoteUrl)) {
            return;
          }

          succeeded = true;
        } finally {
          if (!succeeded) {
            //remove the remote if possible on a failure, so the user can try again.
            removeGitRemote(project, repository, remoteName);
          }
        }

        showInfoUrl(project,
            remoteName,
            GctBundle.message("uploadtogcp.success"),
            remoteUrl);
      }
    }.queue();
  }

  static void showInfoUrl(@NotNull Project project, @NotNull String title,
      @NotNull String message, @NotNull String url) {

    LOG.info(title + "; " + message + "; " + url);

    VcsNotifier.getInstance(project)
        .notifyImportantInfo(title, "<a href='" + url + "'>" + message + "</a>",
            NotificationListener.URL_OPENING_LISTENER);
  }

  @Nullable
  private static GitRepository getGitRepository(@NotNull Project project,
      @Nullable VirtualFile file) {
    GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
    List<GitRepository> repositories = manager.getRepositories();
    if (repositories.size() == 0) {
      return null;
    }
    if (repositories.size() == 1) {
      return repositories.get(0);
    }
    if (file != null) {
      GitRepository repository = manager.getRepositoryForFile(file);
      if (repository != null) {
        return repository;
      }
    }
    return manager.getRepositoryForFile(project.getBaseDir());
  }

  private static boolean isGitSupported(final Project project) {
    final GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
    final String executable = settings.getPathToGit();
    final GitVersion version;
    try {
      version = GitVersion.identifyVersion(executable);
    } catch (Exception ex) {
      Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.giterror"), ex.getMessage());
      return false;
    }

    if (!version.isSupported()) {
      Messages.showWarningDialog(project, GctBundle
              .message("uploadtogcp.git.unsupported.message", version.toString(), GitVersion.MIN),
          GctBundle.message("uploadtogcp.giterror"));
      return false;
    }
    return true;
  }

  private static boolean createEmptyGitRepository(@NotNull final Project project,
      @NotNull VirtualFile root,
      @NotNull ProgressIndicator indicator) {
    final GitLineHandler gitLineHandler = new GitLineHandler(project, root, GitCommand.INIT);
    gitLineHandler.setStdoutSuppressed(false);
    GitHandlerUtil.runInCurrentThread(gitLineHandler, indicator, true,
        GitBundle.getString("initializing.title"));
    if (!gitLineHandler.errors().isEmpty()) {
      GitUIUtil.showOperationErrors(project, gitLineHandler.errors(), "git init");
      LOG.info("Failed to create empty git repo: " + gitLineHandler.errors());
      return false;
    }
    try {
      GitConfigUtil
          .setValue(project, root, "credential.https://source.developers.google.com.useHttpPath",
              "true");
    } catch (VcsException ex) {
      LOG.error(
          "VcsException while setting up credential parameters (credentials will be not be cached "
              + "per project url)" + ex.toString());
    }
    GitInit.refreshAndConfigureVcsMappings(project, root, root.getPath());
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          project.save();
        }
      });
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOG.error("InterruptedException while saving project: " + ex.toString());
      return false;
    } catch (InvocationTargetException ex) {
      LOG.error("InvocationTargetException while saving project: " + ex.toString());
      return false;
    }
    return true;
  }

  private static boolean fetchGit(@NotNull final Project project,
      @NotNull ProgressIndicator indicator,
      @NotNull GitRepository repository,
      final @NotNull String remote) {

    GitFetcher fetcher = new GitFetcher(project, indicator, true);
    GitFetchResult result = fetcher.fetch(repository);

    if (!result.isSuccess()) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.fetchfailed", remote),
              GctBundle.message("uploadtogcp.fetchfailedtitle"));
        }
      });
      return false;
    }
    repository.update();
    return true;
  }

  private static boolean hasRemoteBranch(final @NotNull Project project,
      @NotNull GitRepository repository,
      @NotNull String remoteName,
      final @NotNull String projectId) {
    for (GitRemoteBranch remoteBranch : repository.getInfo().getRemoteBranches()) {
      if (remoteBranch.getRemote().getName().equalsIgnoreCase(remoteName)) {
        LOG.warn("git repo is not empty, bailing");
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog(project,
                GctBundle.message("uploadtogcp.remotenotempty", projectId),
                GctBundle.message("uploadtogcp.remotenotemptytitle"));
          }
        });
        return true;
      }
    }

    return false;
  }

  private static boolean addGitRemote(final @NotNull Project project,
      @NotNull GitRepository repository,
      @NotNull String remote,
      final @NotNull String url) {
    final GitSimpleHandler handler = new GitSimpleHandler(project, repository.getRoot(),
        GitCommand.REMOTE);
    handler.setSilent(true);
    try {
      handler.addParameters("add", remote, url);
      handler.run();
      if (handler.getExitCode() != 0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog(project,
                GctBundle.message("uploadtogcp.addremotefailed", url, handler.getStderr()),
                GctBundle.message("uploadtogcp.addremotefailedtitle"));
          }
        });
        return false;
      }
      // catch newly added remote
      repository.update();
      return true;
    } catch (final VcsException ex) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(project,
              GctBundle.message("uploadtogcp.addremotefailed", url, ex.toString()),
              GctBundle.message("uploadtogcp.addremotefailedtitle"));
        }
      });
      return false;
    }
  }

  private static void removeGitRemote(final @NotNull Project project,
      @NotNull GitRepository repository, @NotNull String remote) {
    final GitSimpleHandler handler = new GitSimpleHandler(project, repository.getRoot(),
        GitCommand.REMOTE);
    handler.setSilent(true);
    try {
      handler.addParameters("remove", remote);
      handler.run();
    } catch (final VcsException ex) {
      LOG.error("error removing newly added remote on error:" + ex.toString());
    }
    // catch newly added remote
    repository.update();
  }


  private static boolean performFirstCommitIfRequired(@NotNull final Project project,
      @NotNull VirtualFile root,
      @NotNull GitRepository repository,
      @NotNull ProgressIndicator indicator) {
    // check if there are no commits
    if (!repository.isFresh()) {
      return true;
    }

    LOG.info("Trying to commit");
    try {
      LOG.info("Adding files for commit");
      indicator.setText(GctBundle.getString("uploadsourceaction.addfiles"));

      // ask for files to add
      final List<VirtualFile> trackedFiles = ChangeListManager.getInstance(project)
          .getAffectedFiles();
      final Collection<VirtualFile> untrackedFiles =
          filterOutIgnored(project, repository.getUntrackedFilesHolder().retrieveUntrackedFiles());
      trackedFiles.removeAll(untrackedFiles); // fix IDEA-119855

      final List<VirtualFile> allFiles = new ArrayList<VirtualFile>();
      allFiles.addAll(trackedFiles);
      allFiles.addAll(untrackedFiles);

      final Ref<GcpUntrackedFilesDialog> dialogRef = new Ref<GcpUntrackedFilesDialog>();
      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        @Override
        public void run() {
          GcpUntrackedFilesDialog dialog = new GcpUntrackedFilesDialog(project, allFiles);
          if (!trackedFiles.isEmpty()) {
            dialog.setSelectedFiles(trackedFiles);
          }
          DialogManager.show(dialog);
          dialogRef.set(dialog);
        }
      }, indicator.getModalityState());
      final GcpUntrackedFilesDialog dialog = dialogRef.get();

      final Collection<VirtualFile> filesToCommit = dialog.getSelectedFiles();
      if (!dialog.isOK() || filesToCommit.isEmpty()) {
        LOG.warn("user canceled out of initial commit.  aborting...");
        return false;
      }

      Set<VirtualFile> filesToCommitAsSet = new HashSet<>(filesToCommit);
      Set<VirtualFile> untrackedFilesAsSet = new HashSet<>(untrackedFiles);
      Set<VirtualFile> trackedFilesAsSet = new HashSet<>(trackedFiles);

      Collection<VirtualFile> filesToAdd
          = Sets.intersection(untrackedFilesAsSet, filesToCommitAsSet);
      Collection<VirtualFile> filesToRm = Sets.difference(trackedFilesAsSet, filesToCommitAsSet);
      Collection<VirtualFile> modified = new HashSet<VirtualFile>(trackedFiles);
      modified.addAll(filesToCommit);

      GitFileUtils.addFiles(project, root, filesToAdd);
      GitFileUtils.deleteFilesFromCache(project, root, filesToRm);

      // commit
      LOG.info("Performing commit");
      indicator.setText(GctBundle.getString("uploadsourceaction.performingcommit"));
      GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.COMMIT);
      handler.setStdoutSuppressed(false);
      handler.addParameters("-m", dialog.getCommitMessage());
      handler.endOptions();
      handler.run();

      VcsFileUtil.markFilesDirty(project, modified);
    } catch (final VcsException ex) {
      LOG.warn(ex);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(project,
              GctBundle.message("uploadtogcp.initialcommitfailed", ex.toString()),
              GctBundle.message("uploadtogcp.initialcommitfailedtitle"));
        }
      });
      return false;
    }
    LOG.info("Successfully created initial commit");
    return true;
  }

  @NotNull
  private static Collection<VirtualFile> filterOutIgnored(@NotNull Project project,
      @NotNull Collection<VirtualFile> files) {
    final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    final FileIndexFacade fileIndex = FileIndexFacade.getInstance(project);
    return Collections2.filter(files, new Predicate<VirtualFile>() {
      @Override
      public boolean apply(@javax.annotation.Nullable VirtualFile file) {
        return !changeListManager.isIgnoredFile(file) && !fileIndex.isExcludedFile(file);
      }
    });
  }

  private static boolean pushCurrentBranch(final @NotNull Project project,
      @NotNull GitRepository repository,
      @NotNull String remoteName,
      @NotNull String remoteUrl) {
    Git git = ServiceManager.getService(Git.class);

    GitLocalBranch currentBranch = repository.getCurrentBranch();
    if (currentBranch == null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.initialpushfailed"),
              GctBundle.message("uploadtogcp.initialpushfailedtitle"));
        }
      });
      return false;
    }
    GitCommandResult result = git
        .push(repository, remoteName, remoteUrl, currentBranch.getName(), true);
    if (!result.success()) {
      LOG.warn(result.getErrorOutputAsJoinedString());
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(project, GctBundle.message("uploadtogcp.initialpushfailed"),
              GctBundle.message("uploadtogcp.initialpushfailedtitle"));
        }
      });
      return false;
    }
    return true;
  }

  private static class GcpUntrackedFilesDialog extends SelectFilesDialog implements
      TypeSafeDataProvider {

    private static final float SPLIT_PROPORTION = 0.7f;

    @NotNull
    private final Project project;
    private CommitMessage commitMessagePanel;

    @SuppressWarnings("ConstantConditions")
    // This suppresses an invalid null warning for calling the super with null params.
    public GcpUntrackedFilesDialog(@NotNull Project project,
        @NotNull List<VirtualFile> untrackedFiles) {
      super(project, untrackedFiles, null, null, true, false, false);
      this.project = project;
      setTitle(GctBundle.getString("uploadsourceaction.addfilestitle"));
      init();
    }

    @Override
    protected JComponent createNorthPanel() {
      return null;
    }

    @Override
    protected JComponent createCenterPanel() {
      final JComponent tree = super.createCenterPanel();

      commitMessagePanel = new CommitMessage(project);
      commitMessagePanel.setCommitMessage("Initial commit");

      Splitter splitter = new Splitter(true);
      splitter.setHonorComponentsMinimumSize(true);
      splitter.setFirstComponent(tree);
      splitter.setSecondComponent(commitMessagePanel);
      splitter.setProportion(SPLIT_PROPORTION);

      return splitter;
    }

    @NotNull
    public String getCommitMessage() {
      return commitMessagePanel.getComment();
    }

    @Override
    public void calcData(DataKey key, DataSink sink) {
      if (key == VcsDataKeys.COMMIT_MESSAGE_CONTROL) {
        sink.put(VcsDataKeys.COMMIT_MESSAGE_CONTROL, commitMessagePanel);
      }
    }

    @Override
    protected String getDimensionServiceKey() {
      return "GCP.UntrackedFilesDialog";
    }
  }

}
