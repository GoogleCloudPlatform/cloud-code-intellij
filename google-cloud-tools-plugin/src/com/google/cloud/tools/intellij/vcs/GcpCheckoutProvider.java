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

package com.google.cloud.tools.intellij.vcs;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationListener.UrlOpeningListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.DialogManager;
import git4idea.GitVcs;
import git4idea.actions.BasicAction;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandlerListener;
import git4idea.commands.GitStandardProgressAnalyzer;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Checkout provider for the Google Cloud Platform. */
public class GcpCheckoutProvider implements CheckoutProvider {

  private static final Logger LOG = Logger.getInstance(GcpCheckoutProvider.class);
  private static final String URL_REGEX =
      "https?://(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&/"
          + "/=]*)";

  private final Git git;

  public GcpCheckoutProvider(@NotNull Git git) {
    this.git = git;
  }

  @Override
  public String getVcsName() {
    return "_Google Cloud";
  }

  @Override
  public void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
    UsageTrackerProvider.getInstance().trackEvent(GctTracking.VCS_CHECKOUT).ping();

    BasicAction.saveAll();
    CloneCloudRepositoryDialog dialog = new CloneCloudRepositoryDialog(project);
    DialogManager.show(dialog);
    if (!dialog.isOK()
        || Strings.isNullOrEmpty(dialog.getParentDirectory())
        || Strings.isNullOrEmpty(dialog.getSourceRepositoryUrl())
        || Strings.isNullOrEmpty(dialog.getDirectoryName())) {
      return;
    }
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    final File parent = new File(dialog.getParentDirectory());
    VirtualFile destinationParent = lfs.findFileByIoFile(parent);
    if (destinationParent == null) {
      destinationParent = lfs.refreshAndFindFileByIoFile(parent);
    }
    if (destinationParent == null) {
      return;
    }
    final String sourceRepositoryUrl = dialog.getSourceRepositoryUrl();
    final String directoryName = dialog.getDirectoryName();
    final String parentDirectory = dialog.getParentDirectory();
    final String gcpUserName = dialog.getGcpUserName();
    if (Strings.isNullOrEmpty(gcpUserName)) {
      LOG.error("unexpected blank username during checkout");
      return;
    }
    clone(
        project,
        git,
        listener,
        destinationParent,
        sourceRepositoryUrl,
        directoryName,
        parentDirectory,
        gcpUserName);
  }

  private static void clone(
      @NotNull final Project project,
      @NotNull final Git git,
      @Nullable final Listener listener,
      @NotNull final VirtualFile destinationParent,
      @NotNull final String sourceRepositoryUrl,
      @NotNull final String directoryName,
      @NotNull final String parentDirectory,
      @Nullable final String gcpUserName) {

    final AtomicBoolean cloneResult = new AtomicBoolean();
    new Task.Backgroundable(
        project, GctBundle.message("clonefromgcp.repository", sourceRepositoryUrl)) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        GcpHttpAuthDataProvider.Context context =
            GcpHttpAuthDataProvider.createContext(gcpUserName);
        try {
          cloneResult.set(
              doClone(
                  project, indicator, git, directoryName, parentDirectory, sourceRepositoryUrl));
        } finally {
          context.close();
        }
      }

      @Override
      public void onSuccess() {
        if (!cloneResult.get()) {
          return;
        }

        destinationParent.refresh(
            true,
            true,
            new Runnable() {
              @Override
              public void run() {
                if (project.isOpen() && (!project.isDisposed()) && (!project.isDefault())) {
                  final VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
                  mgr.fileDirty(destinationParent);
                }
              }
            });

        ProjectManagerListener configWriter =
            new ProjectManagerListener() {
              @Override
              public void projectOpened(Project project) {
                PropertiesComponent.getInstance(project)
                    .setValue(
                        GcpHttpAuthDataProvider.GCP_USER, gcpUserName == null ? "" : gcpUserName);
              }

              @Override
              public boolean canCloseProject(Project project) {
                return true;
              }

              @Override
              public void projectClosed(Project project) {
                // no-op
              }

              @Override
              public void projectClosing(Project project) {
                // no-op
              }
            };

        ProjectManager.getInstance().addProjectManagerListener(configWriter);
        try {
          if (listener != null) {
            listener.directoryCheckedOut(new File(parentDirectory, directoryName), GitVcs.getKey());
            listener.checkoutCompleted();
          }
        } finally {
          ProjectManager.getInstance().removeProjectManagerListener(configWriter);
        }
      }
    }.queue();
  }

  private static boolean doClone(
      @NotNull Project project,
      @NotNull ProgressIndicator indicator,
      @NotNull Git git,
      @NotNull String directoryName,
      @NotNull String parentDirectory,
      @NotNull String sourceRepositoryUrl) {
    indicator.setIndeterminate(false);
    GitLineHandlerListener progressListener = GitStandardProgressAnalyzer.createListener(indicator);

    GitCommandResult result =
        git.clone(
            project,
            new File(parentDirectory),
            sourceRepositoryUrl,
            directoryName,
            progressListener);
    if (result.success()) {
      return true;
    }
    VcsNotifier.getInstance(project)
        .notifyError(
            GctBundle.message("clonefromgcp.failed"),
            result.getErrorOutputAsHtmlString()
                + "<br>"
                + result.getOutputAsJoinedString().replaceAll(URL_REGEX, "<a href=\"$0\">$0</a>"),
            new UrlOpeningListener(true));
    return false;
  }
}
