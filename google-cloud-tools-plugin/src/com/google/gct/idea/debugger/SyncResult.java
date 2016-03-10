package com.google.gct.idea.debugger;

import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the result of comparing local source to the source the cloud debugger is
 * attached to on the server.
 */
public class SyncResult {
  private final boolean invalidDebuggee;

  private final boolean needsStash;
  private final boolean needsSync;
  private final GitRepository targetLocalRepository;
  private final boolean hasRemoteRepository;
  private final String targetSyncSHA;
  private final String repoType;

  SyncResult(boolean invalidDebuggee,
             boolean needsStash,
             boolean needsSync,
             @Nullable String targetSyncSHA,
             @Nullable GitRepository targetRepository,
             boolean hasRemoteRepository,
             String repoType) {
    this.invalidDebuggee = invalidDebuggee;
    this.needsStash = needsStash;
    this.needsSync = needsSync;
    this.targetSyncSHA = targetSyncSHA;
    targetLocalRepository = targetRepository;
    this.hasRemoteRepository = hasRemoteRepository;
    this.repoType = repoType;
  }

  public GitRepository getLocalRepository() {
    return targetLocalRepository;
  }

  public boolean hasLocalRepository() {
    return targetLocalRepository != null;
  }

  @Nullable
  public boolean hasRemoteRepository() {
    return hasRemoteRepository;
  }

  @Nullable
  public String getTargetSyncSHA() {
    return targetSyncSHA;
  }

  public boolean isValidDebuggee() {
    return !invalidDebuggee;
  }

  /**
   * Whether the local repository has uncommitted changes we need to stash
   */
  public boolean needsStash() {
    return needsStash;
  }

  /**
   * Return the kind of remote repository found, or null if no matching repository was found
   */
  @Nullable
  public String getRepositoryType() {
    return repoType;
  }

  /**
   * Whether the local repository needs to be synced with the remote repository
   */
   public boolean needsSync() {
    return needsSync;
   }

}
